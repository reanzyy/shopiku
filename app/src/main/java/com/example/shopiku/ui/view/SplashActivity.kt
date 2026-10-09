package com.example.shopiku.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.shopiku.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val splashDuration = 2000L // 2 detik transisi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Binding view komponen utama untuk efek transisi
        val ivLogo = findViewById<ImageView>(R.id.ivSplashLogo)
        val tvBrand = findViewById<TextView>(R.id.tvSplashBrand)
        val layoutLoading = findViewById<LinearLayout>(R.id.layoutLoading)

        // Tambahkan efek fade-in visual
        val fadeIn = AlphaAnimation(0.0f, 1.0f).apply {
            duration = 1000
            fillAfter = true
        }
        ivLogo.startAnimation(fadeIn)
        tvBrand.startAnimation(fadeIn)
        layoutLoading.startAnimation(fadeIn)

        // Penjadwalan transisi layar
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSessionAndNavigate()
        }, splashDuration)
    }

    private fun checkUserSessionAndNavigate() {
        val sharedPreferences = getSharedPreferences("SHOPIKU_PREFS", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("IS_LOGGED_IN", false)

        val destination = if (isLoggedIn) {
            HomeActivity::class.java
        } else {
            LoginActivity::class.java
        }

        startActivity(Intent(this, destination))
        finish() // Hapus SplashActivity dari back stack
    }
}