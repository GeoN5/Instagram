package com.example.instagram.util

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

fun ImageView.loadImage(url : Uri, context: Context){
    Glide.with(context).load(url).apply(RequestOptions().centerCrop()).into(this)
}

//fun ImageView.loadImage(url : Uri, context: Context){ //캐시 삭제후 로딩
//    Glide.with(context).load(url).apply(RequestOptions().centerCrop().skipMemoryCache(true)
//        .diskCacheStrategy(DiskCacheStrategy.NONE)).into(this)
//}
