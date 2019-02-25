package com.example.instagram.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instagram.R
import com.example.instagram.model.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.item_alarm.view.*

class AlarmFragment : Fragment(){

    private lateinit var auth : FirebaseAuth
    private lateinit var fireStore : FirebaseFirestore
    private lateinit var recyclerview :RecyclerView
    private lateinit var recyclerviewListenerRegistration :ListenerRegistration

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView =  inflater.inflate(R.layout.fragment_alarm,container,false)
        firebaseInit()
        recyclerview = fragmentView.findViewById(R.id.alarmfragment_recyclerview)

        return fragmentView
    }

    private fun firebaseInit(){
        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()
    }

    override fun onResume() {
        super.onResume()
        recyclerview.adapter = AlarmRecyclerViewAdapter()
        recyclerview.layoutManager = LinearLayoutManager(context)
    }

    override fun onStop() {
        super.onStop()
        recyclerviewListenerRegistration.remove()
    }

    inner class AlarmRecyclerViewAdapter : RecyclerView.Adapter<AlarmViewHolder>(){

        private val alarmDTOlist :ArrayList<AlarmDTO> = ArrayList()

        init {
            val uid = auth.currentUser?.uid
            recyclerviewListenerRegistration = fireStore.collection("alarms").whereEqualTo("destinationUid",uid)
                .orderBy("timestamp",Query.Direction.DESCENDING).addSnapshotListener { querySnapshot, _ ->
                    if(querySnapshot == null)return@addSnapshotListener
                    alarmDTOlist.clear()
                    for(snapshot in querySnapshot.documents){
                        alarmDTOlist.add(snapshot.toObject(AlarmDTO::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_alarm,parent,false)
            return AlarmViewHolder(view)
        }

        override fun getItemCount(): Int {
            return alarmDTOlist.size
        }

        override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {

            when(alarmDTOlist[position].kind){
                0 -> {
                    val str0 = "${alarmDTOlist[position].userId}${getString(R.string.alarm_favorite)}"
                    holder.commentTextView.text = str0
                }
                1 -> {
                    val str1 = "${alarmDTOlist[position].userId}${getString(R.string.alarm_who)}" +
                            "\"${alarmDTOlist[position].message}\"${getString(R.string.alarm_comment)}"
                    holder.commentTextView.text = str1
                }
                2 -> {
                    val str2 = "${alarmDTOlist[position].userId}${getString(R.string.alarm_follow)}"
                    holder.commentTextView.text = str2
                }
            }

            fireStore.collection("profileImages").document(alarmDTOlist[position].uid!!).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val url = task.result!!["image"]
                        Glide.with(context!!).load(url).apply(RequestOptions().circleCrop()).into(holder.alarmImage)
                    }
                }
        }

    }

}

class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view){
    var alarmImage = view.alarmviewitem_imageview_profile!!
    var commentTextView = view.alarmviewItem_textview_message!!
}