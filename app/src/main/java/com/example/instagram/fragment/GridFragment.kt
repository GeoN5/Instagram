package com.example.instagram.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.instagram.R
import com.example.instagram.model.ContentDTO
import com.example.instagram.util.loadImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class GridFragment : Fragment(){

    private lateinit var fragmentView :View
    private lateinit var fireStore :FirebaseFirestore
    private lateinit var recyclerview :RecyclerView
    private lateinit var recyclerviewListenerRegistration :ListenerRegistration

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView =  inflater.inflate(R.layout.fragment_grid,container,false)
        fireStore = FirebaseFirestore.getInstance()
        recyclerview = fragmentView.findViewById(R.id.gridfragment_recyclerview)

        return fragmentView
    }

    override fun onResume() {
        super.onResume()
        recyclerview.adapter = GridRecyclerViewAdapter()
        recyclerview.layoutManager = GridLayoutManager(context,3)
    }

    override fun onStop() {
        super.onStop()
        recyclerviewListenerRegistration.remove()
    }

    inner class GridRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = ArrayList()

        init {
            recyclerviewListenerRegistration = fireStore.collection("images").orderBy("timestamp",Query.Direction.DESCENDING)
                .addSnapshotListener { querySnapshot, _ ->
                    if(querySnapshot == null)return@addSnapshotListener
                    contentDTOs.clear()
                    for(snapshot in querySnapshot.documents){
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val width = resources.displayMetrics.widthPixels / 3 //화면의 가로 픽셀값
            val imageView = ImageView(context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width,width)

            return GridViewHolder(imageView)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val imageView = (holder as GridViewHolder).imageView
            imageView.loadImage(contentDTOs[position].imageUri!!,context!!)

            imageView.setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()
                bundle.putString("destinationUid",contentDTOs[position].uid)
                bundle.putString("userId",contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
            }
        }

    }

}

class GridViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)