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
import com.example.instagram.model.ContentDTO
import com.example.instagram.util.loadImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment(){

    val PICK_PROFILE_FROM_ALBUM = 10
    lateinit var fragmentView: View
    lateinit var firestore : FirebaseFirestore
    lateinit var currentUid: String //자신의 uid
    lateinit var uid :String //내가 선택한 uid

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        firestore = FirebaseFirestore.getInstance()
        currentUid = FirebaseAuth.getInstance().currentUser?.uid!!
        fragmentView =  inflater.inflate(R.layout.fragment_user,container,false)
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

    private fun getProfileImages(){
        firestore.collection("profileImages").document(currentUid)
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(documentSnapshot!!.data != null){
                    val uri = documentSnapshot.data!!["image"]
                    Glide.with(context!!).load(uri).apply(RequestOptions().circleCrop()).into(fragmentView.account_iv_profile)
                }

        }
    }

    inner class UserFragmentRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = ArrayList()

        init {

            firestore.collection("images").whereEqualTo("uid",currentUid)//필터링 메소드 2번 호출하면 왜 querySnapshot NPE??
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    for(snapshot in querySnapshot!!.documents){
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