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

        binding.btnRetryVariants.setOnClickListener {
            viewModel.retryVariants()
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

        renderVariantsUi(state)

        binding.tvQuantity.text = state.selectedQuantity.toString()

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

        if (state.isAddingToCart) {
            binding.btnAddToCart.isEnabled = false
            binding.pbAddToCart.visibility = View.VISIBLE
        } else {
            binding.btnAddToCart.isEnabled = state.isVariantSelectionValid
            binding.pbAddToCart.visibility = View.GONE
        }

        val anchorView: View = binding.btnAddToCart.parent as? View ?: binding.btnAddToCart

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
        when {
            state.isVariantsLoading -> {
                binding.layoutVariantSection.visibility = View.VISIBLE
                binding.pbVariants.visibility = View.VISIBLE
                binding.layoutVariantError.visibility = View.GONE
                binding.layoutVariantContent.visibility = View.GONE
            }
            state.variantsErrorMessage != null -> {
                binding.layoutVariantSection.visibility = View.VISIBLE
                binding.pbVariants.visibility = View.GONE
                binding.layoutVariantError.visibility = View.VISIBLE
                binding.layoutVariantContent.visibility = View.GONE
                binding.tvVariantsError.text = state.variantsErrorMessage
            }
            state.variants.isEmpty() -> {
                binding.layoutVariantSection.visibility = View.GONE
            }
            else -> {
                binding.layoutVariantSection.visibility = View.VISIBLE
                binding.pbVariants.visibility = View.GONE
                binding.layoutVariantError.visibility = View.GONE
                binding.layoutVariantContent.visibility = View.VISIBLE
                bindVariantChips(state)
            }
        }
    }

    private fun bindVariantChips(state: ProductDetailUiState) {
        val hasColors = state.availableColors.isNotEmpty()
        val hasSizes = state.availableSizes.isNotEmpty()
        // Keep all colors enabled so users can switch freely; sizes not available
        // for the selected color are disabled (selectColor auto-picks a valid size).
        val enabledSizes = if (hasColors && !state.selectedColor.isNullOrBlank()) {
            state.sizesForSelectedColor()
        } else {
            state.availableSizes
        }

        binding.chipGroupColor.removeAllViews()
        if (hasColors) {
            binding.layoutColorContainer.visibility = View.VISIBLE
            state.availableColors.forEach { color ->
                val chip = Chip(this).apply {
                    text = color
                    isCheckable = true
                    isEnabled = true
                    isChecked = color.equals(state.selectedColor, ignoreCase = true)
                    setOnClickListener {
                        viewModel.selectColor(color)
                    }
                }
                binding.chipGroupColor.addView(chip)
            }
            binding.tvSelectedColor.text = state.selectedColor ?: "-"
        } else {
            binding.layoutColorContainer.visibility = View.GONE
        }

        binding.chipGroupSize.removeAllViews()
        if (hasSizes) {
            binding.layoutSizeContainer.visibility = View.VISIBLE
            state.availableSizes.forEach { size ->
                val chipEnabled = enabledSizes.any { it.equals(size, ignoreCase = true) }
                val chip = Chip(this).apply {
                    text = size
                    isCheckable = true
                    isEnabled = chipEnabled
                    isChecked = size.equals(state.selectedSize, ignoreCase = true)
                    setOnClickListener {
                        if (chipEnabled) {
                            viewModel.selectSize(size)
                        }
                    }
                }
                binding.chipGroupSize.addView(chip)
            }
            binding.tvSelectedSize.text = state.selectedSize ?: "-"
        } else {
            binding.layoutSizeContainer.visibility = View.GONE
        }

        binding.tvVariantStock.text = "${state.stock} buah"

        if (state.variantSelectionInstruction != null) {
            binding.tvVariantInstruction.visibility = View.VISIBLE
            binding.tvVariantInstruction.text = state.variantSelectionInstruction
        } else {
            binding.tvVariantInstruction.visibility = View.GONE
        }
    }
}
