package com.example.instagram

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
}
