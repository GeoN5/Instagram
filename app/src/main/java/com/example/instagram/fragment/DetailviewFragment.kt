package com.example.instagram.fragment

import android.content.Intent
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
import com.example.instagram.activity.CommentActivity
import com.example.instagram.model.AlarmDTO
import com.example.instagram.model.ContentDTO
import com.example.instagram.model.FollowDTO
import com.example.instagram.util.loadImage
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailviewFragment : Fragment(){

    private lateinit var fireStore : FirebaseFirestore
    private lateinit var auth : FirebaseAuth
    private lateinit var recyclerview :RecyclerView
    private lateinit var recyclerviewListenerRegistration : ListenerRegistration

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView =  inflater.inflate(R.layout.fragment_detail,container,false)
        firebaseInit()
        recyclerview = fragmentView.findViewById(R.id.detailviewfragment_recyclerview)

        return fragmentView
    }

    private fun firebaseInit(){
        fireStore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    override fun onResume() {
        super.onResume()
        recyclerview.adapter = DetailRecyclerViewAdapter()
        recyclerview.layoutManager = LinearLayoutManager(context)
    }

    override fun onStop() {
        super.onStop()
        recyclerviewListenerRegistration.remove()
    }

    inner class DetailRecyclerViewAdapter : RecyclerView.Adapter<DetailViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = ArrayList()
        var contentUidList : ArrayList<String> = ArrayList() //document
        var uid = auth.currentUser?.uid //현재 로그인된 유저의 uid

        init {
            fireStore.collection("users").document(uid!!).get().addOnCompleteListener { task: Task<DocumentSnapshot> ->
                if(task.isSuccessful){
                    val followDTO = task.result?.toObject(FollowDTO::class.java)
                    if(followDTO != null) {
                        getContents(followDTO.followings)
                    }
                }
            }
        }

        private fun getContents(followings:MutableMap<String,Boolean>){
            recyclerviewListenerRegistration = fireStore.collection("images").orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { querySnapshot, _ ->
                    if(querySnapshot == null)return@addSnapshotListener
                    contentDTOs.clear()
                    contentUidList.clear()
                    for(snapshot in querySnapshot.documents){
                        val item = snapshot.toObject(ContentDTO::class.java)
                        if(followings.keys.contains(item?.uid)) {
                            contentDTOs.add(item!!)
                            contentUidList.add(snapshot.id)
                        }
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_detail,parent,false)
            return DetailViewHolder(view)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {

            fireStore.collection("profileImages").document(contentDTOs[position].uid!!).get()
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        val url = task.result!!["image"]
                        Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(holder.profileImage)
                    }
                }
            holder.userId.text = contentDTOs[position].userId //유저 아이디
            holder.imageContent.loadImage(contentDTOs[position].imageUri!!,context!!) //이미지
            holder.explainText.text = contentDTOs[position].explain //설명 텍스트
            holder.favoriteCount.text = "좋아요 ${contentDTOs[position].favoriteCount}개" //좋아요 카운터
            holder.favoriteImage.setOnClickListener {
                favoriteEvent(position)
            }
            if(contentDTOs[position].favorites.containsKey(uid)){ //좋아요 클릭되있는 경우
                holder.favoriteImage.setImageResource(R.drawable.ic_favorite)
            }else{ //클릭되있지 않은 경우
                holder.favoriteImage.setImageResource(R.drawable.ic_favorite_border)
            }
            holder.profileImage.setOnClickListener {//프래그먼트간의 전환
                val fragment = UserFragment()
                val bundle = Bundle()
                bundle.putString("destinationUid",contentDTOs[position].uid)
                bundle.putString("userId",contentDTOs[position].userId)
                fragment.arguments = bundle
                activity!!.supportFragmentManager.beginTransaction().replace(R.id.main_content,fragment).commit()
            }
            holder.commentImage.setOnClickListener {
                //게시물 id,게시물 작성자 uid 넘김
                startActivity(Intent(context,CommentActivity::class.java).
                    putExtra("contentUid",contentUidList[position]).putExtra("destinationUid",contentDTOs[position].uid))
            }

        }

        private fun favoriteEvent(position: Int){
            val tsDoc = fireStore.collection("images").document(contentUidList[position])
            fireStore.runTransaction { transaction: Transaction -> //다른 사용자가 도큐먼트를 점유할 수 없음
                val contentDTO = transaction.get(tsDoc).toObject(ContentDTO::class.java)
                if(contentDTO?.favorites?.containsKey(uid)!!){ //좋아요를 누른상태 -> 누르지 않는 상태
                    contentDTO.favorites.remove(uid)
                    contentDTO.favoriteCount -= 1
                }else{ //좋아요를 누르지 않은 상태 -> 누르는 상태
                    contentDTO.favorites[uid!!] = true
                    contentDTO.favoriteCount += 1
                    favoriteAlarm(contentDTOs[position].uid!!)//게시물 올린 사람 uid
                }
                transaction.set(tsDoc,contentDTO)
                return@runTransaction
            }
        }

        private fun favoriteAlarm(destinationUid: String){
            val alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = auth.currentUser?.email
            alarmDTO.uid = auth.currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()

            fireStore.collection("alarms").document().set(alarmDTO)
        }

    }

}

class DetailViewHolder(view: View) : RecyclerView.ViewHolder(view){
    var profileImage = view.detailviewitem_profile_image!!
    var userId = view.detailviewitem_profile_textview!!
    var imageContent = view.detailviewitem_imageview_content!!
    var explainText = view.detailviewitem_explain_textview!!
    var favoriteCount = view.detailviewitem_favoritecounter_textview!!
    var favoriteImage = view.detailviewitem_favorite_imageview!!
    var commentImage = view.detailviewitem_comment_imageview!!
}



