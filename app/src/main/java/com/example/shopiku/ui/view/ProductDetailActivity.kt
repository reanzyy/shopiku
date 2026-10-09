package com.example.shopiku.ui.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.shopiku.R
import com.example.shopiku.databinding.ActivityProductDetailBinding
import com.example.shopiku.ui.adapter.ReviewAdapter
import com.example.shopiku.ui.viewmodel.ProductDetailUiState
import com.example.shopiku.ui.viewmodel.ProductDetailViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val viewModel: ProductDetailViewModel by viewModels()
    private val reviewAdapter = ReviewAdapter()

    private var currentProductId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extractProductId()
        setupRecyclerView()
        setupListeners()
        observeUiState()

        if (currentProductId > 0L) {
            viewModel.loadProductDetail(currentProductId)
        } else {
            Toast.makeText(this, "ID produk tidak valid.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun extractProductId() {
        currentProductId = intent.getLongExtra("PRODUCT_ID", -1L)
        if (currentProductId == -1L) {
            val stringId = intent.getStringExtra("PRODUCT_ID")
            currentProductId = stringId?.toLongOrNull() ?: -1L
        }
    }

    private fun setupRecyclerView() {
        binding.rvReviewsPreview.apply {
            layoutManager = LinearLayoutManager(this@ProductDetailActivity)
            adapter = reviewAdapter
        }
    }

    private fun setupListeners() {
        binding.toolbarProductDetail.setNavigationOnClickListener {
            finish()
        }

        binding.btnQtyMinus.setOnClickListener {
            viewModel.decreaseQuantity()
        }

        binding.btnQtyPlus.setOnClickListener {
            viewModel.increaseQuantity()
        }

        binding.btnAddToCart.setOnClickListener {
            viewModel.addToCart()
        }

        binding.btnBuyNow.setOnClickListener {
            viewModel.addToCart()
            startActivity(Intent(this, CartActivity::class.java))
        }

        binding.tvSeeAllReviews.setOnClickListener {
            val intent = Intent(this, ReviewsActivity::class.java).apply {
                putExtra("PRODUCT_ID", currentProductId)
            }
            startActivity(intent)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }

        binding.btnShare.setOnClickListener {
            val productName = binding.tvProductName.text.toString()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Lihat $productName di SHOPIKU!")
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Produk"))
        }

        binding.btnChat.setOnClickListener {
            Toast.makeText(this, "Fitur Chat Penjual akan segera hadir.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: ProductDetailUiState) {
        // Loading State
        if (state.isLoading) {
            binding.layoutLoading.visibility = View.VISIBLE
            binding.scrollViewContent.visibility = View.GONE
            binding.layoutError.visibility = View.GONE
            return
        }

        // Error State
        if (state.errorMessage != null) {
            binding.layoutLoading.visibility = View.GONE
            binding.scrollViewContent.visibility = View.GONE
            binding.layoutError.visibility = View.VISIBLE
            binding.tvErrorMessage.text = state.errorMessage
            return
        }

        // Success State
        binding.layoutLoading.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.scrollViewContent.visibility = View.VISIBLE

        val product = state.productDetail
        if (product != null) {
            binding.tvProductName.text = product.name
            binding.tvPrice.text = product.getFormattedPrice()
            binding.tvRating.text = "★ ${product.rating ?: 0.0}"
            binding.tvRatingCount.text = "(${state.reviews.size} ulasan)"

            binding.tvCategoryBadge.text = product.category.takeIf { !it.isNullOrBlank() } ?: "Produk"

            binding.tvDescription.text = if (product.description.isNullOrBlank()) {
                "Deskripsi produk belum tersedia."
            } else {
                product.description
            }

            binding.ivProductImage.load(product.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_foreground)
                error(R.drawable.ic_launcher_foreground)
            }
        }

        // Quantity State
        binding.tvQuantity.text = state.selectedQuantity.toString()

        // Reviews State
        if (state.isReviewsLoading) {
            binding.pbReviews.visibility = View.VISIBLE
            binding.tvNoReviews.visibility = View.GONE
            binding.rvReviewsPreview.visibility = View.GONE
        } else {
            binding.pbReviews.visibility = View.GONE
            if (state.reviews.isEmpty()) {
                binding.tvNoReviews.visibility = View.VISIBLE
                binding.rvReviewsPreview.visibility = View.GONE
            } else {
                binding.tvNoReviews.visibility = View.GONE
                binding.rvReviewsPreview.visibility = View.VISIBLE
                reviewAdapter.submitList(state.reviews.take(2))
            }
        }

        // Add to Cart Loading State
        if (state.isAddingToCart) {
            binding.btnAddToCart.isEnabled = false
            binding.pbAddToCart.visibility = View.VISIBLE
        } else {
            binding.btnAddToCart.isEnabled = true
            binding.pbAddToCart.visibility = View.GONE
        }

        // Anchor view for Snackbar so it floats above bottom navigation bar
        val anchorView: View = binding.btnAddToCart.parent as? View ?: binding.btnAddToCart

        // Snackbar Feedback
        state.successMessage?.let { msg ->
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
                .setAnchorView(anchorView)
                .setAction("Lihat Keranjang") {
                    startActivity(Intent(this, CartActivity::class.java))
                }
                .show()
            viewModel.clearAddToCartMessages()
        }

        state.addToCartErrorMessage?.let { msg ->
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
                .setAnchorView(anchorView)
                .show()
            viewModel.clearAddToCartMessages()
        }
    }
}
