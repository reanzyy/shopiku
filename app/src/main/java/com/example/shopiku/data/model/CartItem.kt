package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CartItem(
    @SerialName("id")
    @SerializedName("id")
    val id: String? = null,

    @SerialName("productId")
    @SerializedName("productId")
    val productId: String,

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
    @SerializedName("imageUrl")
    val imageUrl: String
)
