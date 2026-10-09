package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import java.text.NumberFormat
import java.util.Locale

data class Product(
    @SerializedName("id") val id: String? = "",
    @SerializedName("name") val name: String? = "",
    @SerializedName("price") val price: Double? = 0.0,
    @SerializedName("rating") val rating: Double? = 0.0,
    @SerializedName("description") val description: String? = "",
    @SerializedName("imageUrl") val imageUrl: String? = "",
    @SerializedName("category") val category: String? = ""
) {
    fun getFormattedPrice(): String {
        val safePrice = price ?: 0.0
        return try {
            val localeID = Locale("in", "ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            numberFormat.format(safePrice)
        } catch (e: Exception) {
            "Rp${safePrice.toInt()}"
        }
    }
}
