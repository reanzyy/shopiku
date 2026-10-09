package com.example.shopiku.ui.viewmodel

import com.example.shopiku.data.model.ProductDetail
import com.example.shopiku.data.model.Review

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val productDetail: ProductDetail? = null,
    val reviews: List<Review> = emptyList(),
    val isReviewsLoading: Boolean = false,
    val selectedQuantity: Int = 1,
    val errorMessage: String? = null,
    val isAddingToCart: Boolean = false,
    val successMessage: String? = null,
    val addToCartErrorMessage: String? = null,
    val cartItemCount: Int = 0
) {
    // Compatibility getter for quantity
    val quantity: Int get() = selectedQuantity
    val isAddToCartLoading: Boolean get() = isAddingToCart
    val addToCartSuccessMessage: String? get() = successMessage
}
