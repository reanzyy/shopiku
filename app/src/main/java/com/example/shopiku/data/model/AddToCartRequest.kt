package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AddToCartRequest(
    @SerialName("id")
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerialName("product_id")
    @SerializedName("product_id", alternate = ["productId"])
    val product_id: Long,

    @SerialName("variant_id")
    @SerializedName("variant_id", alternate = ["variantId"])
    val variant_id: Long? = null,

    @SerialName("quantity")
    @SerializedName("quantity")
    val quantity: Int
)
