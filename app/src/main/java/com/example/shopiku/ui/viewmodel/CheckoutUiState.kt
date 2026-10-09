package com.example.shopiku.ui.viewmodel

import com.example.shopiku.data.model.Address
import com.example.shopiku.data.model.CheckoutItem
import com.example.shopiku.data.model.CheckoutResponse
import com.example.shopiku.data.model.CheckoutSummary
import com.example.shopiku.data.model.ShippingMethod

data class CheckoutUiState(
    val isLoading: Boolean = false,
    val items: List<CheckoutItem> = emptyList(),
    val address: Address = Address(
        recipientName = "Adriansyah Suryawan",
        phone = "0812-3456-7890",
        fullAddress = "Jl. Pemuda No. 42, RT 03 / RW 07, Kel. Sunyaragi, Kec. Kesambi",
        city = "Kota Cirebon",
        province = "Jawa Barat",
        postalCode = "45132"
    ),
    val selectedShippingMethod: ShippingMethod = ShippingMethod(
        id = "jnt_reg",
        name = "J&T Express Regular",
        courier = "J&T Express",
        price = 15000.0,
        estimatedDays = "2 - 3 Hari (Tiba 11 - 12 Okt)"
    ),
    val selectedPaymentMethodName: String = "BCA Virtual Account",
    val promoCode: String? = "HEMAT20K",
    val discountVoucher: Double = 20000.0,
    val isCreatingOrder: Boolean = false,
    val orderSuccessMessage: String? = null,
    val createdOrder: CheckoutResponse? = null,
    val errorMessage: String? = null
) {
    fun getSummary(): CheckoutSummary {
        val subtotal = items.sumOf { it.subtotal }
        return CheckoutSummary(
            subtotalProduct = subtotal,
            shippingCost = selectedShippingMethod.price,
            discountShipping = if (subtotal > 0.0) 15000.0 else 0.0,
            discountVoucher = discountVoucher,
            serviceFee = 1000.0
        )
    }
}
