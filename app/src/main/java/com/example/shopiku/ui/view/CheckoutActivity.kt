package com.example.shopiku.ui.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopiku.data.model.ShippingMethod
import com.example.shopiku.databinding.ActivityCheckoutBinding
import com.example.shopiku.ui.adapter.CheckoutAdapter
import com.example.shopiku.ui.viewmodel.CheckoutUiState
import com.example.shopiku.ui.viewmodel.CheckoutViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private val viewModel: CheckoutViewModel by viewModels()
    private val checkoutAdapter = CheckoutAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeUiState()
    }

    private fun setupRecyclerView() {
        binding.rvCheckoutItems.apply {
            layoutManager = LinearLayoutManager(this@CheckoutActivity)
            adapter = checkoutAdapter
        }
    }

    private fun setupListeners() {
        binding.ivCheckoutBack.setOnClickListener {
            finish()
        }

        binding.btnPlaceOrder.setOnClickListener {
            viewModel.createOrder()
        }

        binding.btnChangeAddress.setOnClickListener {
            showEditAddressDialog()
        }

        binding.btnApplyPromo.setOnClickListener {
            val code = binding.etPromoCode.text?.toString() ?: ""
            if (code.isNotBlank()) {
                viewModel.applyPromoCode(code)
            } else {
                Toast.makeText(this, "Masukkan kode promo terlebih dahulu.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.radioGroupShipping.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbJntRegular.id -> {
                    viewModel.setShippingMethod(
                        ShippingMethod("jnt_reg", "J&T Express Regular", "J&T Express", 15000.0, "2 - 3 Hari")
                    )
                }
                binding.rbSiCepatReg.id -> {
                    viewModel.setShippingMethod(
                        ShippingMethod("sicepat_reg", "SiCepat REG", "SiCepat", 16000.0, "2 - 4 Hari")
                    )
                }
                binding.rbJneYes.id -> {
                    viewModel.setShippingMethod(
                        ShippingMethod("jne_yes", "JNE YES", "JNE", 28000.0, "1 Hari")
                    )
                }
            }
        }
    }

    private fun showEditAddressDialog() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val currentAddr = viewModel.uiState.value.address

        val etName = EditText(this).apply {
            hint = "Nama Penerima"
            setText(currentAddr.recipientName)
        }
        val etPhone = EditText(this).apply {
            hint = "Nomor Telepon"
            setText(currentAddr.phone)
        }
        val etAddress = EditText(this).apply {
            hint = "Alamat Lengkap (Jl, No, RT/RW, Kel, Kec)"
            setText(currentAddr.fullAddress)
        }

        layout.addView(etName)
        layout.addView(etPhone)
        layout.addView(etAddress)

        AlertDialog.Builder(this)
            .setTitle("Ubah Alamat Pengiriman")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val addr = etAddress.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty() && addr.isNotEmpty()) {
                    viewModel.updateAddress(name, phone, addr)
                } else {
                    Toast.makeText(this, "Semua kolom alamat wajib diisi.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
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

    private fun renderUiState(state: CheckoutUiState) {
        // Render Items
        checkoutAdapter.submitList(state.items)
        binding.tvItemCountBadge.text = "${state.items.size} Produk"

        // Render Address
        val addr = state.address
        binding.tvReceiverName.text = addr.recipientName
        binding.tvReceiverPhone.text = "(${addr.phone})"
        binding.tvAddress.text = addr.getFormattedAddress()

        // Render Summary
        val summary = state.getSummary()
        binding.tvSubtotalProduct.text = summary.formatRupiah(summary.subtotalProduct)
        binding.tvShippingCost.text = summary.formatRupiah(summary.shippingCost)
        binding.tvDiscountShipping.text = "-${summary.formatRupiah(summary.discountShipping)}"
        binding.tvDiscountVoucher.text = "-${summary.formatRupiah(summary.discountVoucher)}"
        binding.tvServiceFee.text = summary.formatRupiah(summary.serviceFee)

        val grandTotalStr = summary.formatRupiah(summary.getGrandTotal())
        binding.tvGrandTotal.text = grandTotalStr
        binding.tvBottomTotal.text = grandTotalStr

        // Loading state
        if (state.isCreatingOrder) {
            binding.btnPlaceOrder.isEnabled = false
            binding.pbPlaceOrder.visibility = View.VISIBLE
        } else {
            binding.btnPlaceOrder.isEnabled = true
            binding.pbPlaceOrder.visibility = View.GONE
        }

        // Order creation success -> Navigate to PaymentActivity
        state.createdOrder?.let { order ->
            Toast.makeText(this, "Pesanan berhasil dibuat!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, PaymentActivity::class.java).apply {
                putExtra("ORDER_ID", order.id)
                putExtra("ORDER_NUMBER", order.orderNumber)
                putExtra("TOTAL_AMOUNT", order.totalAmount)
            }
            startActivity(intent)
            viewModel.clearMessages()
            finish()
        }

        state.errorMessage?.let { msg ->
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }
}
