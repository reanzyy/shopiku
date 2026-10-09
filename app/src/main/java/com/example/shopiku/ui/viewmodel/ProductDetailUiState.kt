package com.example.shopiku.ui.viewmodel

import com.example.shopiku.data.model.ProductDetail
import com.example.shopiku.data.model.ProductVariant
import com.example.shopiku.data.model.Review
import java.text.NumberFormat
import java.util.Locale

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val productDetail: ProductDetail? = null,
    val reviews: List<Review> = emptyList(),
    val isReviewsLoading: Boolean = false,

    // Variant State
    val variants: List<ProductVariant> = emptyList(),
    val isVariantsLoading: Boolean = false,
    val availableColors: List<String> = emptyList(),
    val availableSizes: List<String> = emptyList(),
    val selectedColor: String? = null,
    val selectedSize: String? = null,
    val selectedVariant: ProductVariant? = null,
    val finalPrice: Double = 0.0,
    val stock: Int = 0,
    val isVariantSelectionValid: Boolean = true,
    val variantSelectionInstruction: String? = null,

    val selectedQuantity: Int = 1,
    val errorMessage: String? = null,
    val isAddingToCart: Boolean = false,
    val successMessage: String? = null,
    val addToCartErrorMessage: String? = null,
    val cartItemCount: Int = 0
) {
    val quantity: Int get() = selectedQuantity
    val isAddToCartLoading: Boolean get() = isAddingToCart
    val addToCartSuccessMessage: String? get() = successMessage

    fun getFormattedFinalPrice(): String {
        return try {
            val localeID = Locale("in", "ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            numberFormat.format(finalPrice)
        } catch (e: Exception) {
            "Rp${finalPrice.toLong()}"
        }
    }
}
