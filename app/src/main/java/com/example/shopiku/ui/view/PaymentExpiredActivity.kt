package com.example.shopiku.ui.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.shopiku.data.model.PaymentStatus
import com.example.shopiku.data.repository.PaymentSessionStore
import com.example.shopiku.databinding.ActivityPaymentExpiredBinding
import java.text.NumberFormat
import java.util.Locale

class PaymentExpiredActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentExpiredBinding

    private var orderId: String = ""
    private var orderNumber: String = ""
    private var totalAmount: Double = 0.0
    private var paymentMethod: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentExpiredBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extractIntentExtras()
        setupViews()
        setupListeners()
    }

    private fun extractIntentExtras() {
        orderId = intent.getStringExtra(PaymentActivity.EXTRA_ORDER_ID) ?: ""
        orderNumber = intent.getStringExtra(PaymentActivity.EXTRA_ORDER_NUMBER) ?: orderId
        totalAmount = intent.getDoubleExtra(PaymentActivity.EXTRA_TOTAL_AMOUNT, 0.0)
        paymentMethod = intent.getStringExtra(PaymentActivity.EXTRA_PAYMENT_METHOD)
            ?: "BCA Virtual Account"
    }

    private fun setupViews() {
        binding.tvExpiredOrderId.text = orderNumber
        binding.tvExpiredTotalAmount.text = formatRupiah(totalAmount)
        binding.tvExpiredPaymentMethod.text = paymentMethod
    }

    private fun setupListeners() {
        binding.ivExpiredBack.setOnClickListener {
            finish()
        }

        binding.btnRetryPayment.setOnClickListener {
            // Inisialisasi ulang sesi pembayaran (status kembali PENDING, VA baru).
            // Jangan ubah EXPIRED menjadi SUCCESS tanpa simulasi pembayaran baru.
            PaymentSessionStore.clear(orderId)

            val retryIntent = Intent(this, PaymentActivity::class.java).apply {
                putExtra(PaymentActivity.EXTRA_ORDER_ID, orderId)
                putExtra(PaymentActivity.EXTRA_ORDER_NUMBER, orderNumber)
                putExtra(PaymentActivity.EXTRA_TOTAL_AMOUNT, totalAmount)
                putExtra(PaymentActivity.EXTRA_RESET_SESSION, true)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(retryIntent)
            finish()
        }

        binding.btnGoToOrders.setOnClickListener {
            val ordersIntent = Intent(this, TrackingActivity::class.java).apply {
                putExtra(PaymentActivity.EXTRA_ORDER_ID, orderId)
                putExtra(PaymentActivity.EXTRA_ORDER_NUMBER, orderNumber)
                putExtra(PaymentActivity.EXTRA_PAYMENT_STATUS, PaymentStatus.EXPIRED)
            }
            startActivity(ordersIntent)
            finish()
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
