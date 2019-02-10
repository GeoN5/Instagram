package com.example.instagram.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.example.instagram.R
import com.example.instagram.fragment.AlarmFragment
import com.example.instagram.fragment.DetailviewFragment
import com.example.instagram.fragment.GridFragment
import com.example.instagram.fragment.UserFragment
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() ,BottomNavigationView.OnNavigationItemSelectedListener{

    private val PICK_PROFILE_FROM_ALBUM = 10
    private lateinit var storage : FirebaseStorage
    private lateinit var auth : FirebaseAuth
    private lateinit var firestore : FirebaseFirestore

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setToolbarDefault()
        when(item.itemId){
            R.id.action_home -> {
                supportFragmentManager.beginTransaction().replace(R.id.main_content,DetailviewFragment()).commit()
                return true
            }
            R.id.action_search -> {
                supportFragmentManager.beginTransaction().replace(R.id.main_content,GridFragment()).commit()
                return true
            }
            R.id.action_add_photo -> {
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ) {
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                }
                return true
            }
            R.id.action_favorite_alarm -> {
                supportFragmentManager.beginTransaction().replace(R.id.main_content,AlarmFragment()).commit()
                return true
            }
            R.id.action_account -> {
                val userFragment = UserFragment()
                val bundle = Bundle()
                bundle.putString("destinationUid", FirebaseAuth.getInstance().currentUser!!.uid) //자신의 uid를 넘김
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.main_content,userFragment).commit()
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseInit()

        bottom_navigation.setOnNavigationItemSelectedListener(this)
        bottom_navigation.selectedItemId = R.id.action_home
        //photo
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
    }

    private fun firebaseInit(){
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    private fun setToolbarDefault(){
        toolbar_btn_back.visibility = View.GONE
        toolbar_username.visibility = View.GONE
        toolbar_title_image.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this).setIcon(R.drawable.app_icon).setTitle("Instagram")
            .setMessage("앱을 종료하시겠습니까?").setPositiveButton("확인") { _, _ ->
                ActivityCompat.finishAffinity(this)
            }
            .setNegativeButton("취소"){ dialog, _ ->
                dialog.cancel()
            }.create().show()
    }

    //UserFragment의 startActivityForResult() 처리는 해당 뷰를 담는 액티비티에서 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK){
            val imageUri = data?.data
            val uid = auth.currentUser?.uid
            //uid 값으로 만들어서 프로필이 덮어쓰기
            val storageRef = storage.reference.child("userProfileImages").child(uid!!)

            storageRef.putFile(imageUri!!).addOnFailureListener {
                    Toast.makeText(this, getString(R.string.upload_fail),Toast.LENGTH_SHORT).show()
                }.addOnSuccessListener {
                    Toast.makeText(this,getString(R.string.upload_success),Toast.LENGTH_LONG).show()

                   storageRef.downloadUrl.addOnCompleteListener { task: Task<Uri> ->
                        val uri = task.result.toString() //업로드된 이미지 주소
                        val map = HashMap<String,Any>()
                        map["image"] = uri
                        firestore.collection("profileImages").document(uid).set(map)
                    }
                }
        }
    }
}
