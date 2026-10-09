package com.example.shopiku.ui.viewmodel

import com.example.shopiku.data.model.PaymentResponse
import com.example.shopiku.data.model.PaymentStatus

data class PaymentUiState(
    val isLoading: Boolean = false,
    val paymentInfo: PaymentResponse? = null,
    val currentStatus: String = PaymentStatus.PENDING,
    val statusMessage: String = PaymentStatus.displayMessage(PaymentStatus.PENDING),
    val errorMessage: String? = null,
    val isCheckingStatus: Boolean = false,
    val isPaymentSuccess: Boolean = false,
    /** False setelah SUCCESS — tidak boleh simulasi ulang untuk transaksi yang sama. */
    val canSimulate: Boolean = true,
    /** One-shot: true setelah cek status dan hasilnya masih PENDING. */
    val pendingCheckNotPaid: Boolean = false
)
