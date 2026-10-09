package com.example.shopiku.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaymentViewModel(
    private val paymentRepository: PaymentRepository = PaymentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var currentOrderId: String? = null

    fun initializePayment(orderId: String, orderNumber: String, amount: Double, bank: String = "BCA") {
        if (orderId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "ID Pesanan tidak valid.") }
            return
        }

        currentOrderId = orderId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            paymentRepository.createPaymentTransaction(orderId, orderNumber, amount, bank).collect { result ->
                when (result) {
                    is UiState.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                paymentInfo = result.data,
                                currentStatus = "pending",
                                statusMessage = "Status Transaksi Saat Ini\nMenunggu Pembayaran (Pending)"
                            )
                        }
                    }
                    is UiState.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun checkPaymentStatus() {
        val orderId = currentOrderId ?: return
        if (_uiState.value.isCheckingStatus) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingStatus = true, errorMessage = null) }

            paymentRepository.getOrderPaymentStatus(orderId).collect { result ->
                when (result) {
                    is UiState.Success -> {
                        val status = result.data
                        updateStatusUi(status)
                    }
                    is UiState.Error -> {
                        _uiState.update {
                            it.copy(
                                isCheckingStatus = false,
                                errorMessage = result.message
                            )
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(isCheckingStatus = false) }
                    }
                }
            }
        }
    }

    fun simulateStatusChange(newStatus: String) {
        val orderId = currentOrderId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingStatus = true) }

            paymentRepository.updateOrderStatus(orderId, newStatus).collect { result ->
                when (result) {
                    is UiState.Success -> {
                        updateStatusUi(newStatus)
                    }
                    is UiState.Error -> {
                        _uiState.update {
                            it.copy(
                                isCheckingStatus = false,
                                errorMessage = result.message
                            )
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(isCheckingStatus = false) }
                    }
                }
            }
        }
    }

    private fun updateStatusUi(status: String) {
        val (message, isSuccess) = when (status.lowercase()) {
            "paid", "success" -> Pair("Status Transaksi Saat Ini\nPembayaran Berhasil (Paid)", true)
            "pending" -> Pair("Status Transaksi Saat Ini\nMenunggu Pembayaran (Pending)", false)
            "expired" -> Pair("Status Transaksi Saat Ini\nPembayaran Kedaluwarsa (Expired)", false)
            "failed" -> Pair("Status Transaksi Saat Ini\nPembayaran Gagal (Failed)", false)
            "cancelled" -> Pair("Status Transaksi Saat Ini\nPembayaran Dibatalkan (Cancelled)", false)
            else -> Pair("Status Transaksi Saat Ini\n$status", false)
        }

        _uiState.update {
            it.copy(
                isCheckingStatus = false,
                currentStatus = status,
                statusMessage = message,
                isPaymentSuccess = isSuccess
            )
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
