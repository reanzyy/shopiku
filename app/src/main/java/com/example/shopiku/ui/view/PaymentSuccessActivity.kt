package com.example.shopiku.ui.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.shopiku.databinding.ActivityPaymentSuccessBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PaymentSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentSuccessBinding
    private var hasFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val orderNumber = intent.getStringExtra(PaymentActivity.EXTRA_ORDER_NUMBER)
            ?: intent.getStringExtra(PaymentActivity.EXTRA_ORDER_ID)
            ?: "ORD-001"
        val totalAmount = intent.getDoubleExtra(PaymentActivity.EXTRA_TOTAL_AMOUNT, 0.0)
        val paymentMethod = intent.getStringExtra(PaymentActivity.EXTRA_PAYMENT_METHOD)
            ?: "BCA Virtual Account"

        binding.tvSuccessOrderId.text = orderNumber
        binding.tvSuccessTotalAmount.text = formatRupiah(totalAmount)
        binding.tvSuccessPaymentMethod.text = paymentMethod

        binding.ivSuccessBack.setOnClickListener {
            finishOnce()
        }

        // Tampilkan halaman selama ~2 detik lalu kembali ke PaymentActivity
        lifecycleScope.launch {
            delay(2000)
            finishOnce()
        }
    }

    private fun finishOnce() {
        if (hasFinished || isFinishing) return
        hasFinished = true
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
