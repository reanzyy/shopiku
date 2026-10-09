package com.example.shopiku.data.repository

import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.AddToCartRequest
import com.example.shopiku.data.model.ApiErrorResponse
import com.example.shopiku.data.model.CartItem
import com.example.shopiku.data.model.CartResponse
import com.example.shopiku.data.model.Product
import com.example.shopiku.data.model.ProductVariant
import com.example.shopiku.data.remote.CartApiService
import com.example.shopiku.data.remote.RetrofitClient
import com.example.shopiku.data.remote.ShopeeApiService
import com.example.shopiku.data.remote.SupabaseClient
import com.google.gson.Gson
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response
import java.util.UUID

class CartRepository(
    private val api: ShopeeApiService = RetrofitClient.apiService,
    private val cartApi: CartApiService = RetrofitClient.cartApiService
) {
    /**
     * Ambil seluruh item di keranjang beserta detail produk dan variannya.
     */
    fun getCartFullItems(): Flow<UiState<List<CartItem>>> = flow {
        emit(UiState.Loading)
        try {
            val cartList: List<CartResponse> = try {
                val resp = cartApi.getCartItems()
                if (resp.isSuccessful) resp.body() ?: emptyList()
                else SupabaseClient.client.from("cart").select().decodeList<CartResponse>()
            } catch (e: Exception) {
                try {
                    SupabaseClient.client.from("cart").select().decodeList<CartResponse>()
                } catch (sdkEx: Exception) {
                    emptyList()
                }
            }

            if (cartList.isEmpty()) {
                emit(UiState.Empty)
                return@flow
            }

            val productsList = try {
                val resp = api.getProducts()
                if (resp.isSuccessful) resp.body() ?: emptyList()
                else SupabaseClient.client.from("products").select().decodeList<Product>()
            } catch (e: Exception) {
                try {
                    SupabaseClient.client.from("products").select().decodeList<Product>()
                } catch (sdkEx: Exception) {
                    emptyList()
                }
            }

            val productMap = productsList.associateBy { it.id }

            // Ambil data varian
            val allVariants = try {
                SupabaseClient.client.from("product_variants").select().decodeList<ProductVariant>()
            } catch (e: Exception) {
                emptyList()
            }
            val variantMap = allVariants.associateBy { it.id }

            val fullItems = cartList.mapNotNull { cartRow ->
                val pIdStr = cartRow.product_id?.toString() ?: ""
                val product = productMap[pIdStr]
                val variant = cartRow.variant_id?.let { variantMap[it] }

                val basePrice = product?.price ?: 0.0
                val addPrice = variant?.additionalPrice ?: 0.0
                val finalUnitPrice = basePrice + addPrice

                val imageUrl = variant?.imageUrl.takeIf { !it.isNullOrBlank() }
                    ?: product?.imageUrl ?: ""

                val variantDisplayStr = variant?.getDisplayName() ?: "Variansi Standar"

                if (product != null) {
                    CartItem(
                        id = cartRow.id,
                        productId = pIdStr,
                        variantId = cartRow.variant_id,
                        variantName = variantDisplayStr,
                        name = product.name ?: "Produk",
                        price = finalUnitPrice,
                        quantity = cartRow.quantity ?: 1,
                        imageUrl = imageUrl
                    )
                } else if (cartRow.product_id != null) {
                    CartItem(
                        id = cartRow.id,
                        productId = pIdStr,
                        variantId = cartRow.variant_id,
                        variantName = variantDisplayStr,
                        name = "Produk #${cartRow.product_id}",
                        price = finalUnitPrice,
                        quantity = cartRow.quantity ?: 1,
                        imageUrl = imageUrl
                    )
                } else null
            }

            if (fullItems.isEmpty()) {
                emit(UiState.Empty)
            } else {
                emit(UiState.Success(fullItems))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Gagal memuat item keranjang: ${e.localizedMessage ?: "Terjadi kesalahan"}"))
        }
    }.flowOn(Dispatchers.IO)

    fun updateCartItemQuantity(cartId: String, productId: Long, newQuantity: Int): Flow<UiState<Unit>> = flow {
        emit(UiState.Loading)
        try {
            val req = AddToCartRequest(id = cartId, product_id = productId, quantity = newQuantity)
            val resp = cartApi.updateCartQuantity("eq.$cartId", req)
            if (resp.isSuccessful) {
                emit(UiState.Success(Unit))
            } else {
                try {
                    SupabaseClient.client.from("cart").update({
                        set("quantity", newQuantity)
                    }) {
                        filter { eq("id", cartId) }
                    }
                    emit(UiState.Success(Unit))
                } catch (e: Exception) {
                    emit(UiState.Error("Gagal memperbarui kuantitas"))
                }
            }
        } catch (e: Exception) {
            emit(UiState.Error("Gagal terhubung ke server"))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteCartItemById(cartId: String): Flow<UiState<Unit>> = flow {
        emit(UiState.Loading)
        try {
            val resp = cartApi.deleteCartItem("eq.$cartId")
            if (resp.isSuccessful) {
                emit(UiState.Success(Unit))
            } else {
                try {
                    SupabaseClient.client.from("cart").delete {
                        filter { eq("id", cartId) }
                    }
                    emit(UiState.Success(Unit))
                } catch (e: Exception) {
                    emit(UiState.Error("Gagal menghapus item dari keranjang"))
                }
            }
        } catch (e: Exception) {
            emit(UiState.Error("Gagal terhubung ke server"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Tambah item ke keranjang Supabase dengan dukungan variant_id.
     */
    fun addToCartWithResult(
        productId: Long,
        variantId: Long? = null,
        quantity: Int
    ): Flow<UiState<CartResponse>> = flow {
        emit(UiState.Loading)
        try {
            val existingCartResponse = try {
                cartApi.getCartItems()
            } catch (e: Exception) {
                null
            }

            val existingItem = existingCartResponse?.body()?.find {
                it.product_id == productId && it.variant_id == variantId
            }

            var isInsertedViaRetrofit = false
            var resultCartResponse: CartResponse? = null
            var lastErrorMessage: String? = null

            if (existingItem != null && !existingItem.id.isNullOrBlank()) {
                val newQuantity = (existingItem.quantity ?: 0) + quantity
                val request = AddToCartRequest(
                    id = existingItem.id,
                    product_id = productId,
                    variant_id = variantId,
                    quantity = newQuantity
                )
                val response = cartApi.updateCartQuantity("eq.${existingItem.id}", request)
                if (response.isSuccessful) {
                    isInsertedViaRetrofit = true
                    resultCartResponse = response.body()?.firstOrNull() ?: CartResponse(
                        id = existingItem.id,
                        product_id = productId,
                        variant_id = variantId,
                        quantity = newQuantity
                    )
                } else {
                    lastErrorMessage = parseHttpError(response)
                }
            } else {
                val newId = UUID.randomUUID().toString()
                val request = AddToCartRequest(
                    id = newId,
                    product_id = productId,
                    variant_id = variantId,
                    quantity = quantity
                )
                val response = cartApi.addToCart(request)
                if (response.isSuccessful) {
                    isInsertedViaRetrofit = true
                    resultCartResponse = response.body()?.firstOrNull() ?: CartResponse(
                        id = newId,
                        product_id = productId,
                        variant_id = variantId,
                        quantity = quantity
                    )
                } else {
                    lastErrorMessage = parseHttpError(response)
                }
            }

            if (isInsertedViaRetrofit && resultCartResponse != null) {
                emit(UiState.Success(resultCartResponse))
            } else {
                try {
                    val fallbackId = UUID.randomUUID().toString()
                    val fallbackRequest = AddToCartRequest(
                        id = fallbackId,
                        product_id = productId,
                        variant_id = variantId,
                        quantity = quantity
                    )
                    SupabaseClient.client.from("cart").insert(fallbackRequest)
                    emit(UiState.Success(CartResponse(id = fallbackId, product_id = productId, variant_id = variantId, quantity = quantity)))
                } catch (e: Exception) {
                    val finalError = lastErrorMessage ?: "Gagal memasukkan data ke Supabase: ${e.localizedMessage}"
                    emit(UiState.Error(finalError))
                }
            }
        } catch (e: Exception) {
            try {
                val fallbackId = UUID.randomUUID().toString()
                val fallbackRequest = AddToCartRequest(
                    id = fallbackId,
                    product_id = productId,
                    variant_id = variantId,
                    quantity = quantity
                )
                SupabaseClient.client.from("cart").insert(fallbackRequest)
                emit(UiState.Success(CartResponse(id = fallbackId, product_id = productId, variant_id = variantId, quantity = quantity)))
            } catch (sdkException: Exception) {
                emit(UiState.Error("Gagal menyimpan ke Supabase: ${sdkException.localizedMessage ?: e.localizedMessage}"))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getCartTotalCount(): Flow<UiState<Int>> = flow {
        emit(UiState.Loading)
        try {
            val response = cartApi.getCartCount()
            if (response.isSuccessful) {
                val items = response.body() ?: emptyList()
                val totalQty = items.sumOf { it.quantity ?: 0 }
                emit(UiState.Success(totalQty))
            } else {
                try {
                    val items = SupabaseClient.client.from("cart").select().decodeList<CartResponse>()
                    val totalQty = items.sumOf { it.quantity ?: 0 }
                    emit(UiState.Success(totalQty))
                } catch (e: Exception) {
                    emit(UiState.Error("Gagal mengambil jumlah keranjang"))
                }
            }
        } catch (e: Exception) {
            try {
                val items = SupabaseClient.client.from("cart").select().decodeList<CartResponse>()
                val totalQty = items.sumOf { it.quantity ?: 0 }
                emit(UiState.Success(totalQty))
            } catch (sdkException: Exception) {
                emit(UiState.Error(sdkException.localizedMessage ?: "Terjadi kesalahan koneksi"))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun <T> parseHttpError(response: Response<T>): String {
        val code = response.code()
        val errorBodyString = response.errorBody()?.string()

        val parsedMessage = try {
            if (!errorBodyString.isNullOrBlank()) {
                val errorObj = Gson().fromJson(errorBodyString, ApiErrorResponse::class.java)
                errorObj.message ?: errorObj.details
            } else null
        } catch (e: Exception) {
            null
        }

        return when (code) {
            400 -> parsedMessage ?: "Permintaan tidak valid (400). Periksa skema tabel Supabase."
            401, 403 -> parsedMessage ?: "Akses ditolak (401/403). Periksa RLS Policy di Supabase Dashboard."
            404 -> parsedMessage ?: "Tabel atau endpoint 'cart' tidak ditemukan (404)."
            422 -> parsedMessage ?: "Data tidak dapat diproses (422)."
            500 -> parsedMessage ?: "Terjadi kesalahan pada server Supabase (500)."
            else -> parsedMessage ?: "Gagal menyimpan ke keranjang (HTTP $code)."
        }
    }
}
