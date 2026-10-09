package com.example.shopiku.ui.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.shopiku.data.model.PaymentStatus
import com.example.shopiku.databinding.ActivityPaymentBinding
import com.example.shopiku.ui.viewmodel.PaymentNavEvent
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
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extractIntentExtras()
        setupListeners()
        observeUiState()
        observeNavEvents()

        if (orderId.isNotBlank()) {
            if (intent.getBooleanExtra(EXTRA_RESET_SESSION, false)) {
                viewModel.resetPaymentSessionAfterClear(orderId, orderNumber, totalAmount, "BCA")
            } else {
                viewModel.initializePayment(orderId, orderNumber, totalAmount, "BCA")
            }
        } else {
            Toast.makeText(this, "ID Pesanan tidak valid.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIntentExtras()
        if (orderId.isNotBlank()) {
            if (intent.getBooleanExtra(EXTRA_RESET_SESSION, false)) {
                viewModel.resetPaymentSessionAfterClear(orderId, orderNumber, totalAmount, "BCA")
            } else {
                viewModel.syncStatusFromStore()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isNavigating = false
        viewModel.syncStatusFromStore()
    }

    private fun extractIntentExtras() {
        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
        orderNumber = intent.getStringExtra(EXTRA_ORDER_NUMBER) ?: "ORD-${System.currentTimeMillis()}"
        totalAmount = intent.getDoubleExtra(EXTRA_TOTAL_AMOUNT, 0.0)
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

        binding.btnSimulatePayment.setOnClickListener {
            if (!canClickSimulate()) return@setOnClickListener
            viewModel.simulateStatusChange(PaymentStatus.SUCCESS, navigate = true)
        }

        binding.btnSimulatePending.setOnClickListener {
            if (!canClickSimulate()) return@setOnClickListener
            viewModel.simulateStatusChange(PaymentStatus.PENDING, navigate = true)
        }

        binding.btnSimulateExpired.setOnClickListener {
            if (!canClickSimulate()) return@setOnClickListener
            viewModel.simulateStatusChange(PaymentStatus.EXPIRED, navigate = true)
        }
    }

    private fun canClickSimulate(): Boolean {
        val state = viewModel.uiState.value
        if (!state.canSimulate || PaymentStatus.isSuccess(state.currentStatus)) {
            Toast.makeText(
                this,
                "Transaksi ini sudah berhasil. Simulasi ulang tidak diizinkan.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (isNavigating || state.isCheckingStatus) return false
        return true
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

    private fun observeNavEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navEvent.collect { event ->
                    if (isNavigating) return@collect
                    isNavigating = true
                    when (event) {
                        is PaymentNavEvent.ToSuccess -> openSuccess(event)
                        is PaymentNavEvent.ToPending -> openPending(event)
                        is PaymentNavEvent.ToExpired -> openExpired(event)
                    }
                }
            }
        }
    }

    private fun openSuccess(event: PaymentNavEvent.ToSuccess) {
        val intent = Intent(this, PaymentSuccessActivity::class.java).apply {
            putExtra(EXTRA_ORDER_ID, event.orderId)
            putExtra(EXTRA_ORDER_NUMBER, event.orderNumber)
            putExtra(EXTRA_TOTAL_AMOUNT, event.amount)
            putExtra(EXTRA_PAYMENT_METHOD, event.paymentMethod)
        }
        startActivity(intent)
    }

    private fun openPending(event: PaymentNavEvent.ToPending) {
        val intent = Intent(this, PaymentPendingActivity::class.java).apply {
            putExtra(EXTRA_ORDER_ID, event.orderId)
            putExtra(EXTRA_ORDER_NUMBER, event.orderNumber)
            putExtra(EXTRA_TOTAL_AMOUNT, event.amount)
            putExtra(EXTRA_PAYMENT_METHOD, event.paymentMethod)
            putExtra(EXTRA_VA_NUMBER, event.vaNumber)
        }
        startActivity(intent)
    }

    private fun openExpired(event: PaymentNavEvent.ToExpired) {
        val intent = Intent(this, PaymentExpiredActivity::class.java).apply {
            putExtra(EXTRA_ORDER_ID, event.orderId)
            putExtra(EXTRA_ORDER_NUMBER, event.orderNumber)
            putExtra(EXTRA_TOTAL_AMOUNT, event.amount)
            putExtra(EXTRA_PAYMENT_METHOD, event.paymentMethod)
        }
        startActivity(intent)
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

        val simulationEnabled = state.canSimulate && !state.isCheckingStatus
        binding.btnSimulatePayment.isEnabled = simulationEnabled
        binding.btnSimulatePending.isEnabled = simulationEnabled
        binding.btnSimulateExpired.isEnabled = simulationEnabled
        binding.btnSimulatePayment.alpha = if (simulationEnabled) 1f else 0.5f
        binding.btnSimulatePending.alpha = if (simulationEnabled) 1f else 0.5f
        binding.btnSimulateExpired.alpha = if (simulationEnabled) 1f else 0.5f

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

    companion object {
        const val EXTRA_ORDER_ID = "ORDER_ID"
        const val EXTRA_ORDER_NUMBER = "ORDER_NUMBER"
        const val EXTRA_TOTAL_AMOUNT = "TOTAL_AMOUNT"
        const val EXTRA_PAYMENT_METHOD = "PAYMENT_METHOD"
        const val EXTRA_VA_NUMBER = "VA_NUMBER"
        const val EXTRA_RESET_SESSION = "RESET_SESSION"
        const val EXTRA_PAYMENT_STATUS = "PAYMENT_STATUS"
    }
}
