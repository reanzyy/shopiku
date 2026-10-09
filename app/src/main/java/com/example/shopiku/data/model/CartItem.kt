package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Locale

@Serializable
data class CartItem(
    @SerialName("id")
    @SerializedName("id")
    val id: String? = null,

    @SerialName("productId")
    @SerializedName("productId", alternate = ["product_id"])
    val productId: String,

    @SerialName("variantId")
    @SerializedName("variantId", alternate = ["variant_id"])
    val variantId: Long? = null,

    @SerialName("variantName")
    @SerializedName("variantName", alternate = ["variant_name"])
    val variantName: String? = null,

    @SerialName("name")
    @SerializedName("name")
    val name: String,

    @SerialName("price")
    @SerializedName("price")
    val price: Double,

    @SerialName("quantity")
    @SerializedName("quantity")
    var quantity: Int,

    @SerialName("imageUrl")
    @SerializedName("imageUrl", alternate = ["image_url"])
    val imageUrl: String
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

    fun getTotalPrice(): Double = price * quantity

    fun getFormattedTotalPrice(): String {
        return try {
            val localeID = Locale("in", "ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            numberFormat.format(getTotalPrice())
        } catch (e: Exception) {
            "Rp${getTotalPrice().toLong()}"
        }
    }
}
