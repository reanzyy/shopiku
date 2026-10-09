package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Locale

@Serializable
data class CheckoutItem(
    @SerialName("cart_item_id")
    @SerializedName("cart_item_id", alternate = ["cartItemId"])
    val cartItemId: String? = null,

    @SerialName("product_id")
    @SerializedName("product_id", alternate = ["productId"])
    val productId: Long,

    @SerialName("product_name")
    @SerializedName("product_name", alternate = ["productName"])
    val productName: String,

    @SerialName("price")
    @SerializedName("price")
    val price: Double,

    @SerialName("quantity")
    @SerializedName("quantity")
    val quantity: Int,

    @SerialName("subtotal")
    @SerializedName("subtotal")
    val subtotal: Double = price * quantity,

    @SerialName("imageUrl")
    @SerializedName("imageUrl", alternate = ["image_url"])
    val imageUrl: String? = null
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

    fun getFormattedSubtotal(): String {
        return try {
            val localeID = Locale("in", "ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            numberFormat.format(subtotal)
        } catch (e: Exception) {
            "Rp${subtotal.toLong()}"
        }
    }
}
