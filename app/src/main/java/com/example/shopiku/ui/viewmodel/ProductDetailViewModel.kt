package com.example.shopiku.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.ProductVariant
import com.example.shopiku.data.repository.CartRepository
import com.example.shopiku.data.repository.ProductRepository
import com.example.shopiku.data.repository.ProductVariantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val productRepository: ProductRepository = ProductRepository(),
    private val cartRepository: CartRepository = CartRepository(),
    private val variantRepository: ProductVariantRepository = ProductVariantRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private var currentProductId: Long? = null

    init {
        fetchCartCount()
    }

    fun loadProductDetail(productId: Long) {
        val safeId = if (productId <= 0L) 1L else productId
        currentProductId = safeId
        fetchDetailAndReviews(safeId)
        fetchVariants(safeId)
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

            fetchReviews(productId)

            productRepository.getProductDetailById(productId).collect { detailState ->
                when (detailState) {
                    is UiState.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is UiState.Success -> {
                        val product = detailState.data
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                productDetail = product,
                                finalPrice = product.price,
                                errorMessage = null
                            )
                        }
                        recalculateVariantSelection()
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

    private fun fetchVariants(productId: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isVariantsLoading = true,
                    variantsErrorMessage = null
                )
            }

            variantRepository.getProductVariants(productId).collect { variantState ->
                when (variantState) {
                    is UiState.Loading -> {
                        _uiState.update {
                            it.copy(isVariantsLoading = true, variantsErrorMessage = null)
                        }
                    }
                    is UiState.Success -> {
                        applyVariantList(variantState.data)
                    }
                    is UiState.Empty -> {
                        _uiState.update {
                            it.copy(
                                isVariantsLoading = false,
                                variantsErrorMessage = null,
                                variants = emptyList(),
                                availableColors = emptyList(),
                                availableSizes = emptyList(),
                                selectedColor = null,
                                selectedSize = null,
                                selectedVariant = null,
                                variantSelectionInstruction = null,
                                isVariantSelectionValid = true
                            )
                        }
                        recalculateVariantSelection()
                    }
                    is UiState.Error -> {
                        _uiState.update {
                            it.copy(
                                isVariantsLoading = false,
                                variantsErrorMessage = variantState.message,
                                variants = emptyList(),
                                availableColors = emptyList(),
                                availableSizes = emptyList(),
                                selectedColor = null,
                                selectedSize = null,
                                selectedVariant = null,
                                isVariantSelectionValid = false,
                                variantSelectionInstruction = null
                            )
                        }
                    }
                }
            }
        }
    }

    private fun applyVariantList(variantList: List<ProductVariant>) {
        val active = variantList.filter { it.isActive }
        val colors = active.mapNotNull { it.color?.takeIf { c -> c.isNotBlank() } }.distinct()
        val sizes = active.mapNotNull { it.size?.takeIf { s -> s.isNotBlank() } }.distinct()

        val first = active.firstOrNull()
        val defaultColor = first?.color?.takeIf { it.isNotBlank() } ?: colors.firstOrNull()
        val defaultSize = when {
            defaultColor != null -> {
                active.filter { it.color.equals(defaultColor, ignoreCase = true) }
                    .mapNotNull { it.size?.takeIf { s -> s.isNotBlank() } }
                    .firstOrNull()
            }
            else -> sizes.firstOrNull()
        }

        _uiState.update { currentState ->
            currentState.copy(
                isVariantsLoading = false,
                variantsErrorMessage = null,
                variants = active,
                availableColors = colors,
                availableSizes = sizes,
                selectedColor = defaultColor,
                selectedSize = defaultSize
            )
        }
        recalculateVariantSelection()
    }

    private fun fetchReviews(productId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReviewsLoading = true) }

            productRepository.getProductReviews(productId).collect { reviewsState ->
                when (reviewsState) {
                    is UiState.Success -> {
                        val reviewList = if (reviewsState.data.isEmpty()) {
                            ProductRepository.getDummyReviews(productId)
                        } else {
                            reviewsState.data
                        }
                        _uiState.update {
                            it.copy(
                                isReviewsLoading = false,
                                reviews = reviewList
                            )
                        }
                    }
                    else -> {
                        _uiState.update {
                            it.copy(
                                isReviewsLoading = false,
                                reviews = ProductRepository.getDummyReviews(productId)
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectColor(color: String) {
        val currentState = _uiState.value
        val variantList = currentState.variants
        val sizesForColor = variantList
            .filter { it.color.equals(color, ignoreCase = true) }
            .mapNotNull { it.size?.takeIf { s -> s.isNotBlank() } }
            .distinct()

        val newSize = when {
            sizesForColor.isEmpty() -> null
            currentState.selectedSize != null &&
                sizesForColor.any { it.equals(currentState.selectedSize, ignoreCase = true) } ->
                currentState.selectedSize
            else -> sizesForColor.firstOrNull()
        }

        _uiState.update {
            it.copy(
                selectedColor = color,
                selectedSize = newSize
            )
        }
        recalculateVariantSelection()
    }

    fun selectSize(size: String) {
        val currentState = _uiState.value
        val variantList = currentState.variants
        val colorsForSize = variantList
            .filter { it.size.equals(size, ignoreCase = true) }
            .mapNotNull { it.color?.takeIf { c -> c.isNotBlank() } }
            .distinct()

        val newColor = when {
            colorsForSize.isEmpty() -> currentState.selectedColor
            currentState.selectedColor != null &&
                colorsForSize.any { it.equals(currentState.selectedColor, ignoreCase = true) } ->
                currentState.selectedColor
            else -> colorsForSize.firstOrNull() ?: currentState.selectedColor
        }

        _uiState.update {
            it.copy(
                selectedSize = size,
                selectedColor = newColor
            )
        }
        recalculateVariantSelection()
    }

    private fun recalculateVariantSelection() {
        val currentState = _uiState.value
        val product = currentState.productDetail ?: return
        val basePrice = product.price
        val variantList = currentState.variants

        if (variantList.isEmpty()) {
            _uiState.update {
                it.copy(
                    selectedVariant = null,
                    finalPrice = basePrice,
                    stock = 0,
                    isVariantSelectionValid = currentState.variantsErrorMessage == null,
                    variantSelectionInstruction = null
                )
            }
            return
        }

        val color = currentState.selectedColor
        val size = currentState.selectedSize
        val hasColors = currentState.availableColors.isNotEmpty()
        val hasSizes = currentState.availableSizes.isNotEmpty()

        if (hasColors && color.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    selectedVariant = null,
                    finalPrice = basePrice,
                    stock = 0,
                    isVariantSelectionValid = false,
                    variantSelectionInstruction = "Silakan pilih warna."
                )
            }
            return
        }

        if (hasSizes && size.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    selectedVariant = null,
                    finalPrice = basePrice,
                    stock = 0,
                    isVariantSelectionValid = false,
                    variantSelectionInstruction = "Silakan pilih ukuran."
                )
            }
            return
        }

        // Exact combination match only — no fallback to unrelated variants
        val matchedVariant = variantList.find { v ->
            val colorOk = !hasColors || v.color.equals(color, ignoreCase = true)
            val sizeOk = !hasSizes || v.size.equals(size, ignoreCase = true)
            colorOk && sizeOk
        }

        if (matchedVariant != null) {
            val calcPrice = basePrice + matchedVariant.additionalPrice
            val isStockAvailable = matchedVariant.stock > 0

            _uiState.update {
                it.copy(
                    selectedVariant = matchedVariant,
                    selectedColor = matchedVariant.color?.takeIf { c -> c.isNotBlank() } ?: color,
                    selectedSize = matchedVariant.size?.takeIf { s -> s.isNotBlank() } ?: size,
                    finalPrice = calcPrice,
                    stock = matchedVariant.stock,
                    isVariantSelectionValid = isStockAvailable,
                    variantSelectionInstruction = if (isStockAvailable) null else "Varian ini sedang habis."
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedVariant = null,
                    finalPrice = basePrice,
                    stock = 0,
                    isVariantSelectionValid = false,
                    variantSelectionInstruction = "Kombinasi varian tidak tersedia."
                )
            }
        }
    }

    fun increaseQuantity() {
        val maxStock = _uiState.value.stock
        _uiState.update { currentState ->
            if (maxStock > 0 && currentState.selectedQuantity >= maxStock) {
                currentState.copy(addToCartErrorMessage = "Jumlah pembelian melebihi stok yang tersedia ($maxStock buah).")
            } else {
                currentState.copy(selectedQuantity = currentState.selectedQuantity + 1)
            }
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

        if (currentState.isAddingToCart) return

        val product = currentState.productDetail
        if (product == null) {
            _uiState.update {
                it.copy(addToCartErrorMessage = "Produk belum berhasil dimuat dari server.")
            }
            return
        }

        if (currentState.variantsErrorMessage != null) {
            _uiState.update {
                it.copy(addToCartErrorMessage = "Varian produk gagal dimuat. Silakan coba lagi.")
            }
            return
        }

        if (currentState.variants.isNotEmpty()) {
            if (!currentState.isVariantSelectionValid || currentState.selectedVariant == null) {
                _uiState.update {
                    it.copy(addToCartErrorMessage = currentState.variantSelectionInstruction ?: "Silakan pilih varian warna dan ukuran terlebih dahulu.")
                }
                return
            }

            if (currentState.stock <= 0) {
                _uiState.update {
                    it.copy(addToCartErrorMessage = "Stok varian ini telah habis.")
                }
                return
            }
        }

        val qty = currentState.selectedQuantity
        if (qty < 1) {
            _uiState.update {
                it.copy(addToCartErrorMessage = "Jumlah produk minimal 1.")
            }
            return
        }

        val variantId = currentState.selectedVariant?.id
        val variantName = currentState.selectedVariant?.getDisplayName() ?: "Standar"

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAddingToCart = true,
                    successMessage = null,
                    addToCartErrorMessage = null
                )
            }

            cartRepository.addToCartWithResult(
                productId = product.id,
                variantId = variantId,
                quantity = qty
            ).collect { result ->
                when (result) {
                    is UiState.Loading -> {
                        _uiState.update { it.copy(isAddingToCart = true) }
                    }
                    is UiState.Success -> {
                        _uiState.update {
                            it.copy(
                                isAddingToCart = false,
                                successMessage = "${product.name} [$variantName] ($qty item) berhasil ditambahkan ke keranjang!"
                            )
                        }
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

    fun retryVariants() {
        currentProductId?.let { fetchVariants(it) }
    }
}
