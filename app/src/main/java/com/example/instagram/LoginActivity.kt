package com.example.instagram

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    var auth : FirebaseAuth? = null //로그인 관리

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        email_login_button.setOnClickListener {
            createAndLoginEmail()
        }
    }

    fun createAndLoginEmail(){
        auth?.createUserWithEmailAndPassword("geonho@gmail.com","123456")?.addOnCompleteListener { task: Task<AuthResult> ->
            if(task.isSuccessful){
                Toast.makeText(this,"아이디 생성이 완료되었습니다.",Toast.LENGTH_LONG).show()
            }
        }
    }

}