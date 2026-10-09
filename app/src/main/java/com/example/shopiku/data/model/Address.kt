package com.example.shopiku.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Address(
    @SerialName("recipientName")
    @SerializedName("recipientName")
    val recipientName: String,

    @SerialName("phone")
    @SerializedName("phone")
    val phone: String,

    @SerialName("fullAddress")
    @SerializedName("fullAddress")
    val fullAddress: String,

    @SerialName("city")
    @SerializedName("city")
    val city: String = "Kota Cirebon",

    @SerialName("province")
    @SerializedName("province")
    val province: String = "Jawa Barat",

    @SerialName("postalCode")
    @SerializedName("postalCode")
    val postalCode: String = "45132"
) {
    fun getFormattedAddress(): String {
        return "$fullAddress, $city, $province $postalCode"
    }
}
