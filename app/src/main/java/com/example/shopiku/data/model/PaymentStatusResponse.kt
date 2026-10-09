package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentStatusResponse(
    @SerialName("payment_id")
    @SerializedName("payment_id", alternate = ["paymentId"])
    val paymentId: String,

    @SerialName("order_id")
    @SerializedName("order_id", alternate = ["orderId"])
    val orderId: String,

    @SerialName("status")
    @SerializedName("status")
    val status: String,

    @SerialName("updated_at")
    @SerializedName("updated_at", alternate = ["updatedAt"])
    val updatedAt: String? = null
)
