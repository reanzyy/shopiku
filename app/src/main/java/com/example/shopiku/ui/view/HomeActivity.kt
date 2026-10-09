package com.example.shopiku.ui.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopiku.R
import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.FlashSaleItem
import com.example.shopiku.databinding.ActivityHomeBinding
import com.example.shopiku.ui.adapter.FlashSaleAdapter
import com.example.shopiku.ui.adapter.ProductAdapter
import com.example.shopiku.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter
    private lateinit var flashSaleAdapter: FlashSaleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupBottomNavigation()
        setupFlashSale()
        setupRecommendedProducts()
        observeRecommendedProducts()
    }

    private fun setupListeners() {
        binding.swipeRefreshHome.setOnRefreshListener {
            viewModel.fetchRecommendedProducts()
        }

        binding.etSearchHome.setOnClickListener {
            val query = binding.etSearchHome.text?.toString() ?: ""
            val intent = Intent(this, ProductsActivity::class.java).apply {
                putExtra(ProductsActivity.EXTRA_SEARCH_QUERY, query)
            }
            startActivity(intent)
        }

        binding.etSearchHome.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearchHome.text?.toString() ?: ""
                val intent = Intent(this, ProductsActivity::class.java).apply {
                    putExtra(ProductsActivity.EXTRA_SEARCH_QUERY, query)
                }
                startActivity(intent)
                true
            } else {
                false
            }
        }

        binding.tvSeeAllFlashSale.setOnClickListener {
            startActivity(Intent(this, ProductsActivity::class.java))
        }

        binding.ivProfileAvatarSmall.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.gridCategoriesStatic.setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java))
        }
    }

    private fun setupFlashSale() {
        flashSaleAdapter = FlashSaleAdapter { item ->
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("PRODUCT_ID", item.id)
                putExtra("PRODUCT_NAME", item.name)
            }
            startActivity(intent)
        }

        binding.rvFlashSale.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = flashSaleAdapter
        }

        // Tampilkan dummy flash sale langsung tanpa koneksi Supabase
        flashSaleAdapter.submitList(FlashSaleItem.getDummyFlashSaleList())
    }

    private fun setupRecommendedProducts() {
        productAdapter = ProductAdapter { product ->
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("PRODUCT_ID", product.id)
                putExtra("PRODUCT_NAME", product.name)
            }
            startActivity(intent)
        }

        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 2)
            adapter = productAdapter
        }

        binding.btnLoadMoreProducts.setOnClickListener {
            startActivity(Intent(this, ProductsActivity::class.java))
        }
    }

    private fun observeRecommendedProducts() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recommendedState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.swipeRefreshHome.isRefreshing = true
                        }
                        is UiState.Success -> {
                            binding.swipeRefreshHome.isRefreshing = false
                            productAdapter.submitList(state.data)
                        }
                        is UiState.Error -> {
                            binding.swipeRefreshHome.isRefreshing = false
                        }
                        is UiState.Empty -> {
                            binding.swipeRefreshHome.isRefreshing = false
                        }
                    }
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_categories -> {
                    startActivity(Intent(this, CategoriesActivity::class.java))
                    true
                }
                R.id.nav_cart -> {
                    startActivity(Intent(this, CartActivity::class.java))
                    true
                }
                R.id.nav_orders -> {
                    startActivity(Intent(this, TrackingActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
