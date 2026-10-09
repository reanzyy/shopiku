package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CartResponse(
    @SerialName("id")
    @SerializedName("id")
    val id: String? = null,

    @SerialName("product_id")
    @SerializedName("product_id", alternate = ["productId"])
    val product_id: Long? = null,

    @SerialName("quantity")
    @SerializedName("quantity")
    val quantity: Int? = null,

    @SerialName("created_at")
    @SerializedName("created_at", alternate = ["createdAt"])
    val created_at: String? = null,

    @SerialName("updated_at")
    @SerializedName("updated_at", alternate = ["updatedAt"])
    val updated_at: String? = null
)
