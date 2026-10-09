package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Locale

@Serializable
data class ShippingMethod(
    @SerialName("id")
    @SerializedName("id")
    val id: String,

    @SerialName("name")
    @SerializedName("name")
    val name: String,

    @SerialName("courier")
    @SerializedName("courier")
    val courier: String,

    @SerialName("price")
    @SerializedName("price")
    val price: Double,

    @SerialName("estimatedDays")
    @SerializedName("estimatedDays")
    val estimatedDays: String
) {
    fun getFormattedPrice(): String {
        return try {
            val localeID = Locale("in", "ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            numberFormat.format(price)
        } catch (e: Exception) {
            "Rp${price.toLong()}"
        }
    }
}
