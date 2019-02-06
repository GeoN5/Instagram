package com.example.instagram.activity

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_login.*
import com.example.instagram.R
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.*
import java.util.*


class LoginActivity : AppCompatActivity() {

    private var auth : FirebaseAuth? = null //로그인 관리
    private var googleSigninClient : GoogleSignInClient? = null
    private val GOOGLE_LOGIN_CODE = 9001
    private var callbackManager : CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        password_input_layout.isPasswordVisibilityToggleEnabled = true

        auth = FirebaseAuth.getInstance()
        email_login_button.setOnClickListener {
            createAndLoginEmail()
        }
        google_sign_int_button.setOnClickListener {
            googleLogin()
        }
        facebook_login_button.setOnClickListener {
            facebookLogin()
        }
        //getString(R.string.default_web_client_id) -> client - oauth_client - client_type3
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("553186965299-5c53fidh1pvani35ct5hd2avabt49rcu.apps.googleusercontent.com").requestEmail().build()
        googleSigninClient = GoogleSignIn.getClient(this,gso)
        //printHashKey()
        callbackManager = CallbackManager.Factory.create()
    }

//    private fun printHashKey() { //facebook key hash
//        try {
//            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
//            for (signature in info.signatures) {
//                val md = MessageDigest.getInstance("SHA")
//                md.update(signature.toByteArray())
//                val hashKey = String(Base64.encode(md.digest(), 0))
//                Log.d("asdf", "printHashKey() Hash Key: $hashKey")
//            }
//        } catch (e: NoSuchAlgorithmException) {
//            Log.d("asdf", "printHashKey()", e)
//        } catch (e: Exception) {
//            Log.d("asdf", "printHashKey()", e)
//        }
//    }

    private fun createAndLoginEmail() { //Authentication
        if (email_edittext.text.trim().toString().isNotEmpty() && password_edittext.text.trim().toString().isNotEmpty()) {
            auth?.createUserWithEmailAndPassword(email_edittext.text.trim().toString(), password_edittext.text.trim().toString())
                ?.addOnCompleteListener { task: Task<AuthResult> ->
                    when {
                        //계정이 없을경우(회원가입)
                        task.isSuccessful -> {
                            moveMainPage(auth?.currentUser,"회원가입 및 로그인 성공.")
                        }
                        //계정이 있고 에러가 난 경우
                        task.exception?.message.isNullOrEmpty() -> Toast.makeText(this, task.exception!!.message, Toast.LENGTH_LONG).show()
                        //계정이 있고 에러가 나지 않은 경우(로그인 메소드 호출)
                        else -> signinEmail()
                    }
                }
        } else {
            Toast.makeText(this,"이메일과 비밀번호를 입력해주세요.",Toast.LENGTH_LONG).show()
        }
    }

    private fun signinEmail(){ //Authentication
        auth?.signInWithEmailAndPassword(email_edittext.text.trim().toString(),password_edittext.text.trim().toString())
            ?.addOnCompleteListener { task: Task<AuthResult> ->
                when {
                    task.isSuccessful -> {
                        moveMainPage(auth?.currentUser)
                    }
                    else -> Toast.makeText(this, task.exception!!.message,Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun moveMainPage(user : FirebaseUser?,message:String =""){
        if(user != null){
            if(message.isEmpty()){
                Toast.makeText(this,"로그인에 성공했습니다.",Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }else{
                Toast.makeText(this, message,Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun googleLogin(){
        val signInIntent = googleSigninClient?.signInIntent
        startActivityForResult(signInIntent,GOOGLE_LOGIN_CODE)
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount){
        //idToken으로 인증서를 받고 인증서를 통한 로그인
        val credential = GoogleAuthProvider.getCredential(account.idToken,null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task: Task<AuthResult> ->
            if(task.isSuccessful){
                moveMainPage(auth?.currentUser)
            }
        }
    }

    private fun facebookLogin(){
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile","email"))
        LoginManager.getInstance().registerCallback(callbackManager,object : FacebookCallback<LoginResult>{
            override fun onSuccess(result: LoginResult?) {
                handleFacebookAccessToken(result?.accessToken!!)
            }
            override fun onCancel() {

            }
            override fun onError(error: FacebookException?) {
                Toast.makeText(this@LoginActivity, error?.message!!.toString(),Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun handleFacebookAccessToken(token: AccessToken){
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task: Task<AuthResult> ->
            if(task.isSuccessful){
                moveMainPage(auth?.currentUser)
            }
        }
    }

//    override fun onResume() { //auto login
//        super.onResume()
//        moveMainPage(auth?.currentUser)
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode,resultCode,data)

        if(requestCode == GOOGLE_LOGIN_CODE && resultCode == Activity.RESULT_OK){
            //구글 로그인 결과값을 받아옴
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result.isSuccess){
                //계정 정보를 넘김
                firebaseAuthWithGoogle(result.signInAccount!!)
            }
        }
    }

}