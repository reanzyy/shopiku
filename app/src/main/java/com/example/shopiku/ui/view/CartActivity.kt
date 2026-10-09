package com.example.shopiku.ui.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopiku.R
import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.CartItem
import com.example.shopiku.databinding.ActivityCartBinding
import com.example.shopiku.ui.adapter.CartAdapter
import com.example.shopiku.ui.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private val viewModel: CartViewModel by viewModels()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        setupBottomNavigation()
        observeCartState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchCart()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onIncreaseQty = { item -> viewModel.increaseQuantity(item) },
            onDecreaseQty = { item -> viewModel.decreaseQuantity(item) },
            onDeleteItem = { item -> viewModel.deleteItem(item) },
            onItemCheckedChange = { _, _ -> updateSummary() }
        )

        binding.rvCart.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartAdapter
        }
    }

    private fun setupListeners() {
        binding.btnCheckout.setOnClickListener {
            startActivity(Intent(this, CheckoutActivity::class.java))
        }

        binding.ivCartLogo.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            cartAdapter.setAllChecked(isChecked)
            binding.cbBottomSelectAll.isChecked = isChecked
            updateSummary()
        }

        binding.cbBottomSelectAll.setOnCheckedChangeListener { _, isChecked ->
            cartAdapter.setAllChecked(isChecked)
            binding.cbSelectAll.isChecked = isChecked
            updateSummary()
        }

        binding.btnDeleteSelected.setOnClickListener {
            val checkedItems = cartAdapter.getCheckedItems()
            checkedItems.forEach { item ->
                viewModel.deleteItem(item)
            }
        }
    }

    private fun observeCartState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cartState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.rvCart.visibility = View.GONE
                        }
                        is UiState.Success -> {
                            binding.rvCart.visibility = View.VISIBLE
                            cartAdapter.submitList(state.data) {
                                updateSummary()
                            }
                            binding.tvCartItemCountBadge.text = "(${state.data.size} Produk)"
                        }
                        is UiState.Empty -> {
                            binding.rvCart.visibility = View.GONE
                            cartAdapter.submitList(emptyList())
                            updateSummary()
                            binding.tvCartItemCountBadge.text = "(0 Produk)"
                        }
                        is UiState.Error -> {
                            binding.rvCart.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun updateSummary() {
        val checkedItems = cartAdapter.getCheckedItems()
        val totalAmount = checkedItems.sumOf { it.getTotalPrice() }
        val count = checkedItems.size

        binding.tvTotal.text = formatRupiah(totalAmount)
        binding.tvSelectedCount.text = "($count dipilih)"
        binding.btnCheckout.text = "Checkout ($count)"
        binding.tvSelectAllSubtitle.text = "$count dari ${cartAdapter.itemCount} barang siap diproses"
    }

    private fun formatRupiah(amount: Double): String {
        return try {
            val localeID = Locale("in", "ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            numberFormat.format(amount)
        } catch (e: Exception) {
            "Rp${amount.toLong()}"
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_cart

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    startActivity(Intent(this, CategoriesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_cart -> true
                R.id.nav_orders -> {
                    startActivity(Intent(this, TrackingActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
