package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    @SerialName("id")
    @SerializedName("id")
    val id: String,

    @SerialName("product_id")
    @SerializedName("product_id", alternate = ["productId"])
    val productId: Long,

    @SerialName("rating")
    @SerializedName("rating")
    val rating: Int,

    @SerialName("comment")
    @SerializedName("comment")
    val comment: String? = null,

    @SerialName("created_at")
    @SerializedName("created_at", alternate = ["createdAt"])
    val createdAt: String? = null,

    @SerialName("user_name")
    @SerializedName("user_name", alternate = ["userName", "username"])
    val userName: String? = null
)
