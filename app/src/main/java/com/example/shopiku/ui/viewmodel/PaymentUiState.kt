package com.example.shopiku.ui.viewmodel

import com.example.shopiku.data.model.PaymentResponse

data class PaymentUiState(
    val isLoading: Boolean = false,
    val paymentInfo: PaymentResponse? = null,
    val currentStatus: String = "pending",
    val statusMessage: String = "Menunggu Pembayaran (Pending)",
    val errorMessage: String? = null,
    val isCheckingStatus: Boolean = false,
    val isPaymentSuccess: Boolean = false
)
