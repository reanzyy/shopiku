package com.example.shopiku.data.repository

import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.Product
import com.example.shopiku.data.remote.RetrofitClient
import com.example.shopiku.data.remote.ShopeeApiService
import com.example.shopiku.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
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
            val cleanQuery = searchName?.trim()?.replace("\\s+".toRegex(), " ")

            // 1. Ambil data secara langsung menggunakan Supabase SDK
            val products = try {
                SupabaseClient.client.from("products")
                    .select()
                    .decodeList<Product>()
            } catch (e: Exception) {
                // 2. Fallback via Retrofit Supabase REST Endpoint
                val response = apiService.getProducts(
                    search = if (cleanQuery.isNullOrBlank()) null else cleanQuery,
                    searchName = if (cleanQuery.isNullOrBlank()) null else cleanQuery,
                    page = page,
                    limit = limit
                )
                if (response.isSuccessful) {
                    response.body() ?: emptyList()
                } else {
                    throw Exception("Supabase REST Error (${response.code()}): ${response.message()}")
                }
            }

            if (products.isEmpty()) {
                emit(UiState.Empty)
            } else {
                val filteredProducts = if (!cleanQuery.isNullOrBlank()) {
                    products.filter { product ->
                        product.name?.contains(cleanQuery, ignoreCase = true) == true ||
                                product.description?.contains(cleanQuery, ignoreCase = true) == true ||
                                product.category?.contains(cleanQuery, ignoreCase = true) == true
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
        } catch (e: Exception) {
            emit(UiState.Error("Terjadi kesalahan koneksi Supabase: ${e.localizedMessage ?: "Koneksi bermasalah"}"))
        }
    }.flowOn(Dispatchers.IO)

    fun getProductDetail(id: String): Flow<UiState<Product>> = flow {
        emit(UiState.Loading)
        try {
            val product = try {
                SupabaseClient.client.from("products")
                    .select {
                        filter {
                            eq("id", id)
                        }
                    }.decodeSingleOrNull<Product>()
            } catch (e: Exception) {
                val response = apiService.getProductById(id)
                if (response.isSuccessful) response.body() else null
            }

            if (product != null) {
                emit(UiState.Success(product))
            } else {
                emit(UiState.Error("Detail produk tidak ditemukan di Supabase"))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Gagal terhubung ke Supabase: ${e.localizedMessage ?: "Koneksi bermasalah"}"))
        }
    }.flowOn(Dispatchers.IO)
}
