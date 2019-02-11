//package com.example.instagram.util
//
//import com.bumptech.glide.Glide.init
//import com.example.instagram.model.FcmDTO
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.gson.Gson
//import okhttp3.*
//import java.io.IOException
//
//object FcmUtil{
//    var JSON = MediaType.parse("application/json; charset=utf-8")
//    var url = "https://fcm.googleapis.com/fcm/send"
//    var serverKey = "AAAAgMyDNzM:APA91bES-cGRkOl3yWcAgZNRjxb4CV8Kx4bObYUqa40wvuumxDY_aFxeRk0QTZfHUyB30fKOmBpjj4TgnHuV8DkU_u9R3Jsa94bhaiDZCt2-YSKIxX2tQtEZcnev5Rnla_GKXuU0XZ5l"
//
//    var okHttpClient : OkHttpClient = OkHttpClient()
//    var gson : Gson = Gson()
//
//
//    fun sendMessage(destinationUid:String, title: String?,message: String?){
//        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener {
//                task ->
//            if(task.isSuccessful){
//                println("pushToken" + task.result!!["pushToken"])
//                val token = task.result!!["pushToken"].toString()
//
//                val fcmDTO = FcmDTO()
//                fcmDTO.to = token
//                fcmDTO.notification?.title = title
//                fcmDTO.notification?.body = message
//
//                val body = RequestBody.create(JSON, gson.toJson(fcmDTO))
//                val request = Request.Builder()
//                    .addHeader("Content-Type","application/json")
//                    .addHeader("Authorization", "key=$serverKey")
//                    .url(url)
//                    .post(body)
//                    .build()
//
//                okHttpClient.newCall(request).enqueue(object : Callback{
//                    override fun onFailure(call: Call?, e: IOException?) {
//
//                    }
//
//                    override fun onResponse(call: Call?, response: Response?) {
//                        println(response?.body()?.string())
//
//                    }
//
//                })
//
//
//            }
//
//        }
//    }
//}