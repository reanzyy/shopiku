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
class SearchViewModel(
    private val repository: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<UiState<List<Product>>>(UiState.Loading)
    val searchResults: StateFlow<UiState<List<Product>>> = _searchResults.asStateFlow()

    init {
        observeSearchQuery()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L) // Debounce 300ms to avoid excessive API calls
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    repository.getProducts(searchName = query)
                }
                .collect { state ->
                    _searchResults.value = state
                }
        }
    }

    fun retrySearch() {
        val currentQuery = _searchQuery.value
        viewModelScope.launch {
            repository.getProducts(searchName = currentQuery).collect { state ->
                _searchResults.value = state
            }
        }
    }
}
