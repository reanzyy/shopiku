package com.example.shopiku.data.remote

import com.example.shopiku.data.model.CheckoutItem
import com.example.shopiku.data.model.CheckoutRequest
import com.example.shopiku.data.model.CheckoutResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface CheckoutApiService {

    @Headers("Prefer: return=representation")
    @POST("orders")
    suspend fun createOrder(
        @Body request: CheckoutRequest
    ): Response<List<CheckoutResponse>>

    @Headers("Prefer: return=representation")
    @POST("order_items")
    suspend fun createOrderItems(
        @Body items: List<CheckoutItem>
    ): Response<List<CheckoutItem>>
}
