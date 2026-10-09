package com.example.shopiku.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.Address
import com.example.shopiku.data.model.CheckoutItem
import com.example.shopiku.data.model.CheckoutRequest
import com.example.shopiku.data.model.ShippingMethod
import com.example.shopiku.data.repository.CartRepository
import com.example.shopiku.data.repository.CheckoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CheckoutViewModel(
    private val cartRepository: CartRepository = CartRepository(),
    private val checkoutRepository: CheckoutRepository = CheckoutRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        loadCheckoutItems()
    }

    fun loadCheckoutItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            cartRepository.getCartFullItems().collect { cartState ->
                when (cartState) {
                    is UiState.Success -> {
                        val checkoutItems = cartState.data.map { cartItem ->
                            CheckoutItem(
                                cartItemId = cartItem.id,
                                productId = cartItem.productId.toLongOrNull() ?: 1L,
                                productName = cartItem.name,
                                price = cartItem.price,
                                quantity = cartItem.quantity,
                                subtotal = cartItem.getTotalPrice(),
                                imageUrl = cartItem.imageUrl
                            )
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                items = checkoutItems,
                                errorMessage = null
                            )
                        }
                    }
                    is UiState.Empty -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                items = emptyList(),
                                errorMessage = "Keranjang Anda kosong."
                            )
                        }
                    }
                    is UiState.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = cartState.message
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

    fun updateAddress(recipientName: String, phone: String, fullAddress: String) {
        val newAddr = Address(
            recipientName = recipientName,
            phone = phone,
            fullAddress = fullAddress,
            city = "Kota Cirebon",
            province = "Jawa Barat",
            postalCode = "45132"
        )
        _uiState.update { it.copy(address = newAddr) }
    }

    fun setShippingMethod(method: ShippingMethod) {
        _uiState.update { it.copy(selectedShippingMethod = method) }
    }

    fun setPaymentMethod(methodName: String) {
        _uiState.update { it.copy(selectedPaymentMethodName = methodName) }
    }

    fun applyPromoCode(code: String) {
        if (code.equals("HEMAT20K", ignoreCase = true)) {
            _uiState.update {
                it.copy(
                    promoCode = "HEMAT20K",
                    discountVoucher = 20000.0,
                    errorMessage = null
                )
            }
        } else {
            _uiState.update {
                it.copy(errorMessage = "Kode promo '$code' tidak valid.")
            }
        }
    }

    fun removePromoCode() {
        _uiState.update {
            it.copy(promoCode = null, discountVoucher = 0.0)
        }
    }

    fun createOrder() {
        val currentState = _uiState.value

        // 1. Cegah request ganda jika pembuatan order sedang berlangsung
        if (currentState.isCreatingOrder) return

        // 2. Validasi items
        if (currentState.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Keranjang belanja kosong. Tidak dapat membuat pesanan.") }
            return
        }

        // 3. Validasi alamat
        val addr = currentState.address
        if (addr.recipientName.isBlank() || addr.phone.isBlank() || addr.fullAddress.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Alamat penerima dan nomor telepon wajib diisi lengkap.") }
            return
        }

        val grandTotal = currentState.getSummary().getGrandTotal()
        val formattedFullAddress = "${addr.recipientName} (${addr.phone}) - ${addr.getFormattedAddress()}"

        val request = CheckoutRequest(
            totalAmount = grandTotal,
            shippingAddress = formattedFullAddress,
            paymentMethod = currentState.selectedPaymentMethodName
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCreatingOrder = true,
                    orderSuccessMessage = null,
                    errorMessage = null
                )
            }

            checkoutRepository.createOrder(request).collect { result ->
                when (result) {
                    is UiState.Loading -> {
                        _uiState.update { it.copy(isCreatingOrder = true) }
                    }
                    is UiState.Success -> {
                        _uiState.update {
                            it.copy(
                                isCreatingOrder = false,
                                createdOrder = result.data,
                                orderSuccessMessage = "Pesanan ${result.data.orderNumber} berhasil dibuat!"
                            )
                        }
                    }
                    is UiState.Error -> {
                        _uiState.update {
                            it.copy(
                                isCreatingOrder = false,
                                errorMessage = result.message
                            )
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(isCreatingOrder = false) }
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                orderSuccessMessage = null,
                errorMessage = null
            )
        }
    }
}
