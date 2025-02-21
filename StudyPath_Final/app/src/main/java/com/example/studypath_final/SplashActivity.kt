package com.example.studypath_final

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the splash screen layout
        setContentView(R.layout.activity_splash)

        // Delay for 3 seconds before transitioning to ActivityLogin
        Handler().postDelayed({
            // After the splash screen, navigate to ActivityLogin
            val intent = Intent(this@SplashActivity, ActivityLogin::class.java)
            startActivity(intent)
            finish() // Close SplashActivity to prevent it from staying in the back stack
        }, 3000) // 3-second delay
    }
}
