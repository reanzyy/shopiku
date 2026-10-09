package com.example.shopiku.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.CartItem
import com.example.shopiku.data.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CartViewModel(
    private val repository: CartRepository = CartRepository()
) : ViewModel() {

    private val _cartState = MutableStateFlow<UiState<List<CartItem>>>(UiState.Loading)
    val cartState: StateFlow<UiState<List<CartItem>>> = _cartState.asStateFlow()

    init {
        fetchCart()
    }

    fun fetchCart(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading && _cartState.value !is UiState.Success) {
                _cartState.value = UiState.Loading
            }
            repository.getCartFullItems().collect { state ->
                // Jika background sync (showLoading == false), jangan timpa dengan UiState.Loading
                if (!showLoading && state is UiState.Loading) {
                    return@collect
                }
                _cartState.value = state
            }
        }
    }

    fun increaseQuantity(item: CartItem) {
        val cartId = item.id ?: return
        val productId = item.productId.toLongOrNull() ?: return
        val newQty = item.quantity + 1

        // 1. Optimistic Update: Perbarui tampilan secara instan tanpa reload / flicker
        val currentSuccess = _cartState.value as? UiState.Success
        val currentList = currentSuccess?.data?.toMutableList()
        val index = currentList?.indexOfFirst { it.id == cartId } ?: -1
        if (index != -1 && currentList != null) {
            currentList[index] = currentList[index].copy(quantity = newQty)
            _cartState.value = UiState.Success(currentList.toList())
        }

        // 2. Sinkronisasi ke server di background tanpa memicu UiState.Loading
        viewModelScope.launch {
            repository.updateCartItemQuantity(cartId, productId, newQty).collect { result ->
                if (result is UiState.Error) {
                    // Revert jika gagal
                    fetchCart(showLoading = false)
                }
            }
        }
    }

    fun decreaseQuantity(item: CartItem) {
        val cartId = item.id ?: return
        val productId = item.productId.toLongOrNull() ?: return
        val newQty = item.quantity - 1

        if (newQty <= 0) {
            deleteItem(item)
            return
        }

        // 1. Optimistic Update: Perbarui tampilan secara instan tanpa reload / flicker
        val currentSuccess = _cartState.value as? UiState.Success
        val currentList = currentSuccess?.data?.toMutableList()
        val index = currentList?.indexOfFirst { it.id == cartId } ?: -1
        if (index != -1 && currentList != null) {
            currentList[index] = currentList[index].copy(quantity = newQty)
            _cartState.value = UiState.Success(currentList.toList())
        }

        // 2. Sinkronisasi ke server di background tanpa memicu UiState.Loading
        viewModelScope.launch {
            repository.updateCartItemQuantity(cartId, productId, newQty).collect { result ->
                if (result is UiState.Error) {
                    // Revert jika gagal
                    fetchCart(showLoading = false)
                }
            }
        }
    }

    fun deleteItem(item: CartItem) {
        val cartId = item.id ?: return

        // 1. Optimistic Remove: Hapus langsung dari list
        val currentSuccess = _cartState.value as? UiState.Success
        val currentList = currentSuccess?.data?.toMutableList()
        if (currentList != null) {
            currentList.removeAll { it.id == cartId }
            if (currentList.isEmpty()) {
                _cartState.value = UiState.Empty
            } else {
                _cartState.value = UiState.Success(currentList.toList())
            }
        }

        // 2. Sinkronisasi hapus ke server
        viewModelScope.launch {
            repository.deleteCartItemById(cartId).collect { result ->
                if (result is UiState.Error) {
                    fetchCart(showLoading = false)
                }
            }
        }
    }
}
