package com.example.shopiku.data.model

/**
 * Status pembayaran lokal (sandbox). Bukan konfirmasi gateway sungguhan.
 */
object PaymentStatus {
    const val PENDING = "PENDING"
    const val SUCCESS = "SUCCESS"
    const val EXPIRED = "EXPIRED"
    const val CANCELLED = "CANCELLED"

    fun normalize(raw: String?): String {
        return when (raw?.trim()?.lowercase()) {
            "paid", "success", "lunas", "settlement" -> SUCCESS
            "pending", "waiting", "unpaid" -> PENDING
            "expired", "expire", "timeout" -> EXPIRED
            "cancelled", "canceled", "cancel" -> CANCELLED
            "failed", "fail" -> CANCELLED
            else -> raw?.trim()?.uppercase() ?: PENDING
        }
    }

    fun isSuccess(status: String?): Boolean = normalize(status) == SUCCESS

    fun isPending(status: String?): Boolean = normalize(status) == PENDING

    fun isExpired(status: String?): Boolean = normalize(status) == EXPIRED

    fun displayMessage(status: String?): String {
        return when (normalize(status)) {
            SUCCESS -> "Status Transaksi Saat Ini\nPembayaran Berhasil (SUCCESS)"
            PENDING -> "Status Transaksi Saat Ini\nMenunggu Pembayaran (PENDING)"
            EXPIRED -> "Status Transaksi Saat Ini\nPembayaran Kedaluwarsa (EXPIRED)"
            CANCELLED -> "Status Transaksi Saat Ini\nPembayaran Dibatalkan (CANCELLED)"
            else -> "Status Transaksi Saat Ini\n${normalize(status)}"
        }
    }
}
