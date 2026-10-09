package com.example.shopiku.ui.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.shopiku.data.common.UiState
import com.example.shopiku.databinding.ActivityProductDetailBinding
import com.example.shopiku.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val viewModel: ProductViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val productId = intent.getStringExtra("PRODUCT_ID") ?: ""
        val productName = intent.getStringExtra("PRODUCT_NAME") ?: ""

        if (productName.isNotEmpty()) {
            binding.tvProductName.text = productName
        }

        if (productId.isNotEmpty()) {
            viewModel.fetchProductDetail(productId)
        }

        setupListeners()
        observeDetail()
    }

    private fun setupListeners() {
        binding.toolbarProductDetail.setNavigationOnClickListener {
            finish()
        }

        binding.btnAddToCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        binding.btnBuyNow.setOnClickListener {
            startActivity(Intent(this, CheckoutActivity::class.java))
        }

        binding.tvSeeAllReviews.setOnClickListener {
            startActivity(Intent(this, ReviewsActivity::class.java))
        }

        binding.btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Lihat ${binding.tvProductName.text} di SHOPIKU!")
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Produk"))
        }
    }

    private fun observeDetail() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productDetailState.collect { state ->
                    when (state) {
                        is UiState.Success -> {
                            val product = state.data
                            binding.tvProductName.text = product.name ?: ""
                            binding.tvPrice.text = product.getFormattedPrice()
                            binding.tvRating.text = "★ ${product.rating ?: 0.0}"
                            binding.tvDescription.text = if (product.description.isNullOrEmpty()) {
                                "Deskripsi produk belum tersedia."
                            } else {
                                product.description
                            }
                        }
                        else -> {
                            // Default UI state
                        }
                    }
                }
            }
        }
    }
}
