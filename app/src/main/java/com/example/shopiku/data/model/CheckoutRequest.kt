package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CheckoutRequest(
    @SerialName("id")
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerialName("order_number")
    @SerializedName("order_number", alternate = ["orderNumber"])
    val orderNumber: String = "ORD-${System.currentTimeMillis()}",

    @SerialName("total_amount")
    @SerializedName("total_amount", alternate = ["totalAmount"])
    val totalAmount: Double,

    @SerialName("status")
    @SerializedName("status")
    val status: String = "pending",

    @SerialName("shipping_address")
    @SerializedName("shipping_address", alternate = ["shippingAddress"])
    val shippingAddress: String,

    @SerialName("payment_method")
    @SerializedName("payment_method", alternate = ["paymentMethod"])
    val paymentMethod: String
)
