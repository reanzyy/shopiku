package com.example.shopiku.data.remote

import com.example.shopiku.data.model.AddToCartRequest
import com.example.shopiku.data.model.CartItem
import com.example.shopiku.data.model.ProductDetail
import com.example.shopiku.data.model.Review
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface ProductApiService {

    @GET("products?select=*")
    suspend fun getProductDetail(
        @Query("id") idFilter: String
    ): Response<List<ProductDetail>>

    @GET("reviews?select=*")
    suspend fun getProductReviews(
        @Query("product_id") productIdFilter: String
    ): Response<List<Review>>

    @Headers("Prefer: return=representation")
    @POST("cart")
    suspend fun addToCart(
        @Body request: AddToCartRequest
    ): Response<List<CartItem>>
}
