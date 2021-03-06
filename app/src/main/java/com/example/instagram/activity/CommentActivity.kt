package com.example.instagram.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instagram.R
import com.example.instagram.model.AlarmDTO
import com.example.instagram.model.ContentDTO
import com.example.instagram.util.DateUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*
import java.util.*

class CommentActivity : AppCompatActivity() {

    private lateinit var contentUid : String //게시물 id
    private lateinit var destinationUid: String //게시물 올린 사람의 uid
    private lateinit var storage : FirebaseStorage
    private lateinit var auth : FirebaseAuth
    private lateinit var fireStore : FirebaseFirestore
    private lateinit var recyclerviewListenerRegistration :ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        firebaseInit()

        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        comment_btn_send.setOnClickListener {
            val comment = ContentDTO.Comment()
            comment.userId = auth.currentUser?.email
            comment.comment = comment_edit_message.text.toString()
            comment.uid = auth.currentUser?.uid
            comment.timestamp = System.currentTimeMillis()

            fireStore.collection("images")
                .document(contentUid).collection("comments").document().set(comment)
            commentAlarm(destinationUid,comment_edit_message.text.toString())
            comment_edit_message.text = null
        }

    }

    override fun onResume() {
        super.onResume()
        comment_recyclerview.adapter = CommentRecyclerViewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)
    }

    override fun onStop() {
        super.onStop()
        recyclerviewListenerRegistration.remove()

    }

    private fun firebaseInit(){
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()
    }

    private fun commentAlarm(destinationUid: String,message: String){
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth.currentUser?.email
        alarmDTO.uid  = auth.currentUser?.uid
        alarmDTO.kind = 1 //댓글
        alarmDTO.message = message
        alarmDTO.timestamp = System.currentTimeMillis()

        fireStore.collection("alarms").document().set(alarmDTO)
    }

    inner class CommentRecyclerViewAdapter : RecyclerView.Adapter<CommentViewHolder>(){

        private val comments : ArrayList<ContentDTO.Comment> = ArrayList()

        init {
            recyclerviewListenerRegistration = fireStore.collection("images").document(contentUid).collection("comments")
                .orderBy("timestamp",Query.Direction.DESCENDING)
                .addSnapshotListener { querySnapshot, _ ->
                    if(querySnapshot == null)return@addSnapshotListener
                    comments.clear()
                    for(snapshot in querySnapshot.documents){
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
            val view = LayoutInflater.from(this@CommentActivity).inflate(R.layout.item_comment,parent,false)
            return CommentViewHolder(view)
        }

        override fun getItemCount(): Int {
            return comments.size
        }

        override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
            holder.dataText.text = DateUtil.formatDate(Date(comments[position].timestamp!!))
            holder.profileText.text = comments[position].userId
            holder.commentText.text = comments[position].comment

            fireStore.collection("profileImages").document(comments[position].uid!!).get()
                .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val url = task.result!!["image"]
                    Glide.with(this@CommentActivity).load(url).apply(RequestOptions().circleCrop()).into(holder.profileImage)
                }
            }
        }

    }

}

class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view){
    var dataText = view.commentviewItem_textview_date!!
    var profileText = view.commentviewItem_textview_profile!!
    var commentText = view.commentviewItem_textview_comment!!
    var profileImage = view.commentviewItem_imageview_profile!!
}
