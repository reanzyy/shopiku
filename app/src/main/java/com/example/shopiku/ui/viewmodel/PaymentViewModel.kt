package com.example.shopiku.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.PaymentStatus
import com.example.shopiku.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class PaymentNavEvent {
    data class ToSuccess(
        val orderId: String,
        val orderNumber: String,
        val amount: Double,
        val paymentMethod: String
    ) : PaymentNavEvent()

    data class ToPending(
        val orderId: String,
        val orderNumber: String,
        val amount: Double,
        val paymentMethod: String,
        val vaNumber: String
    ) : PaymentNavEvent()

    data class ToExpired(
        val orderId: String,
        val orderNumber: String,
        val amount: Double,
        val paymentMethod: String
    ) : PaymentNavEvent()
}

class PaymentViewModel(
    private val paymentRepository: PaymentRepository = PaymentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<PaymentNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<PaymentNavEvent> = _navEvent.asSharedFlow()

    private var currentOrderId: String? = null
    private var currentOrderNumber: String = ""
    private var currentAmount: Double = 0.0
    private var currentBank: String = "BCA"

    fun initializePayment(orderId: String, orderNumber: String, amount: Double, bank: String = "BCA") {
        if (orderId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "ID Pesanan tidak valid.") }
            return
        }

        currentOrderId = orderId
        currentOrderNumber = orderNumber
        currentAmount = amount
        currentBank = bank

        val local = paymentRepository.getLocalSession(orderId)
        if (local != null) {
            applySessionToUi(local.status, local.toPaymentResponse())
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            paymentRepository.createPaymentTransaction(orderId, orderNumber, amount, bank).collect { result ->
                when (result) {
                    is UiState.Success -> {
                        applySessionToUi(result.data.status, result.data)
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

    /**
     * Mengikat Activity sekunder ke sesi yang sudah ada tanpa membuat transaksi baru.
     */
    fun bindExistingSession(
        orderId: String,
        orderNumber: String,
        amount: Double,
        paymentMethod: String? = null,
        vaNumber: String? = null
    ) {
        currentOrderId = orderId
        currentOrderNumber = orderNumber
        currentAmount = amount

        val local = paymentRepository.getLocalSession(orderId)
        if (local != null) {
            applySessionToUi(local.status, local.toPaymentResponse())
        } else {
            val fallbackMethod = paymentMethod ?: "BCA Virtual Account"
            val fallbackVa = vaNumber ?: "-"
            _uiState.update {
                it.copy(
                    paymentInfo = com.example.shopiku.data.model.PaymentResponse(
                        paymentId = "PAY-TEMP",
                        orderId = orderId,
                        orderNumber = orderNumber,
                        status = PaymentStatus.PENDING,
                        paymentMethod = fallbackMethod,
                        bank = "BCA",
                        virtualAccountNumber = fallbackVa,
                        amount = amount
                    ),
                    currentStatus = PaymentStatus.PENDING,
                    statusMessage = PaymentStatus.displayMessage(PaymentStatus.PENDING),
                    isPaymentSuccess = false,
                    canSimulate = true
                )
            }
        }
    }

    fun syncStatusFromStore() {
        val orderId = currentOrderId ?: return
        val local = paymentRepository.getLocalSession(orderId) ?: return
        applySessionToUi(local.status, local.toPaymentResponse())
    }

    fun checkPaymentStatus() {
        val orderId = currentOrderId ?: return
        if (_uiState.value.isCheckingStatus) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isCheckingStatus = true, errorMessage = null, pendingCheckNotPaid = false)
            }

            paymentRepository.getOrderPaymentStatus(orderId).collect { result ->
                when (result) {
                    is UiState.Success -> {
                        val normalized = PaymentStatus.normalize(result.data)
                        updateStatusUi(normalized, fromStatusCheck = true)
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

    /**
     * Simulasi perubahan status. Jika [navigate] true, emit event navigasi ke halaman hasil.
     */
    fun simulateStatusChange(newStatus: String, navigate: Boolean = true) {
        val orderId = currentOrderId ?: return

        if (!_uiState.value.canSimulate && PaymentStatus.isSuccess(_uiState.value.currentStatus)) {
            _uiState.update {
                it.copy(errorMessage = "Transaksi ini sudah berhasil. Simulasi ulang tidak diizinkan.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingStatus = true, errorMessage = null) }

            paymentRepository.updateOrderStatus(orderId, newStatus).collect { result ->
                when (result) {
                    is UiState.Success -> {
                        val normalized = PaymentStatus.normalize(result.data)
                        updateStatusUi(normalized)
                        if (navigate) {
                            emitNavigation(normalized)
                        }
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

    fun resetPaymentSession() {
        val orderId = currentOrderId ?: return
        resetPaymentSessionAfterClear(orderId, currentOrderNumber, currentAmount, currentBank)
    }

    /**
     * Membuat sesi pembayaran baru (mis. setelah EXPIRED → Coba Bayar Lagi).
     * Status EXPIRED tidak diubah menjadi SUCCESS tanpa simulasi baru.
     */
    fun resetPaymentSessionAfterClear(
        orderId: String,
        orderNumber: String,
        amount: Double,
        bank: String = "BCA"
    ) {
        currentOrderId = orderId
        currentOrderNumber = orderNumber
        currentAmount = amount
        currentBank = bank

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, canSimulate = true) }
            paymentRepository.resetPaymentSession(orderId, orderNumber, amount, bank).collect { result ->
                when (result) {
                    is UiState.Success -> applySessionToUi(PaymentStatus.PENDING, result.data)
                    is UiState.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                    }
                    else -> _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearPendingCheckFlag() {
        _uiState.update { it.copy(pendingCheckNotPaid = false) }
    }

    private fun applySessionToUi(status: String, paymentInfo: com.example.shopiku.data.model.PaymentResponse) {
        val normalized = PaymentStatus.normalize(status)
        val isSuccess = PaymentStatus.isSuccess(normalized)
        _uiState.update {
            it.copy(
                isLoading = false,
                isCheckingStatus = false,
                paymentInfo = paymentInfo,
                currentStatus = normalized,
                statusMessage = PaymentStatus.displayMessage(normalized),
                isPaymentSuccess = isSuccess,
                canSimulate = !isSuccess,
                pendingCheckNotPaid = false
            )
        }
    }

    private fun updateStatusUi(status: String, fromStatusCheck: Boolean = false) {
        val normalized = PaymentStatus.normalize(status)
        val isSuccess = PaymentStatus.isSuccess(normalized)
        val stillPending = PaymentStatus.isPending(normalized)

        _uiState.update {
            it.copy(
                isCheckingStatus = false,
                currentStatus = normalized,
                statusMessage = PaymentStatus.displayMessage(normalized),
                isPaymentSuccess = isSuccess,
                canSimulate = !isSuccess,
                pendingCheckNotPaid = fromStatusCheck && stillPending,
                paymentInfo = it.paymentInfo?.copy(status = normalized)
            )
        }
    }

    private suspend fun emitNavigation(status: String) {
        val orderId = currentOrderId ?: return
        val info = _uiState.value.paymentInfo
        val orderNumber = info?.orderNumber ?: currentOrderNumber
        val amount = info?.amount ?: currentAmount
        val method = info?.let { "${it.bank} Virtual Account" } ?: "$currentBank Virtual Account"
        val va = info?.virtualAccountNumber ?: ""

        when (PaymentStatus.normalize(status)) {
            PaymentStatus.SUCCESS -> _navEvent.emit(
                PaymentNavEvent.ToSuccess(orderId, orderNumber, amount, method)
            )
            PaymentStatus.PENDING -> _navEvent.emit(
                PaymentNavEvent.ToPending(orderId, orderNumber, amount, method, va)
            )
            PaymentStatus.EXPIRED -> _navEvent.emit(
                PaymentNavEvent.ToExpired(orderId, orderNumber, amount, method)
            )
        }
    }
}
