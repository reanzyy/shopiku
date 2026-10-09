package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Locale

@Serializable
data class ProductVariant(
    @SerialName("id")
    @SerializedName("id")
    val id: Long,

    @SerialName("product_id")
    @SerializedName("product_id", alternate = ["productId"])
    val productId: Long,

    @SerialName("sku")
    @SerializedName("sku")
    val sku: String? = null,

    @SerialName("color")
    @SerializedName("color")
    val color: String? = null,

    @SerialName("size")
    @SerializedName("size")
    val size: String? = null,

    @SerialName("additional_price")
    @SerializedName("additional_price", alternate = ["additionalPrice"])
    val additionalPrice: Double = 0.0,

    @SerialName("stock")
    @SerializedName("stock")
    val stock: Int = 0,

    @SerialName("image_url")
    @SerializedName("image_url", alternate = ["imageUrl"])
    val imageUrl: String? = null,

    @SerialName("is_active")
    @SerializedName("is_active", alternate = ["isActive"])
    val isActive: Boolean = true
) {
    fun getFormattedAdditionalPrice(): String {
        return if (additionalPrice > 0) {
            val localeID = Locale("in", "ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            "+${numberFormat.format(additionalPrice)}"
        } else {
            ""
        }
    }

    fun getDisplayName(): String {
        val parts = mutableListOf<String>()
        if (!color.isNullOrBlank()) parts.add("Warna: $color")
        if (!size.isNullOrBlank()) parts.add("Ukuran: $size")
        return if (parts.isEmpty()) "Standar" else parts.joinToString(", ")
    }
}
