package com.example.shopiku.ui.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.shopiku.databinding.ActivityTrackingBinding

class TrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarTracking.setNavigationOnClickListener {
            finish()
        }
    }
}
