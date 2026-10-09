package com.example.shopiku.data.repository

import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.AddToCartRequest
import com.example.shopiku.data.model.ApiErrorResponse
import com.example.shopiku.data.model.CartItem
import com.example.shopiku.data.model.CartResponse
import com.example.shopiku.data.model.Product
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
    suspend fun getCartItems(): Response<List<CartItem>> = api.getCartItems()

    suspend fun addToCart(item: CartItem): Response<CartItem> = api.addToCart(item)

    suspend fun updateQuantity(cartId: String, item: CartItem): Response<CartItem> =
        api.updateCartQuantity(cartId, item)

    suspend fun deleteCartItem(cartId: String): Response<CartItem> =
        api.deleteCartItem(cartId)

    /**
     * Ambil seluruh item di keranjang beserta detail produknya untuk ditampilkan di CartActivity.
     */
    fun getCartFullItems(): Flow<UiState<List<CartItem>>> = flow {
        emit(UiState.Loading)
        try {
            // 1. Ambil data keranjang dari Supabase via Retrofit / Native
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

            // 2. Ambil daftar produk untuk mencocokkan nama, harga, dan gambar
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

            // 3. Gabungkan data keranjang dengan data produk
            val fullItems = cartList.mapNotNull { cartRow ->
                val pIdStr = cartRow.product_id?.toString() ?: ""
                val product = productMap[pIdStr]
                if (product != null) {
                    CartItem(
                        id = cartRow.id,
                        productId = pIdStr,
                        name = product.name ?: "Produk",
                        price = product.price ?: 0.0,
                        quantity = cartRow.quantity ?: 1,
                        imageUrl = product.imageUrl ?: ""
                    )
                } else if (cartRow.product_id != null) {
                    CartItem(
                        id = cartRow.id,
                        productId = pIdStr,
                        name = "Produk #${cartRow.product_id}",
                        price = 0.0,
                        quantity = cartRow.quantity ?: 1,
                        imageUrl = ""
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
     * Tambah item ke keranjang Supabase dengan dukungan UUID generator, agregasi item duplikat,
     * serta fallback ke Supabase SDK Native jika REST API bermasalah.
     */
    fun addToCartWithResult(productId: Long, quantity: Int): Flow<UiState<CartResponse>> = flow {
        emit(UiState.Loading)
        try {
            // 1. Cek apakah item sudah ada di keranjang untuk menggabungkan kuantitas
            val existingCartResponse = try {
                cartApi.getCartItems()
            } catch (e: Exception) {
                null
            }

            val existingItem = existingCartResponse?.body()?.find { it.product_id == productId }

            var isInsertedViaRetrofit = false
            var resultCartResponse: CartResponse? = null
            var lastErrorMessage: String? = null

            if (existingItem != null && !existingItem.id.isNullOrBlank()) {
                // Update kuantitas item yang sudah ada
                val newQuantity = (existingItem.quantity ?: 0) + quantity
                val request = AddToCartRequest(
                    id = existingItem.id,
                    product_id = productId,
                    quantity = newQuantity
                )
                val response = cartApi.updateCartQuantity("eq.${existingItem.id}", request)
                if (response.isSuccessful) {
                    isInsertedViaRetrofit = true
                    resultCartResponse = response.body()?.firstOrNull() ?: CartResponse(
                        id = existingItem.id,
                        product_id = productId,
                        quantity = newQuantity
                    )
                } else {
                    lastErrorMessage = parseHttpError(response)
                }
            } else {
                // Insert item baru dengan UUID
                val newId = UUID.randomUUID().toString()
                val request = AddToCartRequest(
                    id = newId,
                    product_id = productId,
                    quantity = quantity
                )
                val response = cartApi.addToCart(request)
                if (response.isSuccessful) {
                    isInsertedViaRetrofit = true
                    resultCartResponse = response.body()?.firstOrNull() ?: CartResponse(
                        id = newId,
                        product_id = productId,
                        quantity = quantity
                    )
                } else {
                    lastErrorMessage = parseHttpError(response)
                }
            }

            if (isInsertedViaRetrofit && resultCartResponse != null) {
                emit(UiState.Success(resultCartResponse))
            } else {
                // Fallback ke Supabase SDK Native jika Retrofit gagal
                try {
                    val fallbackId = UUID.randomUUID().toString()
                    val fallbackRequest = AddToCartRequest(
                        id = fallbackId,
                        product_id = productId,
                        quantity = quantity
                    )
                    SupabaseClient.client.from("cart").insert(fallbackRequest)
                    emit(UiState.Success(CartResponse(id = fallbackId, product_id = productId, quantity = quantity)))
                } catch (e: Exception) {
                    val finalError = lastErrorMessage ?: "Gagal memasukkan data ke Supabase: ${e.localizedMessage}"
                    emit(UiState.Error(finalError))
                }
            }
        } catch (e: java.net.UnknownHostException) {
            emit(UiState.Error("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: java.net.SocketTimeoutException) {
            emit(UiState.Error("Koneksi timeout. Coba beberapa saat lagi."))
        } catch (e: Exception) {
            // Fallback ke Supabase SDK Native jika terjadi exception jaringan
            try {
                val fallbackId = UUID.randomUUID().toString()
                val fallbackRequest = AddToCartRequest(
                    id = fallbackId,
                    product_id = productId,
                    quantity = quantity
                )
                SupabaseClient.client.from("cart").insert(fallbackRequest)
                emit(UiState.Success(CartResponse(id = fallbackId, product_id = productId, quantity = quantity)))
            } catch (sdkException: Exception) {
                emit(UiState.Error("Gagal menyimpan ke Supabase: ${sdkException.localizedMessage ?: e.localizedMessage}"))
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Ambil total kuantitas seluruh item di keranjang untuk pembaruan badge.
     */
    fun getCartTotalCount(): Flow<UiState<Int>> = flow {
        emit(UiState.Loading)
        try {
            val response = cartApi.getCartCount()
            if (response.isSuccessful) {
                val items = response.body() ?: emptyList()
                val totalQty = items.sumOf { it.quantity ?: 0 }
                emit(UiState.Success(totalQty))
            } else {
                // Fallback ke Supabase SDK Native
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
            422 -> parsedMessage ?: "Data tidak dapat diproses (422). Periksa foreign key product_id."
            500 -> parsedMessage ?: "Terjadi kesalahan pada server Supabase (500)."
            else -> parsedMessage ?: "Gagal menyimpan ke keranjang (HTTP $code)."
        }
    }
}
