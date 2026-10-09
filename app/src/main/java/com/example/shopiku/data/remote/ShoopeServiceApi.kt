package com.example.shopiku.data.remote

import com.example.shopiku.data.model.CartItem
import com.example.shopiku.data.model.Product
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ShopeeApiService {
    @GET("products")
    suspend fun getProducts(
        @Query("search") search: String? = null,
        @Query("name") searchName: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<Product>>

    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: String): Response<Product>

    @GET("cart")
    suspend fun getCartItems(): Response<List<CartItem>>

    @POST("cart")
    suspend fun addToCart(@Body item: CartItem): Response<CartItem>

    @PUT("cart/{id}")
    suspend fun updateCartQuantity(
        @Path("id") cartId: String,
        @Body item: CartItem
    ): Response<CartItem>

    @DELETE("cart/{id}")
    suspend fun deleteCartItem(@Path("id") cartId: String): Response<CartItem>
}
