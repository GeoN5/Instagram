package com.example.instagram.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.instagram.R
import com.example.instagram.model.ContentDTO
import com.example.instagram.util.loadImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailviewFragment : Fragment(){

    var firesotre : FirebaseFirestore? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        firesotre = FirebaseFirestore.getInstance()

        val view =  inflater.inflate(R.layout.fragment_detail,container,false)
        view.detailviewfragment_recyclerview.adapter = DetailRecyclerviewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(context)
        return view
    }

    inner class DetailRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = ArrayList()
        var contentUidList : ArrayList<String> = ArrayList() //document
        var uid = FirebaseAuth.getInstance().currentUser?.uid //현재 로그인된 유저의 uid

        init {
            firesotre?.collection("images")?.orderBy("timestamp", Query.Direction.DESCENDING)?.//내림차순 정렬
                addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()
                    for(snapshot in querySnapshot!!.documents){
                        val item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!)
                        contentUidList.add(snapshot.id)
                    }
                    notifyDataSetChanged()
                }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_detail,parent,false)
            return DetailViewHolder(view)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolder = holder.itemView

            viewHolder.detailviewitem_profile_textview.text = contentDTOs[position].userId //유저 아이디
            viewHolder.detailviewitem_imageview_content.loadImage(contentDTOs[position].imageUri!!,context!!) //이미지
            viewHolder.detailviewitem_explain_textview.text = contentDTOs[position].explain //설명 텍스트
            viewHolder.detailviewitem_favoritecounter_textview.text = "좋아요 ${contentDTOs[position].favoriteCount}개" //좋아요 카운터
            viewHolder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }
            if(contentDTOs[position].favorites.containsKey(uid)){ //좋아요 클릭되있는 경우
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{ //클릭되있지 않은 경우
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }
        }

        private fun favoriteEvent(position: Int){
            val tsDoc = firesotre?.collection("images")?.document(contentUidList[position])
            firesotre?.runTransaction { transaction: Transaction -> //다른 사용자가 도큐먼트를 점유할 수 없음
                val contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)
                if(contentDTO?.favorites?.containsKey(uid)!!){ //좋아요를 누른상태
                    contentDTO.favorites.remove(uid)
                    contentDTO.favoriteCount -= 1
                }else{ //좋아요를 누르지 않은 상태
                    contentDTO.favorites[uid!!] = true
                    contentDTO.favoriteCount += 1
                }
                transaction.set(tsDoc,contentDTO)
            }
        }

    }

}

class DetailViewHolder(view: View) : RecyclerView.ViewHolder(view)



