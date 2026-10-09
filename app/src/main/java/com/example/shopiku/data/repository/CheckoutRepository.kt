package com.example.shopiku.data.repository

import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.ApiErrorResponse
import com.example.shopiku.data.model.CheckoutRequest
import com.example.shopiku.data.model.CheckoutResponse
import com.example.shopiku.data.remote.CartApiService
import com.example.shopiku.data.remote.CheckoutApiService
import com.example.shopiku.data.remote.RetrofitClient
import com.example.shopiku.data.remote.SupabaseClient
import com.google.gson.Gson
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response

class CheckoutRepository(
    private val checkoutApi: CheckoutApiService = RetrofitClient.checkoutApiService,
    private val cartApi: CartApiService = RetrofitClient.cartApiService
) {
    fun createOrder(request: CheckoutRequest): Flow<UiState<CheckoutResponse>> = flow {
        emit(UiState.Loading)
        try {
            var isSuccess = false
            var createdOrder: CheckoutResponse? = null
            var errorMsg: String? = null

            val response = checkoutApi.createOrder(request)
            if (response.isSuccessful) {
                isSuccess = true
                createdOrder = response.body()?.firstOrNull() ?: CheckoutResponse(
                    id = request.id,
                    orderNumber = request.orderNumber,
                    totalAmount = request.totalAmount,
                    status = request.status,
                    shippingAddress = request.shippingAddress,
                    paymentMethod = request.paymentMethod
                )
            } else {
                errorMsg = parseHttpError(response)
            }

            if (isSuccess && createdOrder != null) {
                // Clear cart on successful order creation
                try {
                    SupabaseClient.client.from("cart").delete { filter { true } }
                } catch (e: Exception) {
                    // Ignore cart cleanup error
                }
                emit(UiState.Success(createdOrder))
            } else {
                // Fallback ke Supabase SDK Native
                try {
                    SupabaseClient.client.from("orders").insert(request)
                    val fallbackOrder = CheckoutResponse(
                        id = request.id,
                        orderNumber = request.orderNumber,
                        totalAmount = request.totalAmount,
                        status = request.status,
                        shippingAddress = request.shippingAddress,
                        paymentMethod = request.paymentMethod
                    )
                    try {
                        SupabaseClient.client.from("cart").delete { filter { true } }
                    } catch (e: Exception) {
                        // Ignore
                    }
                    emit(UiState.Success(fallbackOrder))
                } catch (sdkEx: Exception) {
                    emit(UiState.Error(errorMsg ?: "Gagal membuat pesanan: ${sdkEx.localizedMessage}"))
                }
            }
        } catch (e: java.net.UnknownHostException) {
            emit(UiState.Error("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            try {
                SupabaseClient.client.from("orders").insert(request)
                val fallbackOrder = CheckoutResponse(
                    id = request.id,
                    orderNumber = request.orderNumber,
                    totalAmount = request.totalAmount,
                    status = request.status,
                    shippingAddress = request.shippingAddress,
                    paymentMethod = request.paymentMethod
                )
                emit(UiState.Success(fallbackOrder))
            } catch (sdkEx: Exception) {
                emit(UiState.Error("Gagal terhubung ke server: ${sdkEx.localizedMessage ?: e.localizedMessage}"))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun <T> parseHttpError(response: Response<T>): String {
        val code = response.code()
        val errorBodyString = response.errorBody()?.string()

        val parsedMessage = try {
            if (!errorBodyString.isNullOrBlank()) {
                val errorObj = Gson().fromJson(errorBodyString, ApiErrorResponse::class.java)
                errorObj.message ?: errorObj.details
            } else null
        } catch (e: Exception) {
            null
        }

        return when (code) {
            400 -> parsedMessage ?: "Permintaan checkout tidak valid (400)."
            401, 403 -> parsedMessage ?: "Akses ditolak (401/403). Periksa izin database Supabase."
            404 -> parsedMessage ?: "Tabel atau endpoint 'orders' tidak ditemukan."
            422 -> parsedMessage ?: "Validasi data checkout gagal (422)."
            500 -> parsedMessage ?: "Terjadi kesalahan pada server Supabase (500)."
            else -> parsedMessage ?: "Gagal membuat pesanan (HTTP $code)."
        }
    }
}
