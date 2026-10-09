package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Locale

@Serializable
data class ProductDetail(
    @SerialName("id")
    @SerializedName("id")
    val id: Long,

    @SerialName("name")
    @SerializedName("name")
    val name: String,

    @SerialName("price")
    @SerializedName("price")
    val price: Double,

    @SerialName("rating")
    @SerializedName("rating")
    val rating: Double? = null,

    @SerialName("description")
    @SerializedName("description")
    val description: String? = null,

    @SerialName("imageUrl")
    @SerializedName("imageUrl", alternate = ["image_url"])
    val imageUrl: String? = null,

    @SerialName("category")
    @SerializedName("category")
    val category: String? = null
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
