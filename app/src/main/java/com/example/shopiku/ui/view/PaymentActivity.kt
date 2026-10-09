package com.example.shopiku.ui.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.shopiku.databinding.ActivityPaymentBinding

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.ivPaymentBack.setOnClickListener {
            finish()
        }

        binding.btnCheckStatus.setOnClickListener {
            startActivity(Intent(this, TrackingActivity::class.java))
            finish()
        }

        binding.btnSimulatePayment.setOnClickListener {
            binding.tvPaymentStatus.text = "Status Transaksi Saat Ini\nPembayaran Berhasil (Success)"
            startActivity(Intent(this, TrackingActivity::class.java))
            finish()
        }
    }
}
