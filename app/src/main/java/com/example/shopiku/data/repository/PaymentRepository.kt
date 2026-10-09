package com.example.shopiku.data.repository

import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.CheckoutResponse
import com.example.shopiku.data.model.PaymentResponse
import com.example.shopiku.data.remote.PaymentApiService
import com.example.shopiku.data.remote.RetrofitClient
import com.example.shopiku.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class PaymentRepository(
    private val paymentApi: PaymentApiService = RetrofitClient.paymentApiService
) {
    /**
     * Membuat detail transaksi pembayaran sandbox beserta Virtual Account number.
     */
    fun createPaymentTransaction(
        orderId: String,
        orderNumber: String,
        amount: Double,
        bank: String = "BCA"
    ): Flow<UiState<PaymentResponse>> = flow {
        emit(UiState.Loading)
        try {
            // Generasi Virtual Account number resmi sandbox
            val bankPrefix = when (bank.uppercase()) {
                "BCA" -> "8277"
                "MANDIRI" -> "8800"
                "BRI" -> "8888"
                "BNI" -> "8808"
                else -> "8277"
            }
            // Generate 12-digit suffix from orderId/timestamp
            val digits = orderId.replace("-", "").takeLast(10).padEnd(10, '0')
            val vaNumber = "$bankPrefix $digits"

            val paymentResp = PaymentResponse(
                paymentId = "PAY-${System.currentTimeMillis()}",
                orderId = orderId,
                orderNumber = orderNumber,
                status = "pending",
                paymentMethod = "Virtual Account",
                bank = bank.uppercase(),
                virtualAccountNumber = vaNumber,
                amount = amount,
                expiresAt = "24 Jam dari sekarang"
            )

            emit(UiState.Success(paymentResp))
        } catch (e: Exception) {
            emit(UiState.Error("Gagal membuat transaksi pembayaran: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Mengambil status transaksi pesanan langsung dari Supabase.
     */
    fun getOrderPaymentStatus(orderId: String): Flow<UiState<String>> = flow {
        emit(UiState.Loading)
        try {
            val response = paymentApi.getOrderById("eq.$orderId")
            val order = if (response.isSuccessful) {
                response.body()?.firstOrNull()
            } else {
                try {
                    SupabaseClient.client.from("orders").select {
                        filter { eq("id", orderId) }
                    }.decodeSingleOrNull<CheckoutResponse>()
                } catch (e: Exception) {
                    null
                }
            }

            if (order != null) {
                emit(UiState.Success(order.status))
            } else {
                emit(UiState.Error("Pesanan tidak ditemukan."))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Gagal memeriksa status: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Memperbarui status transaksi pesanan di Supabase (Simulasi Sandbox / Callback).
     */
    fun updateOrderStatus(orderId: String, newStatus: String): Flow<UiState<String>> = flow {
        emit(UiState.Loading)
        try {
            val payload = mapOf("status" to newStatus)
            val response = paymentApi.updateOrderStatus("eq.$orderId", payload)
            if (response.isSuccessful) {
                emit(UiState.Success(newStatus))
            } else {
                try {
                    SupabaseClient.client.from("orders").update({
                        set("status", newStatus)
                    }) {
                        filter { eq("id", orderId) }
                    }
                    emit(UiState.Success(newStatus))
                } catch (e: Exception) {
                    emit(UiState.Error("Gagal memperbarui status di Supabase."))
                }
            }
        } catch (e: Exception) {
            emit(UiState.Error("Terjadi kesalahan koneksi: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)
}
