package com.example.shopiku.data.repository

import android.util.Log
import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.ApiErrorResponse
import com.example.shopiku.data.model.ProductVariant
import com.example.shopiku.data.remote.ProductVariantApiService
import com.example.shopiku.data.remote.RetrofitClient
import com.example.shopiku.data.remote.SupabaseClient
import com.google.gson.Gson
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response

class ProductVariantRepository(
    private val variantApi: ProductVariantApiService = RetrofitClient.productVariantApiService
) {
    /**
     * Ambil data varian produk secara murni dari database Supabase PostgreSQL.
     * Hanya mengembalikan varian yang benar-benar milik product_id yang dipilih
     * dan berstatus aktif (is_active == true).
     *
     * Membedakan Success / Empty / Error — tidak pernah menyembunyikan kegagalan
     * sebagai daftar kosong.
     */
    fun getProductVariants(productId: Long): Flow<UiState<List<ProductVariant>>> = flow {
        emit(UiState.Loading)

        var lastError: String? = null
        var retrofitSucceededEmpty = false

        // 1. Retrofit PostgREST
        try {
            val response = variantApi.getProductVariants(
                productIdFilter = "eq.$productId",
                isActiveFilter = "eq.true",
                order = "id.asc"
            )
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                val active = filterActiveForProduct(body, productId)
                Log.d(TAG, "Retrofit OK productId=$productId count=${active.size}")
                if (active.isNotEmpty()) {
                    emit(UiState.Success(active))
                    return@flow
                }
                // Successful but empty — fall through to SDK as a second check
                retrofitSucceededEmpty = true
            } else {
                lastError = parseHttpError(response)
                Log.e(TAG, "Retrofit HTTP ${response.code()} productId=$productId: $lastError")
            }
        } catch (e: Exception) {
            lastError = e.localizedMessage ?: "Gagal terhubung ke server"
            Log.e(TAG, "Retrofit exception productId=$productId: $lastError", e)
        }

        // 2. Fallback SDK (transport failure, HTTP error, or empty Retrofit body)
        try {
            val variants = SupabaseClient.client.from("product_variants")
                .select {
                    filter {
                        eq("product_id", productId)
                        eq("is_active", true)
                    }
                }.decodeList<ProductVariant>()

            val active = filterActiveForProduct(variants, productId)
            Log.d(TAG, "SDK OK productId=$productId count=${active.size}")
            if (active.isEmpty()) {
                emit(UiState.Empty)
            } else {
                emit(UiState.Success(active))
            }
        } catch (sdkEx: Exception) {
            val sdkMessage = sdkEx.localizedMessage ?: "Gagal mengambil varian dari Supabase"
            Log.e(TAG, "SDK exception productId=$productId: $sdkMessage", sdkEx)
            if (retrofitSucceededEmpty) {
                // Retrofit already confirmed empty with HTTP 200
                emit(UiState.Empty)
            } else {
                val message = lastError ?: sdkMessage
                emit(UiState.Error("Gagal memuat varian produk: $message"))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun filterActiveForProduct(
        variants: List<ProductVariant>,
        productId: Long
    ): List<ProductVariant> {
        return variants
            .filter { it.productId == productId && it.isActive }
            .sortedBy { it.id }
    }

    private fun <T> parseHttpError(response: Response<T>): String {
        val code = response.code()
        val errorBodyString = try {
            response.errorBody()?.string()
        } catch (e: Exception) {
            null
        }

        val parsedMessage = try {
            if (!errorBodyString.isNullOrBlank()) {
                val errorObj = Gson().fromJson(errorBodyString, ApiErrorResponse::class.java)
                errorObj.message ?: errorObj.details
            } else null
        } catch (e: Exception) {
            null
        }

        return when (code) {
            400 -> parsedMessage ?: "Permintaan tidak valid (400). Periksa skema tabel product_variants."
            401, 403 -> parsedMessage
                ?: "Akses ditolak ($code). Periksa RLS Policy untuk product_variants."
            404 -> parsedMessage ?: "Tabel atau endpoint product_variants tidak ditemukan (404)."
            422 -> parsedMessage ?: "Data tidak dapat diproses (422)."
            500 -> parsedMessage ?: "Terjadi kesalahan pada server Supabase (500)."
            else -> parsedMessage ?: "Gagal memuat varian (HTTP $code)."
        }
    }

    companion object {
        private const val TAG = "ProductVariantRepo"
    }
}
