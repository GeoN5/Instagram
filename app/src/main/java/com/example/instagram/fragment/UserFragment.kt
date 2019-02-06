package com.example.instagram.fragment

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instagram.R
import com.example.instagram.activity.LoginActivity
import com.example.instagram.activity.MainActivity
import com.example.instagram.model.ContentDTO
import com.example.instagram.model.FollowDTO
import com.example.instagram.util.loadImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment(){

    val PICK_PROFILE_FROM_ALBUM = 10
    lateinit var fragmentView: View
    lateinit var firestore : FirebaseFirestore
    lateinit var currentUserUid: String //자신의 uid
    lateinit var uid :String //내가 선택한 uid
    lateinit var auth : FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        firestore = FirebaseFirestore.getInstance()
        currentUserUid = FirebaseAuth.getInstance().currentUser?.uid!!
        auth = FirebaseAuth.getInstance()
        fragmentView =  inflater.inflate(R.layout.fragment_user,container,false)

        if(arguments != null) {
            uid = arguments!!.getString("destinationUid")!!
                if (uid == currentUserUid) { //나의 유저페이지
                    fragmentView.account_btn_follow_signout.text = getString(R.string.signout)
                    fragmentView.account_btn_follow_signout.setOnClickListener {
                        activity?.finish()
                        startActivity(Intent(activity, LoginActivity::class.java))
                        auth.signOut()
                    }
                } else { //제3자의 유저페이지
                    fragmentView.account_btn_follow_signout.text = getString(R.string.follow)
                    val mainActivity = (activity as MainActivity)
                    mainActivity.toolbar_title_image.visibility = View.GONE
                    mainActivity.toolbar_btn_back.visibility = View.VISIBLE
                    mainActivity.toolbar_username.visibility = View.VISIBLE
                    mainActivity.toolbar_username.text = arguments!!.getString("userId")
                    mainActivity.toolbar_btn_back.setOnClickListener {
                        mainActivity.bottom_navigation.selectedItemId = R.id.action_home
                    }
                    fragmentView.account_btn_follow_signout.setOnClickListener {
                        requestFollow()
                    }
                }
                //uid = currentUserUid
        }
        fragmentView.account_iv_profile.setOnClickListener {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent,PICK_PROFILE_FROM_ALBUM)
        }
        fragmentView.account_recyclerview.adapter = UserFragmentRecyclerviewAdapter()
        fragmentView.account_recyclerview.layoutManager = GridLayoutManager(context!!,3)
        getProfileImages()
        return fragmentView
    }

    private fun requestFollow(){
        val tsDocFollowing = firestore.collection("users").document(currentUserUid)
        firestore.runTransaction { transaction: Transaction ->
            var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)
            if(followDTO == null){ //내가 아무도 팔로잉하고 있지 않을 경우
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid] = true
                transaction.set(tsDocFollowing,followDTO)
                return@runTransaction
            }
            if(followDTO.followings.containsKey(uid)){ //내가 이미 팔로잉 하고 있을 경우
                followDTO.followingCount -= 1
                followDTO.followings.remove(uid)
            }else{ //내가 아직 팔로잉 하지 않았을 경우
                followDTO.followingCount += 1
                followDTO.followings[uid] = true
            }
            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }
        val tsDocFollower = firestore.collection("users").document(uid)
        firestore.runTransaction { transaction: Transaction ->
            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)
            if(followDTO == null){ //아무도 제3자를 팔로워 하지 않았을 경우
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid] = true
                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }
            if(followDTO!!.followers.containsKey(currentUserUid)){ //제3자를 내가 이미 팔로잉 하고 있을 경우 -> 팔로워 취소 하겠다
                followDTO!!.followerCount -= 1
                followDTO!!.followers.remove(currentUserUid)
            }else{ //제3자를 내가 아직 팔로워 하지 않았을 경우 -> 팔로워 하겠다
                followDTO!!.followerCount += 1
                followDTO!!.followers[currentUserUid] = true
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }

    private fun getProfileImages(){
        firestore.collection("profileImages").document(uid)
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(documentSnapshot == null)return@addSnapshotListener
                if(documentSnapshot.data != null){
                    val uri = documentSnapshot.data!!["image"]
                    Glide.with(context!!).load(uri).apply(RequestOptions().circleCrop()).into(fragmentView.account_iv_profile)
                }

        }
    }

    inner class UserFragmentRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = ArrayList()

        init {

            firestore.collection("images").whereEqualTo("uid",uid)//필터링 메소드 2번 호출하면 왜 querySnapshot NPE??
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if(querySnapshot == null)return@addSnapshotListener
                    contentDTOs.clear()
                    for(snapshot in querySnapshot.documents){
                        val item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!)
                    }
                    fragmentView.account_tv_post_count.text = itemCount.toString()
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val width = resources.displayMetrics.widthPixels / 3
            val imageView = ImageView(context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width,width)
            return UserViewHolder(imageView)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val imageView = (holder as UserViewHolder).imageView
            imageView.loadImage(contentDTOs[position].imageUri!!,context!!)
        }

    }

}

class UserViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)