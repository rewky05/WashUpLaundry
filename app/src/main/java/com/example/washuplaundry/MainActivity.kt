package com.example.washuplaundry

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        Thread.sleep(3000)
        installSplashScreen()
        setContentView(R.layout.activity_main)

        val fragmentContainer = findViewById<FrameLayout>(R.id.fragmentContainer)

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(
            fragmentContainer.id,
            SignIn()
        )
        fragmentTransaction.commit()
    }
}
