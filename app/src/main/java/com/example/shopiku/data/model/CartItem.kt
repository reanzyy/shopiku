package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName

data class CartItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("productId") val productId: String,
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Double,
    @SerializedName("quantity") var quantity: Int,
    @SerializedName("imageUrl") val imageUrl: String
)