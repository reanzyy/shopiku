package com.example.shopiku.ui.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.repository.ProductRepository
import com.example.shopiku.databinding.ActivityReviewsBinding
import com.example.shopiku.ui.adapter.ReviewAdapter
import kotlinx.coroutines.launch

class ReviewsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewsBinding
    private val reviewAdapter = ReviewAdapter()
    private val repository = ProductRepository()

    private var productId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productId = intent.getLongExtra("PRODUCT_ID", -1L)
        if (productId == -1L) {
            val stringId = intent.getStringExtra("PRODUCT_ID")
            productId = stringId?.toLongOrNull() ?: -1L
        }

        setupRecyclerView()
        setupListeners()

        if (productId > 0L) {
            fetchReviews(productId)
        } else {
            showEmpty("ID produk tidak valid.")
        }
    }

    private fun setupRecyclerView() {
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(this@ReviewsActivity)
            adapter = reviewAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBackReviews.setOnClickListener {
            finish()
        }
    }

    private fun fetchReviews(id: Long) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.getProductReviews(id).collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.pbReviewsList.visibility = View.VISIBLE
                            binding.tvEmptyReviews.visibility = View.GONE
                            binding.rvReviews.visibility = View.GONE
                        }
                        is UiState.Success -> {
                            binding.pbReviewsList.visibility = View.GONE
                            binding.tvEmptyReviews.visibility = View.GONE
                            binding.rvReviews.visibility = View.VISIBLE
                            reviewAdapter.submitList(state.data)
                        }
                        is UiState.Empty -> {
                            showEmpty("Belum ada ulasan untuk produk ini.")
                        }
                        is UiState.Error -> {
                            showEmpty(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun showEmpty(message: String) {
        binding.pbReviewsList.visibility = View.GONE
        binding.rvReviews.visibility = View.GONE
        binding.tvEmptyReviews.visibility = View.VISIBLE
        binding.tvEmptyReviews.text = message
    }
}
