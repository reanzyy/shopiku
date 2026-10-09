package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponse(
    @SerialName("payment_id")
    @SerializedName("payment_id", alternate = ["paymentId"])
    val paymentId: String,

    @SerialName("order_id")
    @SerializedName("order_id", alternate = ["orderId"])
    val orderId: String,

    @SerialName("order_number")
    @SerializedName("order_number", alternate = ["orderNumber"])
    val orderNumber: String? = null,

    @SerialName("status")
    @SerializedName("status")
    val status: String = "pending",

    @SerialName("payment_method")
    @SerializedName("payment_method", alternate = ["paymentMethod"])
    val paymentMethod: String = "bank_transfer",

    @SerialName("bank")
    @SerializedName("bank")
    val bank: String = "bca",

    @SerialName("virtual_account_number")
    @SerializedName("virtual_account_number", alternate = ["virtualAccountNumber"])
    val virtualAccountNumber: String,

    @SerialName("amount")
    @SerializedName("amount")
    val amount: Double,

    @SerialName("expires_at")
    @SerializedName("expires_at", alternate = ["expiresAt"])
    val expiresAt: String? = null
)
