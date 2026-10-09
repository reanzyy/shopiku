package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentRequest(
    @SerialName("order_id")
    @SerializedName("order_id", alternate = ["orderId"])
    val orderId: String,

    @SerialName("payment_method")
    @SerializedName("payment_method", alternate = ["paymentMethod"])
    val paymentMethod: String = "bank_transfer",

    @SerialName("bank")
    @SerializedName("bank")
    val bank: String = "bca"
)
