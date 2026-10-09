package com.example.shopiku.data.repository

import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.Product
import com.example.shopiku.data.remote.RetrofitClient
import com.example.shopiku.data.remote.ShopeeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ProductRepository(
    private val apiService: ShopeeApiService = RetrofitClient.apiService
) {
    fun getProducts(
        searchName: String? = null,
        page: Int? = null,
        limit: Int? = null
    ): Flow<UiState<List<Product>>> = flow {
        emit(UiState.Loading)
        try {
            // Clean extra spaces & handle empty/blank search queries
            val cleanQuery = searchName?.trim()?.replace("\\s+".toRegex(), " ")
            val apiQuery = if (cleanQuery.isNullOrBlank()) null else cleanQuery

            val response = apiService.getProducts(
                search = apiQuery,
                searchName = apiQuery,
                page = page,
                limit = limit
            )

            if (response.isSuccessful) {
                val products = response.body()
                if (products.isNullOrEmpty()) {
                    emit(UiState.Empty)
                } else {
                    // Fallback local filtering if backend returns unfiltered list
                    val filteredProducts = if (!apiQuery.isNullOrBlank()) {
                        products.filter { product ->
                            product.name?.contains(apiQuery, ignoreCase = true) == true ||
                                    product.description?.contains(apiQuery, ignoreCase = true) == true ||
                                    product.category?.contains(apiQuery, ignoreCase = true) == true
                        }
                    } else {
                        products
                    }

                    if (filteredProducts.isEmpty()) {
                        emit(UiState.Empty)
                    } else {
                        emit(UiState.Success(filteredProducts))
                    }
                }
            } else {
                emit(UiState.Error("Gagal memuat produk (${response.code()}): ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Terjadi kesalahan jaringan: ${e.localizedMessage ?: "Koneksi bermasalah"}"))
        }
    }.flowOn(Dispatchers.IO)

    fun getProductDetail(id: String): Flow<UiState<Product>> = flow {
        emit(UiState.Loading)
        try {
            val response = apiService.getProductById(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                emit(UiState.Success(body))
            } else {
                emit(UiState.Error("Detail produk tidak ditemukan (${response.code()})"))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Gagal terhubung ke server: ${e.localizedMessage ?: "Koneksi bermasalah"}"))
        }
    }.flowOn(Dispatchers.IO)
}
