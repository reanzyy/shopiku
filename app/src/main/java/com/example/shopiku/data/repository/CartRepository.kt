package com.example.shopiku.data.repository

import com.example.shopiku.data.model.CartItem
import com.example.shopiku.data.remote.RetrofitClient
import retrofit2.Response

class CartRepository {
    private val api = RetrofitClient.apiService

    suspend fun getCartItems(): Response<List<CartItem>> = api.getCartItems()

    suspend fun addToCart(item: CartItem): Response<CartItem> = api.addToCart(item)

    suspend fun updateQuantity(cartId: String, item: CartItem): Response<CartItem> =
        api.updateCartQuantity(cartId, item)

    suspend fun deleteCartItem(cartId: String): Response<CartItem> =
        api.deleteCartItem(cartId)
}