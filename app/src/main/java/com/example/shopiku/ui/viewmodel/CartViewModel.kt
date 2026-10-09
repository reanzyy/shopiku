package com.example.shopiku.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopiku.data.model.CartItem
import com.example.shopiku.data.repository.CartRepository
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {
    private val repository = CartRepository()

    private val _cartItems = MutableLiveData<List<CartItem>>()
    val cartItems: LiveData<List<CartItem>> get() = _cartItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun fetchCart() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getCartItems()
                if (response.isSuccessful) {
                    _cartItems.value = response.body() ?: emptyList()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Gagal memuat keranjang: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Terjadi kesalahan jaringan"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateQuantity(item: CartItem, newQty: Int) {
        if (newQty <= 0) {
            deleteItem(item)
            return
        }
        val id = item.id ?: return
        viewModelScope.launch {
            try {
                val updatedPayload = item.copy(quantity = newQty)
                val response = repository.updateQuantity(id, updatedPayload)
                if (response.isSuccessful) {
                    fetchCart()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memperbarui kuantitas"
            }
        }
    }

    fun deleteItem(item: CartItem) {
        val id = item.id ?: return
        viewModelScope.launch {
            try {
                val response = repository.deleteCartItem(id)
                if (response.isSuccessful) {
                    fetchCart()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus produk"
            }
        }
    }
}