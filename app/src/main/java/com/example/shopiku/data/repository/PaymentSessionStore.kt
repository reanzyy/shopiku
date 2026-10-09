package com.example.shopiku.data.repository

import com.example.shopiku.data.model.PaymentResponse
import com.example.shopiku.data.model.PaymentStatus
import java.util.concurrent.ConcurrentHashMap

/**
 * Sumber status pembayaran lokal bersama antar Activity/ViewModel.
 * Digunakan untuk simulasi sandbox agar status tetap tersedia saat navigasi.
 */
object PaymentSessionStore {

    data class Session(
        val paymentId: String,
        val orderId: String,
        val orderNumber: String,
        val amount: Double,
        val bank: String,
        val paymentMethod: String,
        val virtualAccountNumber: String,
        val expiresAt: String?,
        var status: String
    ) {
        fun toPaymentResponse(): PaymentResponse = PaymentResponse(
            paymentId = paymentId,
            orderId = orderId,
            orderNumber = orderNumber,
            status = status,
            paymentMethod = paymentMethod,
            bank = bank,
            virtualAccountNumber = virtualAccountNumber,
            amount = amount,
            expiresAt = expiresAt
        )
    }

    private val sessions = ConcurrentHashMap<String, Session>()

    fun save(session: Session) {
        sessions[session.orderId] = session.copy(status = PaymentStatus.normalize(session.status))
    }

    fun get(orderId: String): Session? = sessions[orderId]

    fun updateStatus(orderId: String, newStatus: String): Session? {
        val current = sessions[orderId] ?: return null
        val updated = current.copy(status = PaymentStatus.normalize(newStatus))
        sessions[orderId] = updated
        return updated
    }

    fun getStatus(orderId: String): String? = sessions[orderId]?.status

    fun clear(orderId: String) {
        sessions.remove(orderId)
    }
}
