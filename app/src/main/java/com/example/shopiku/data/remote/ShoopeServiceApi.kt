package com.example.shopiku.data.remote

import com.example.shopiku.data.model.CartItem
import com.example.shopiku.data.model.Product
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ShopeeApiService {
    @GET("products?select=*")
    suspend fun getProducts(): Response<List<Product>>

    @GET("products?select=*")
    suspend fun getProductById(@Query("id") idFilter: String): Response<List<Product>>

    @GET("cart?select=*")
    suspend fun getCartItems(): Response<List<CartItem>>

    @POST("cart")
    suspend fun addToCart(@Body item: CartItem): Response<CartItem>

    @PUT("cart")
    suspend fun updateCartQuantity(
        @Query("id") idFilter: String,
        @Body item: CartItem
    ): Response<CartItem>

    @DELETE("cart")
    suspend fun deleteCartItem(@Query("id") idFilter: String): Response<CartItem>
}
