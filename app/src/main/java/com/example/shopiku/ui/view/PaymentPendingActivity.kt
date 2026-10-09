package com.example.shopiku.ui.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.shopiku.data.model.PaymentStatus
import com.example.shopiku.databinding.ActivityPaymentPendingBinding
import com.example.shopiku.ui.viewmodel.PaymentViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PaymentPendingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentPendingBinding
    private val viewModel: PaymentViewModel by viewModels()

    private var orderId: String = ""
    private var orderNumber: String = ""
    private var totalAmount: Double = 0.0
    private var paymentMethod: String = ""
    private var vaNumber: String = ""
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentPendingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extractIntentExtras()
        setupViews()
        setupListeners()
        observeViewModel()

        // Jangan anggap berhasil hanya karena halaman ini dibuka.
        // Ikat ke sesi yang sudah ada; status tetap PENDING sampai berubah.
        if (orderId.isNotBlank()) {
            viewModel.bindExistingSession(orderId, orderNumber, totalAmount, paymentMethod, vaNumber)
        }
    }

    private fun extractIntentExtras() {
        orderId = intent.getStringExtra(PaymentActivity.EXTRA_ORDER_ID) ?: ""
        orderNumber = intent.getStringExtra(PaymentActivity.EXTRA_ORDER_NUMBER) ?: orderId
        totalAmount = intent.getDoubleExtra(PaymentActivity.EXTRA_TOTAL_AMOUNT, 0.0)
        paymentMethod = intent.getStringExtra(PaymentActivity.EXTRA_PAYMENT_METHOD)
            ?: "BCA Virtual Account"
        vaNumber = intent.getStringExtra(PaymentActivity.EXTRA_VA_NUMBER)
            ?: "8277 0812 3456 7890"
    }

    private fun setupViews() {
        binding.tvPendingOrderId.text = orderNumber
        binding.tvPendingTotalAmount.text = formatRupiah(totalAmount)
        binding.tvPendingPaymentMethod.text = paymentMethod
        binding.tvPendingVaNumber.text = vaNumber
    }

    private fun setupListeners() {
        binding.ivPendingBack.setOnClickListener {
            finish()
        }

        binding.btnCheckPendingStatus.setOnClickListener {
            viewModel.checkPaymentStatus()
        }

        binding.btnSimulateSuccessFromPending.setOnClickListener {
            if (hasNavigated) return@setOnClickListener
            viewModel.simulateStatusChange(PaymentStatus.SUCCESS, navigate = false)
        }

        binding.btnSimulateExpiredFromPending.setOnClickListener {
            if (hasNavigated) return@setOnClickListener
            viewModel.simulateStatusChange(PaymentStatus.EXPIRED, navigate = false)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isCheckingStatus) {
                        binding.btnCheckPendingStatus.isEnabled = false
                        binding.btnCheckPendingStatus.text = "Memeriksa Status..."
                    } else {
                        binding.btnCheckPendingStatus.isEnabled = true
                        binding.btnCheckPendingStatus.text = "Cek Status Pembayaran"
                    }

                    when {
                        PaymentStatus.isSuccess(state.currentStatus) -> openSuccessActivity()
                        PaymentStatus.isExpired(state.currentStatus) -> openExpiredActivity()
                        state.pendingCheckNotPaid -> {
                            Toast.makeText(
                                this@PaymentPendingActivity,
                                "Pembayaran belum diterima. Status masih PENDING.",
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.clearPendingCheckFlag()
                        }
                    }

                    state.errorMessage?.let { msg ->
                        Toast.makeText(this@PaymentPendingActivity, msg, Toast.LENGTH_LONG).show()
                        viewModel.clearErrorMessage()
                    }
                }
            }
        }
    }

    private fun openSuccessActivity() {
        if (hasNavigated) return
        hasNavigated = true
        val intentSuccess = Intent(this, PaymentSuccessActivity::class.java).apply {
            putExtra(PaymentActivity.EXTRA_ORDER_ID, orderId)
            putExtra(PaymentActivity.EXTRA_ORDER_NUMBER, orderNumber)
            putExtra(PaymentActivity.EXTRA_TOTAL_AMOUNT, totalAmount)
            putExtra(PaymentActivity.EXTRA_PAYMENT_METHOD, paymentMethod)
        }
        startActivity(intentSuccess)
        finish()
    }

    private fun openExpiredActivity() {
        if (hasNavigated) return
        hasNavigated = true
        val intentExpired = Intent(this, PaymentExpiredActivity::class.java).apply {
            putExtra(PaymentActivity.EXTRA_ORDER_ID, orderId)
            putExtra(PaymentActivity.EXTRA_ORDER_NUMBER, orderNumber)
            putExtra(PaymentActivity.EXTRA_TOTAL_AMOUNT, totalAmount)
            putExtra(PaymentActivity.EXTRA_PAYMENT_METHOD, paymentMethod)
        }
        startActivity(intentExpired)
        finish()
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
