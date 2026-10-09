package com.example.shopiku.ui.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.shopiku.data.common.UiState
import com.example.shopiku.databinding.ActivityProductsBinding
import com.example.shopiku.ui.adapter.ProductAdapter
import com.example.shopiku.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

class ProductsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SEARCH_QUERY = "EXTRA_SEARCH_QUERY"
    }

    private lateinit var binding: ActivityProductsBinding
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeSearchResults()

        // Pre-fill search query if navigated from HomeActivity or elsewhere
        val initialQuery = intent.getStringExtra(EXTRA_SEARCH_QUERY) ?: ""
        if (initialQuery.isNotEmpty()) {
            binding.etSearchProduct.setText(initialQuery)
            binding.etSearchProduct.setSelection(initialQuery.length)
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("PRODUCT_ID", product.id)
                putExtra("PRODUCT_NAME", product.name)
            }
            startActivity(intent)
        }

        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(this@ProductsActivity, 2)
            adapter = productAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBackProducts.setOnClickListener {
            finish()
        }

        binding.btnCartProducts.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        binding.btnRetry.setOnClickListener {
            viewModel.retrySearch()
        }

        // Live instant debounced search listener
        binding.etSearchProduct.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString() ?: ""
            viewModel.onSearchQueryChanged(query)
        }

        binding.etSearchProduct.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearchProduct.text?.toString() ?: ""
                viewModel.onSearchQueryChanged(query)
                true
            } else {
                false
            }
        }
    }

    private fun observeSearchResults() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchResults.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvProducts.visibility = View.GONE
                            binding.layoutError.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.GONE
                        }
                        is UiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.layoutError.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.GONE
                            binding.rvProducts.visibility = View.VISIBLE
                            productAdapter.submitList(state.data)
                            binding.tvProductCount.text = "${state.data.size} produk ditemukan"
                        }
                        is UiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvProducts.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.GONE
                            binding.layoutError.visibility = View.VISIBLE
                            binding.tvErrorMessage.text = state.message
                        }
                        is UiState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvProducts.visibility = View.GONE
                            binding.layoutError.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.VISIBLE
                            binding.tvProductCount.text = "0 produk ditemukan"
                        }
                    }
                }
            }
        }
    }
}
