package com.example.shopiku.data.repository

import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.ProductVariant
import com.example.shopiku.data.remote.ProductVariantApiService
import com.example.shopiku.data.remote.RetrofitClient
import com.example.shopiku.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ProductVariantRepository(
    private val variantApi: ProductVariantApiService = RetrofitClient.productVariantApiService
) {
    /**
     * Ambil data varian produk secara murni dari database Supabase PostgreSQL.
     * Hanya mengembalikan varian yang benar-benar milik product_id yang dipilih
     * dan berstatus aktif (is_active == true).
     */
    fun getProductVariants(productId: Long): Flow<UiState<List<ProductVariant>>> = flow {
        emit(UiState.Loading)
        try {
            // 1. Kueri dari Retrofit API Service (REST endpoint /rest/v1/product_variants?select=*&product_id=eq.{id})
            val response = variantApi.getProductVariants("eq.$productId")
            var variants: List<ProductVariant> = if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }

            // 2. Jika Retrofit kosong/gagal, kueri Supabase Client SDK langsung untuk product_id yang sama
            if (variants.isEmpty()) {
                try {
                    variants = SupabaseClient.client.from("product_variants")
                        .select {
                            filter {
                                eq("product_id", productId)
                            }
                        }.decodeList<ProductVariant>()
                } catch (e: Exception) {
                    // Ignore
                }
            }

            // 3. Filter murni hanya varian yang milik product_id dan berstatus is_active == true
            val activeProductVariants = variants.filter {
                it.productId == productId && it.isActive
            }

            emit(UiState.Success(activeProductVariants))
        } catch (e: Exception) {
            try {
                val variants = SupabaseClient.client.from("product_variants")
                    .select {
                        filter {
                            eq("product_id", productId)
                        }
                    }.decodeList<ProductVariant>()
                    .filter { it.productId == productId && it.isActive }

                emit(UiState.Success(variants))
            } catch (sdkEx: Exception) {
                emit(UiState.Success(emptyList()))
            }
        }
    }.flowOn(Dispatchers.IO)
}
