package com.example.iperftesting

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class MyIntroScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_intro_screen)
//
//        Handler(Looper.getMainLooper()).postDelayed({
//            val intent = Intent(this, IperfInputs::class.java)
//            finish()
//            startActivity(intent)
//        },4000)
    }
}