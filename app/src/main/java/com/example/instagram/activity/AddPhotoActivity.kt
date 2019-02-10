package com.example.instagram.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.instagram.R
import com.example.instagram.model.ContentDTO
import com.example.instagram.util.loadImage
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {

    val PICK_IMAGE_FROM_ALBUM = 0
    private lateinit var storage : FirebaseStorage
    private lateinit var photoUri : Uri
    private lateinit var auth : FirebaseAuth
    private lateinit var firestore : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        init()
        listener()

        val photoPickerIntent =  Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent,PICK_IMAGE_FROM_ALBUM)
    }

    private fun init(){
        edit_input_layout.isCounterEnabled = true
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    private fun listener(){
        addphoto_image.setOnClickListener {
            val photoPickerIntent =  Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent,PICK_IMAGE_FROM_ALBUM)
        }
        addphoto_btn_upload.setOnClickListener {
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PICK_IMAGE_FROM_ALBUM && resultCode == Activity.RESULT_OK){
            photoUri = data?.data!!
            addphoto_image.loadImage(photoUri,this)
        }else{ //갤러리에서 취소시
            finish()
        }
    }

    private fun contentUpload(){ //게시물 업로드
        val timeStamp= SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "${timeStamp}_.png"
        val storageRef = storage.reference.child("images").child(imageFileName)

        storageRef.putFile(photoUri).addOnFailureListener {
            Toast.makeText(this, getString(R.string.upload_fail),Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener {
            Toast.makeText(this,getString(R.string.upload_success),Toast.LENGTH_LONG).show()

            storageRef.downloadUrl.addOnCompleteListener {  task: Task<Uri> ->
                val uri = task.result.toString() //업로드된 이미지 주소
                val contentDTO = ContentDTO()
                contentDTO.imageUri = uri //이미지 주소
                contentDTO.uid = auth.currentUser?.uid //유저의 uid
                contentDTO.explain =  addphoto_edit_explain.text.toString() //게시물 설명
                contentDTO.userId = auth.currentUser?.email //유저 이메일
                contentDTO.timestamp = System.currentTimeMillis() //게시물 업로드 시간

                firestore.collection("images").document().set(contentDTO)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

}
