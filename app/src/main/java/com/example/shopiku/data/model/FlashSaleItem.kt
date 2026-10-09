package com.example.shopiku.data.model

import java.text.NumberFormat
import java.util.Locale

data class FlashSaleItem(
    val id: String,
    val name: String,
    val flashSalePrice: Double,
    val originalPrice: Double,
    val discountPercent: Int,
    val stockPercentage: Int,
    val stockText: String,
    val imageUrl: String
) {
    fun getFormattedFlashSalePrice(): String {
        return try {
            val localeID = Locale("in", "ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            numberFormat.format(flashSalePrice)
        } catch (e: Exception) {
            "Rp${flashSalePrice.toLong()}"
        }
    }

    fun getFormattedOriginalPrice(): String {
        return try {
            val localeID = Locale("in", "ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            numberFormat.format(originalPrice)
        } catch (e: Exception) {
            "Rp${originalPrice.toLong()}"
        }
    }

    companion object {
        fun getDummyFlashSaleList(): List<FlashSaleItem> {
            return listOf(
                FlashSaleItem(
                    id = "1",
                    name = "TWS Wireless Bluetooth Earbuds Pro ANC",
                    flashSalePrice = 149000.0,
                    originalPrice = 399000.0,
                    discountPercent = 63,
                    stockPercentage = 80,
                    stockText = "Sisa 20%",
                    imageUrl = "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=500&auto=format&fit=crop&q=60"
                ),
                FlashSaleItem(
                    id = "2",
                    name = "Smartwatch AMOLED Waterproof Sport",
                    flashSalePrice = 199000.0,
                    originalPrice = 499000.0,
                    discountPercent = 60,
                    stockPercentage = 90,
                    stockText = "Sisa 10%",
                    imageUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&auto=format&fit=crop&q=60"
                ),
                FlashSaleItem(
                    id = "3",
                    name = "Powerbank 20000mAh Fast Charging 22.5W",
                    flashSalePrice = 119000.0,
                    originalPrice = 249000.0,
                    discountPercent = 52,
                    stockPercentage = 65,
                    stockText = "Sisa 35%",
                    imageUrl = "https://images.unsplash.com/photo-1609592424367-17eb481df0bb?w=500&auto=format&fit=crop&q=60"
                ),
                FlashSaleItem(
                    id = "4",
                    name = "Keyboard Mechanical RGB Blue Switch",
                    flashSalePrice = 269000.0,
                    originalPrice = 550000.0,
                    discountPercent = 51,
                    stockPercentage = 75,
                    stockText = "Sisa 25%",
                    imageUrl = "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500&auto=format&fit=crop&q=60"
                ),
                FlashSaleItem(
                    id = "5",
                    name = "Sneakers Pria Sport Running Breathable",
                    flashSalePrice = 159000.0,
                    originalPrice = 350000.0,
                    discountPercent = 55,
                    stockPercentage = 85,
                    stockText = "Sisa 15%",
                    imageUrl = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500&auto=format&fit=crop&q=60"
                ),
                FlashSaleItem(
                    id = "6",
                    name = "Tas Ransel Laptop Waterproof USB Port",
                    flashSalePrice = 89000.0,
                    originalPrice = 199000.0,
                    discountPercent = 55,
                    stockPercentage = 70,
                    stockText = "Sisa 30%",
                    imageUrl = "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500&auto=format&fit=crop&q=60"
                )
            )
        }
    }
}
