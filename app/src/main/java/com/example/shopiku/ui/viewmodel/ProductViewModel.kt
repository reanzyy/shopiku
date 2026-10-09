package com.example.shopiku.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.Product
import com.example.shopiku.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ProductViewModel(
    private val repository: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _productsState = MutableStateFlow<UiState<List<Product>>>(UiState.Loading)
    val productsState: StateFlow<UiState<List<Product>>> = _productsState.asStateFlow()

    private val _recommendedState = MutableStateFlow<UiState<List<Product>>>(UiState.Loading)
    val recommendedState: StateFlow<UiState<List<Product>>> = _recommendedState.asStateFlow()

    private val _productDetailState = MutableStateFlow<UiState<Product>>(UiState.Loading)
    val productDetailState: StateFlow<UiState<Product>> = _productDetailState.asStateFlow()

    init {
        observeSearchQuery()
        fetchRecommendedProducts()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L) // 300ms debounce
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    repository.getProducts(searchName = query)
                }
                .collect { state ->
                    _productsState.value = state
                }
        }
    }

    fun fetchProducts(query: String? = null, page: Int = 1, limit: Int = 20) {
        _searchQuery.value = query ?: ""
    }

    fun fetchRecommendedProducts(limit: Int = 10) {
        viewModelScope.launch {
            repository.getProducts(limit = limit).collect { state ->
                _recommendedState.value = state
            }
        }
    }

    fun fetchProductDetail(id: String) {
        viewModelScope.launch {
            repository.getProductDetail(id).collect { state ->
                _productDetailState.value = state
            }
        }
    }
}
