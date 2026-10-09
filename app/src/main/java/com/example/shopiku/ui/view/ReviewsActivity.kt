package com.example.shopiku.ui.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.shopiku.databinding.ActivityReviewsBinding

class ReviewsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackReviews.setOnClickListener {
            finish()
        }
    }
}
