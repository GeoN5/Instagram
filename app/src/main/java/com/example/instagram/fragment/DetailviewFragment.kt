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
            firesotre?.collection("images")?.orderBy("timestamp")?.
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
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail,parent,false)
            return CustomViewHolder(view)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolder = holder.itemView

            viewHolder.detailviewitem_profile_textview.text = contentDTOs[position].userId //유저 아이디
            //이미지
            viewHolder.detailviewitem_explain_textview.text = contentDTOs[position].explain //설명 텍스트
            viewHolder.detailviewitem_favoritecounter_textview.text = "좋아요 ${contentDTOs[position].favoriteCount}개" //좋아요 카운터
            viewHolder.detailviewitem_imageview_content.loadImage(contentDTOs[position].imageUri!!,context!!)

        }

    }

    class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

}



