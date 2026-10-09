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
import com.google.android.material.chip.Chip
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
            binding.tvPrice.text = state.getFormattedFinalPrice()
            binding.tvRating.text = "★ ${product.rating ?: 0.0}"
            binding.tvRatingCount.text = "(${state.reviews.size} ulasan)"

            binding.tvCategoryBadge.text = product.category.takeIf { !it.isNullOrBlank() } ?: "Produk"

            binding.tvDescription.text = if (product.description.isNullOrBlank()) {
                "Deskripsi produk belum tersedia."
            } else {
                product.description
            }

            val displayImg = state.selectedVariant?.imageUrl.takeIf { !it.isNullOrBlank() }
                ?: product.imageUrl

            binding.ivProductImage.load(displayImg) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_foreground)
                error(R.drawable.ic_launcher_foreground)
            }
        }

        // Render Variant Section
        renderVariantsUi(state)

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

        // Add to Cart Loading & Validation State
        if (state.isAddingToCart) {
            binding.btnAddToCart.isEnabled = false
            binding.pbAddToCart.visibility = View.VISIBLE
        } else {
            binding.btnAddToCart.isEnabled = state.isVariantSelectionValid
            binding.pbAddToCart.visibility = View.GONE
        }

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

    private fun renderVariantsUi(state: ProductDetailUiState) {
        if (state.variants.isEmpty()) {
            binding.layoutVariantSection.visibility = View.GONE
            return
        }

        binding.layoutVariantSection.visibility = View.VISIBLE

        // Render Color Chips
        binding.chipGroupColor.removeAllViews()
        if (state.availableColors.isNotEmpty()) {
            binding.layoutColorContainer.visibility = View.VISIBLE
            state.availableColors.forEach { color ->
                val chip = Chip(this).apply {
                    text = color
                    isCheckable = true
                    isChecked = color.equals(state.selectedColor, ignoreCase = true)
                    setOnClickListener {
                        viewModel.selectColor(color)
                    }
                }
                binding.chipGroupColor.addView(chip)
            }
        } else {
            binding.layoutColorContainer.visibility = View.GONE
        }

        // Render Size Chips
        binding.chipGroupSize.removeAllViews()
        if (state.availableSizes.isNotEmpty()) {
            binding.layoutSizeContainer.visibility = View.VISIBLE
            state.availableSizes.forEach { size ->
                val chip = Chip(this).apply {
                    text = size
                    isCheckable = true
                    isChecked = size.equals(state.selectedSize, ignoreCase = true)
                    setOnClickListener {
                        viewModel.selectSize(size)
                    }
                }
                binding.chipGroupSize.addView(chip)
            }
        } else {
            binding.layoutSizeContainer.visibility = View.GONE
        }

        binding.tvSelectedColor.text = state.selectedColor ?: "-"
        binding.tvSelectedSize.text = state.selectedSize ?: "-"
        binding.tvVariantStock.text = "${state.stock} buah"

        if (state.variantSelectionInstruction != null) {
            binding.tvVariantInstruction.visibility = View.VISIBLE
            binding.tvVariantInstruction.text = state.variantSelectionInstruction
        } else {
            binding.tvVariantInstruction.visibility = View.GONE
        }
    }
}
