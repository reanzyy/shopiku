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

    fun fetchCart() {
        viewModelScope.launch {
            repository.getCartFullItems().collect { state ->
                _cartState.value = state
            }
        }
    }

    fun increaseQuantity(item: CartItem) {
        val cartId = item.id ?: return
        val productId = item.productId.toLongOrNull() ?: return
        val newQty = item.quantity + 1

        viewModelScope.launch {
            repository.updateCartItemQuantity(cartId, productId, newQty).collect { result ->
                if (result is UiState.Success) {
                    fetchCart()
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

        viewModelScope.launch {
            repository.updateCartItemQuantity(cartId, productId, newQty).collect { result ->
                if (result is UiState.Success) {
                    fetchCart()
                }
            }
        }
    }

    fun deleteItem(item: CartItem) {
        val cartId = item.id ?: return
        viewModelScope.launch {
            repository.deleteCartItemById(cartId).collect { result ->
                if (result is UiState.Success) {
                    fetchCart()
                }
            }
        }
    }
}
