package com.example.instagram.fragment

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instagram.R
import com.example.instagram.activity.LoginActivity
import com.example.instagram.activity.MainActivity
import com.example.instagram.model.AlarmDTO
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

    private val PICK_PROFILE_FROM_ALBUM = 10
    private lateinit var fragmentView: View
    private lateinit var fireStore : FirebaseFirestore
    private lateinit var currentUserUid: String //자신의 uid
    private lateinit var uid :String //내가 선택한 uid
    private lateinit var auth : FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView =  inflater.inflate(R.layout.fragment_user,container,false)
        firebaseInit()
        currentUserUid = auth.currentUser?.uid!!

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
        }

        fragmentView.account_iv_profile.setOnClickListener {
            if(currentUserUid == uid) {
                val photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
            }
        }
        fragmentView.account_recyclerview.adapter = UserRecyclerViewAdapter()
        fragmentView.account_recyclerview.layoutManager = GridLayoutManager(context!!,3)
        getProfileImages()
        getFollower()
        getFollowing()
        return fragmentView
    }

    private fun firebaseInit(){
        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()
    }

    private fun requestFollow(){ //내가 제3자의 팔로우 버튼을 눌렀을 때
        val tsDocFollowing = fireStore.collection("users").document(currentUserUid)
        fireStore.runTransaction { transaction: Transaction ->
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
        val tsDocFollower = fireStore.collection("users").document(uid)
        fireStore.runTransaction { transaction: Transaction ->
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
                followerAlarm(uid)
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }

    private fun getProfileImages(){
        fireStore.collection("profileImages").document(uid)
            .addSnapshotListener { documentSnapshot, _ ->
                if(documentSnapshot == null)return@addSnapshotListener
                if(documentSnapshot.data != null){
                    val uri = documentSnapshot.data!!["image"]
                    Glide.with(context!!).load(uri).apply(RequestOptions().circleCrop()).into(fragmentView.account_iv_profile)
                }

        }
    }

    private fun getFollower(){
        fireStore.collection("users").document(uid)
            .addSnapshotListener { documentSnapshot, _ ->
                if(documentSnapshot == null)return@addSnapshotListener
                val followDTO = documentSnapshot.toObject(FollowDTO::class.java)
                fragmentView.account_tv_follower_count.text = followDTO?.followerCount.toString()
            }
    }

    private fun getFollowing(){
        fireStore.collection("users").document(uid)
            .addSnapshotListener { documentSnapshot, _ ->
                if(documentSnapshot == null)return@addSnapshotListener
                val followDTO = documentSnapshot.toObject(FollowDTO::class.java)
                fragmentView.account_tv_following_count.text = followDTO?.followingCount.toString()
            }
    }

    private fun followerAlarm(destinationUid: String){
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth.currentUser?.email
        alarmDTO.uid = auth.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()

        fireStore.collection("alarms").document().set(alarmDTO)
    }

    inner class UserRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = ArrayList()

        init {
            fireStore.collection("images").whereEqualTo("uid",uid).orderBy("timestamp",Query.Direction.DESCENDING)
                .addSnapshotListener { querySnapshot, _ ->
                    if(querySnapshot == null)return@addSnapshotListener
                    contentDTOs.clear()
                    for(snapshot in querySnapshot.documents){
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
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