package com.example.shopiku.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.repository.CartRepository
import com.example.shopiku.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val productRepository: ProductRepository = ProductRepository(),
    private val cartRepository: CartRepository = CartRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private var currentProductId: Long? = null

    init {
        fetchCartCount()
    }

    fun loadProductDetail(productId: Long) {
        if (productId <= 0L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "ID produk tidak valid."
                )
            }
            return
        }

        currentProductId = productId
        fetchDetailAndReviews(productId)
    }

    fun fetchCartCount() {
        viewModelScope.launch {
            cartRepository.getCartTotalCount().collect { countState ->
                if (countState is UiState.Success) {
                    _uiState.update { it.copy(cartItemCount = countState.data) }
                }
            }
        }
    }

    private fun fetchDetailAndReviews(productId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            productRepository.getProductDetailById(productId).collect { detailState ->
                when (detailState) {
                    is UiState.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is UiState.Success -> {
                        val product = detailState.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                productDetail = product,
                                errorMessage = null
                            )
                        }
                        fetchReviews(productId)
                    }
                    is UiState.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = detailState.message
                            )
                        }
                    }
                    is UiState.Empty -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Detail produk tidak ditemukan."
                            )
                        }
                    }
                }
            }
        }
    }

    private fun fetchReviews(productId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReviewsLoading = true) }

            productRepository.getProductReviews(productId).collect { reviewsState ->
                when (reviewsState) {
                    is UiState.Success -> {
                        _uiState.update {
                            it.copy(
                                isReviewsLoading = false,
                                reviews = reviewsState.data
                            )
                        }
                    }
                    else -> {
                        _uiState.update {
                            it.copy(
                                isReviewsLoading = false,
                                reviews = emptyList()
                            )
                        }
                    }
                }
            }
        }
    }

    fun increaseQuantity() {
        _uiState.update { currentState ->
            currentState.copy(selectedQuantity = currentState.selectedQuantity + 1)
        }
    }

    fun decreaseQuantity() {
        _uiState.update { currentState ->
            if (currentState.selectedQuantity > 1) {
                currentState.copy(selectedQuantity = currentState.selectedQuantity - 1)
            } else {
                currentState
            }
        }
    }

    fun addToCart() {
        val currentState = _uiState.value

        // 1. Cegah request ganda jika request sebelumnya sedang berlangsung
        if (currentState.isAddingToCart) return

        // 2. Validasi produk sudah dimuat
        val product = currentState.productDetail
        if (product == null) {
            _uiState.update {
                it.copy(addToCartErrorMessage = "Produk belum berhasil dimuat dari server.")
            }
            return
        }

        // 3. Validasi productId
        if (product.id <= 0L) {
            _uiState.update {
                it.copy(addToCartErrorMessage = "ID Produk tidak valid.")
            }
            return
        }

        // 4. Validasi kuantitas minimal 1
        val qty = currentState.selectedQuantity
        if (qty < 1) {
            _uiState.update {
                it.copy(addToCartErrorMessage = "Jumlah produk minimal 1.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAddingToCart = true,
                    successMessage = null,
                    addToCartErrorMessage = null
                )
            }

            cartRepository.addToCartWithResult(productId = product.id, quantity = qty).collect { result ->
                when (result) {
                    is UiState.Loading -> {
                        _uiState.update { it.copy(isAddingToCart = true) }
                    }
                    is UiState.Success -> {
                        _uiState.update {
                            it.copy(
                                isAddingToCart = false,
                                successMessage = "${product.name} ($qty item) berhasil ditambahkan ke keranjang!"
                            )
                        }
                        // Perbarui badge jumlah item keranjang dari server
                        fetchCartCount()
                    }
                    is UiState.Error -> {
                        _uiState.update {
                            it.copy(
                                isAddingToCart = false,
                                addToCartErrorMessage = result.message
                            )
                        }
                    }
                    else -> {
                        _uiState.update {
                            it.copy(isAddingToCart = false)
                        }
                    }
                }
            }
        }
    }

    fun clearAddToCartMessages() {
        _uiState.update {
            it.copy(
                successMessage = null,
                addToCartErrorMessage = null
            )
        }
    }

    fun retry() {
        currentProductId?.let { loadProductDetail(it) }
    }
}
