package com.example.shopiku.data.remote

import com.example.shopiku.data.model.CheckoutResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.Query

interface PaymentApiService {

    @GET("orders?select=*")
    suspend fun getOrderById(
        @Query("id") idFilter: String
    ): Response<List<CheckoutResponse>>

    @Headers("Prefer: return=representation")
    @PATCH("orders")
    suspend fun updateOrderStatus(
        @Query("id") idFilter: String,
        @Body statusPayload: Map<String, String>
    ): Response<List<CheckoutResponse>>
}
