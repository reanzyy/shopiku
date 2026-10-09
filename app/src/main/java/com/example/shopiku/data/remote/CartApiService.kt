package com.example.shopiku.data.remote

import com.example.shopiku.data.model.AddToCartRequest
import com.example.shopiku.data.model.CartResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface CartApiService {

    @GET("cart?select=*")
    suspend fun getCartItems(): Response<List<CartResponse>>

    @Headers("Prefer: return=representation")
    @POST("cart")
    suspend fun addToCart(
        @Body request: AddToCartRequest
    ): Response<List<CartResponse>>

    @GET("cart?select=quantity")
    suspend fun getCartCount(): Response<List<CartResponse>>

    @Headers("Prefer: return=representation")
    @PATCH("cart")
    suspend fun updateCartQuantity(
        @Query("id") idFilter: String,
        @Body request: AddToCartRequest
    ): Response<List<CartResponse>>

    @DELETE("cart")
    suspend fun deleteCartItem(
        @Query("id") idFilter: String
    ): Response<List<CartResponse>>
}
