package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Locale

@Serializable
data class CheckoutSummary(
    @SerialName("subtotalProduct")
    @SerializedName("subtotalProduct")
    val subtotalProduct: Double = 0.0,

    @SerialName("shippingCost")
    @SerializedName("shippingCost")
    val shippingCost: Double = 15000.0,

    @SerialName("discountShipping")
    @SerializedName("discountShipping")
    val discountShipping: Double = 15000.0,

    @SerialName("discountVoucher")
    @SerializedName("discountVoucher")
    val discountVoucher: Double = 0.0,

    @SerialName("serviceFee")
    @SerializedName("serviceFee")
    val serviceFee: Double = 1000.0
) {
    fun getGrandTotal(): Double {
        val total = (subtotalProduct + shippingCost + serviceFee) - (discountShipping + discountVoucher)
        return if (total < 0.0) 0.0 else total
    }

    fun formatRupiah(amount: Double): String {
        return try {
            val localeID = Locale("in", "ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            numberFormat.format(amount)
        } catch (e: Exception) {
            "Rp${amount.toLong()}"
        }
    }
}
