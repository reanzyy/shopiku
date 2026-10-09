package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    @SerialName("message")
    @SerializedName("message", alternate = ["msg", "error", "hint"])
    val message: String? = null,

    @SerialName("details")
    @SerializedName("details")
    val details: String? = null,

    @SerialName("code")
    @SerializedName("code")
    val code: String? = null
)
