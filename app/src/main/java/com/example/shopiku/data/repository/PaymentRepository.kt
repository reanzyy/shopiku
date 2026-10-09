package com.example.shopiku.data.repository

import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.CheckoutResponse
import com.example.shopiku.data.model.PaymentResponse
import com.example.shopiku.data.model.PaymentStatus
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
     * Status disimpan di [PaymentSessionStore] agar tetap tersedia lintas Activity.
     */
    fun createPaymentTransaction(
        orderId: String,
        orderNumber: String,
        amount: Double,
        bank: String = "BCA"
    ): Flow<UiState<PaymentResponse>> = flow {
        emit(UiState.Loading)
        try {
            val existing = PaymentSessionStore.get(orderId)
            if (existing != null && PaymentStatus.isSuccess(existing.status)) {
                emit(UiState.Success(existing.toPaymentResponse()))
                return@flow
            }

            val bankPrefix = when (bank.uppercase()) {
                "BCA" -> "8277"
                "MANDIRI" -> "8800"
                "BRI" -> "8888"
                "BNI" -> "8808"
                else -> "8277"
            }
            val digits = orderId.replace("-", "").takeLast(10).padEnd(10, '0')
            val vaNumber = "$bankPrefix $digits"
            val normalizedBank = bank.uppercase()

            val session = PaymentSessionStore.Session(
                paymentId = "PAY-${System.currentTimeMillis()}",
                orderId = orderId,
                orderNumber = orderNumber,
                amount = amount,
                bank = normalizedBank,
                paymentMethod = "$normalizedBank Virtual Account",
                virtualAccountNumber = vaNumber,
                expiresAt = "24 Jam dari sekarang",
                status = PaymentStatus.PENDING
            )
            PaymentSessionStore.save(session)

            emit(UiState.Success(session.toPaymentResponse()))
        } catch (e: Exception) {
            emit(UiState.Error("Gagal membuat transaksi pembayaran: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Mengambil status transaksi: prioritas lokal (sandbox), lalu remote bila perlu.
     */
    fun getOrderPaymentStatus(orderId: String): Flow<UiState<String>> = flow {
        emit(UiState.Loading)
        try {
            val localStatus = PaymentSessionStore.getStatus(orderId)
            if (localStatus != null) {
                emit(UiState.Success(localStatus))
                return@flow
            }

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
                val normalized = PaymentStatus.normalize(order.status)
                PaymentSessionStore.updateStatus(orderId, normalized)
                    ?: PaymentSessionStore.save(
                        PaymentSessionStore.Session(
                            paymentId = "PAY-REMOTE",
                            orderId = orderId,
                            orderNumber = orderId,
                            amount = 0.0,
                            bank = "BCA",
                            paymentMethod = "Virtual Account",
                            virtualAccountNumber = "-",
                            expiresAt = null,
                            status = normalized
                        )
                    )
                emit(UiState.Success(normalized))
            } else {
                emit(UiState.Error("Pesanan tidak ditemukan."))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Gagal memeriksa status: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Memperbarui status simulasi lokal. Sinkronisasi remote bersifat best-effort.
     */
    fun updateOrderStatus(orderId: String, newStatus: String): Flow<UiState<String>> = flow {
        emit(UiState.Loading)
        try {
            val normalized = PaymentStatus.normalize(newStatus)
            val updated = PaymentSessionStore.updateStatus(orderId, normalized)
            if (updated == null) {
                emit(UiState.Error("Sesi pembayaran tidak ditemukan. Muat ulang halaman Payment."))
                return@flow
            }

            // Best-effort sync ke backend (simulasi tidak bergantung pada hasil remote)
            try {
                val payload = mapOf("status" to normalized.lowercase())
                val response = paymentApi.updateOrderStatus("eq.$orderId", payload)
                if (!response.isSuccessful) {
                    SupabaseClient.client.from("orders").update({
                        set("status", normalized.lowercase())
                    }) {
                        filter { eq("id", orderId) }
                    }
                }
            } catch (_: Exception) {
                // Abaikan kegagalan remote — status lokal tetap berlaku untuk sandbox
            }

            emit(UiState.Success(normalized))
        } catch (e: Exception) {
            emit(UiState.Error("Terjadi kesalahan: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Menginisialisasi ulang sesi pembayaran (mis. setelah EXPIRED → Coba Bayar Lagi).
     */
    fun resetPaymentSession(
        orderId: String,
        orderNumber: String,
        amount: Double,
        bank: String = "BCA"
    ): Flow<UiState<PaymentResponse>> {
        PaymentSessionStore.clear(orderId)
        return createPaymentTransaction(orderId, orderNumber, amount, bank)
    }

    fun getLocalSession(orderId: String): PaymentSessionStore.Session? =
        PaymentSessionStore.get(orderId)
}
