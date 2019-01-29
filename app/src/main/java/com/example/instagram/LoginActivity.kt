package com.example.instagram

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

    private fun createAndLoginEmail() { //Authentication
        if (email_edittext.text.trim().toString().isNotEmpty() && password_edittext.text.trim().toString().isNotEmpty()) {
            auth?.createUserWithEmailAndPassword(email_edittext.text.trim().toString(), password_edittext.text.trim().toString())
                ?.addOnCompleteListener { task: Task<AuthResult> ->
                    when {
                        //계정이 없을경우
                        task.isSuccessful -> {
                            Toast.makeText(this, "아이디 생성이 완료되었습니다.", Toast.LENGTH_LONG).show()
                            moveMainPage(auth?.currentUser)
                        }
                        //계정이 있고 에러가 난 경우
                        task.exception?.message.isNullOrEmpty() -> Toast.makeText(this, task.exception!!.message, Toast.LENGTH_LONG).show()
                        //계정이 있고 에러가 나지 않은 경우
                        else -> signinEmail()
                    }
                }
        } else {
            Toast.makeText(this,"Please enter your email and password",Toast.LENGTH_LONG).show()
        }
    }

    private fun signinEmail(){ //Authentication
        auth?.signInWithEmailAndPassword(email_edittext.text.trim().toString(),password_edittext.text.trim().toString())
            ?.addOnCompleteListener { task: Task<AuthResult> ->
                when {
                    task.isSuccessful -> {
                        Toast.makeText(this,"로그인이 성공했습니다.",Toast.LENGTH_LONG).show()
                        moveMainPage(auth?.currentUser)
                    }
                    else -> Toast.makeText(this, task.exception!!.message,Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun moveMainPage(user : FirebaseUser?){
        if(user != null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }

}