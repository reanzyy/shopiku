package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Locale

@Serializable
data class Product(
    @SerialName("id")
    @SerializedName("id")
    val id: String? = "",

    @SerialName("name")
    @SerializedName("name")
    val name: String? = "",

    @SerialName("price")
    @SerializedName("price")
    val price: Double? = 0.0,

    @SerialName("rating")
    @SerializedName("rating")
    val rating: Double? = 0.0,

    @SerialName("description")
    @SerializedName("description")
    val description: String? = "",

    @SerialName("imageUrl")
    @SerializedName("imageUrl", alternate = ["image_url"])
    val imageUrl: String? = "",

    @SerialName("category")
    @SerializedName("category")
    val category: String? = ""
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
