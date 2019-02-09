package com.example.instagram.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.instagram.R
import com.example.instagram.model.AlarmDTO
import com.example.instagram.model.ContentDTO
import com.example.instagram.util.DateUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*
import java.util.*

class CommentActivity : AppCompatActivity() {

    lateinit var contentUid : String
    lateinit var destinationUid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")
        comment_recyclerview.adapter = CommentRecyclerViewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)
        comment_btn_send.setOnClickListener {
            val comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.comment = comment_edit_message.text.toString()
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("images")
                .document(contentUid).collection("comments").document().set(comment)
            commentAlarmm(destinationUid,comment_edit_message.text.toString())
            comment_edit_message.text = null
        }

    }

    private fun commentAlarmm(destinationUid: String,message: String){
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.uid  = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.kind = 1
        alarmDTO.message = message
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
    }

    inner class CommentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        private val comments : ArrayList<ContentDTO.Comment> = ArrayList()

        init {
            FirebaseFirestore.getInstance().collection("images").document(contentUid).collection("comments")
                .orderBy("timestamp",Query.Direction.DESCENDING)
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if(querySnapshot == null)return@addSnapshotListener
                    comments.clear()
                    for(snapshot in querySnapshot.documents){
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(this@CommentActivity).inflate(R.layout.item_comment,parent,false)
            return CommentViewHolder(view)
        }

        override fun getItemCount(): Int {
            return comments.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val view = holder.itemView
            view.commentviewItem_textview_date.text = DateUtil.formatDate(Date(comments[position].timestamp!!))
            view.commentviewItem_textview_profile.text = comments[position].userId
            view.commentviewItem_textview_comment.text = comments[position].comment
        }

    }

}

class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view)
