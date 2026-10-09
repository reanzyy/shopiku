package com.example.shopiku.ui.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.shopiku.databinding.ActivityPaymentBinding
import com.example.shopiku.ui.viewmodel.PaymentUiState
import com.example.shopiku.ui.viewmodel.PaymentViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private val viewModel: PaymentViewModel by viewModels()

    private var orderId: String = ""
    private var orderNumber: String = ""
    private var totalAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extractIntentExtras()
        setupListeners()
        observeUiState()

        if (orderId.isNotBlank()) {
            viewModel.initializePayment(orderId, orderNumber, totalAmount, "BCA")
        } else {
            Toast.makeText(this, "ID Pesanan tidak valid.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun extractIntentExtras() {
        orderId = intent.getStringExtra("ORDER_ID") ?: ""
        orderNumber = intent.getStringExtra("ORDER_NUMBER") ?: "ORD-${System.currentTimeMillis()}"
        totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)
    }

    private fun setupListeners() {
        binding.ivPaymentBack.setOnClickListener {
            finish()
        }

        binding.btnCopyVA.setOnClickListener {
            val va = binding.tvVirtualAccount.text.toString()
            copyToClipboard("Nomor Virtual Account", va)
        }

        binding.btnCopyAmount.setOnClickListener {
            val amount = binding.tvPaymentTotal.text.toString()
            copyToClipboard("Total Tagihan", amount)
        }

        binding.btnCheckStatus.setOnClickListener {
            viewModel.checkPaymentStatus()
        }

        // Sandbox Simulation Buttons
        binding.btnSimulatePayment.setOnClickListener {
            viewModel.simulateStatusChange("paid")
        }

        binding.btnSimulatePending.setOnClickListener {
            viewModel.simulateStatusChange("pending")
        }

        binding.btnSimulateExpired.setOnClickListener {
            viewModel.simulateStatusChange("expired")
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$label berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
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

    private fun renderUiState(state: PaymentUiState) {
        val info = state.paymentInfo
        if (info != null) {
            binding.tvOrderId.text = info.orderNumber ?: info.orderId
            binding.tvBankName.text = "${info.bank} Virtual Account"
            binding.tvVirtualAccount.text = info.virtualAccountNumber
            binding.tvPaymentTotal.text = formatRupiah(info.amount)
        }

        binding.tvPaymentStatus.text = state.statusMessage

        if (state.isCheckingStatus) {
            binding.btnCheckStatus.isEnabled = false
            binding.btnCheckStatus.text = "Memeriksa Status..."
        } else {
            binding.btnCheckStatus.isEnabled = true
            binding.btnCheckStatus.text = "Cek Status Pembayaran"
        }

        if (state.isPaymentSuccess) {
            Snackbar.make(binding.root, "Pembayaran Berhasil Dikonfirmasi!", Snackbar.LENGTH_INDEFINITE)
                .setAction("Lacak Pesanan") {
                    startActivity(Intent(this, TrackingActivity::class.java))
                    finish()
                }
                .show()
        }

        state.errorMessage?.let { msg ->
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
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
}
