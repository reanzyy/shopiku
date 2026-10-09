package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutResponse(
    @SerialName("id")
    @SerializedName("id")
    val id: String,

    @SerialName("order_number")
    @SerializedName("order_number", alternate = ["orderNumber"])
    val orderNumber: String,

    @SerialName("total_amount")
    @SerializedName("total_amount", alternate = ["totalAmount"])
    val totalAmount: Double,

    @SerialName("status")
    @SerializedName("status")
    val status: String = "pending",

    @SerialName("shipping_address")
    @SerializedName("shipping_address", alternate = ["shippingAddress"])
    val shippingAddress: String? = null,

    @SerialName("payment_method")
    @SerializedName("payment_method", alternate = ["paymentMethod"])
    val paymentMethod: String? = null,

    @SerialName("created_at")
    @SerializedName("created_at", alternate = ["createdAt"])
    val createdAt: String? = null
)
