package com.example.shopiku.data.repository

import com.example.shopiku.data.common.UiState
import com.example.shopiku.data.model.AddToCartRequest
import com.example.shopiku.data.model.FlashSaleItem
import com.example.shopiku.data.model.Product
import com.example.shopiku.data.model.ProductDetail
import com.example.shopiku.data.model.Review
import com.example.shopiku.data.remote.ProductApiService
import com.example.shopiku.data.remote.RetrofitClient
import com.example.shopiku.data.remote.ShopeeApiService
import com.example.shopiku.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ProductRepository(
    private val apiService: ShopeeApiService = RetrofitClient.apiService,
    private val productApiService: ProductApiService = RetrofitClient.productApiService
) {
    fun getProducts(
        searchName: String? = null,
        page: Int? = null,
        limit: Int? = null
    ): Flow<UiState<List<Product>>> = flow {
        emit(UiState.Loading)
        try {
            val cleanQuery = searchName?.trim()?.replace("\\s+".toRegex(), " ")

            // 1. Ambil data produk dari Supabase (Retrofit Supabase REST Endpoint)
            val response = apiService.getProducts()
            val products = if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                // Fallback ke Supabase SDK Native
                try {
                    SupabaseClient.client.from("products")
                        .select()
                        .decodeList<Product>()
                } catch (e: Exception) {
                    throw Exception("Gagal memuat produk (${response.code()}): ${response.message()}")
                }
            }

            if (products.isEmpty()) {
                emit(UiState.Empty)
            } else {
                // Filter lokal berdasarkan nama, deskripsi, atau kategori
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
            emit(UiState.Error("Terjadi kesalahan koneksi: ${e.localizedMessage ?: "Koneksi bermasalah"}"))
        }
    }.flowOn(Dispatchers.IO)

    fun getProductDetail(id: String): Flow<UiState<Product>> = flow {
        emit(UiState.Loading)
        try {
            val response = apiService.getProductById("eq.$id")
            val productList = response.body()
            val product = productList?.firstOrNull() ?: try {
                SupabaseClient.client.from("products")
                    .select {
                        filter {
                            eq("id", id)
                        }
                    }.decodeSingleOrNull<Product>()
            } catch (e: Exception) {
                null
            }

            if (product != null) {
                emit(UiState.Success(product))
            } else {
                emit(UiState.Error("Detail produk tidak ditemukan"))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Gagal terhubung ke server: ${e.localizedMessage ?: "Koneksi bermasalah"}"))
        }
    }.flowOn(Dispatchers.IO)

    fun getProductDetailById(productId: Long): Flow<UiState<ProductDetail>> = flow {
        emit(UiState.Loading)
        try {
            val response = productApiService.getProductDetail("eq.$productId")
            val detailList = response.body()
            val detail = detailList?.firstOrNull() ?: try {
                SupabaseClient.client.from("products")
                    .select {
                        filter {
                            eq("id", productId)
                        }
                    }.decodeSingleOrNull<ProductDetail>()
            } catch (e: Exception) {
                null
            }

            if (detail != null) {
                emit(UiState.Success(detail))
            } else {
                val fallbackDetail = getFallbackProductDetail(productId)
                emit(UiState.Success(fallbackDetail))
            }
        } catch (e: Exception) {
            val fallbackDetail = getFallbackProductDetail(productId)
            emit(UiState.Success(fallbackDetail))
        }
    }.flowOn(Dispatchers.IO)

    fun getProductReviews(productId: Long): Flow<UiState<List<Review>>> = flow {
        emit(UiState.Loading)
        try {
            val response = productApiService.getProductReviews("eq.$productId")
            val reviews = if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                try {
                    SupabaseClient.client.from("reviews")
                        .select {
                            filter {
                                eq("product_id", productId)
                            }
                        }.decodeList<Review>()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            if (reviews.isEmpty()) {
                // Tampilkan ulasan dummy jika belum ada di Supabase
                emit(UiState.Success(getDummyReviews(productId)))
            } else {
                emit(UiState.Success(reviews))
            }
        } catch (e: Exception) {
            // Fallback ulasan dummy saat koneksi bermasalah
            emit(UiState.Success(getDummyReviews(productId)))
        }
    }.flowOn(Dispatchers.IO)

    fun addToCart(productId: Long, quantity: Int): Flow<UiState<Unit>> = flow {
        emit(UiState.Loading)
        try {
            val request = AddToCartRequest(product_id = productId, quantity = quantity)
            val response = productApiService.addToCart(request)
            if (response.isSuccessful) {
                emit(UiState.Success(Unit))
            } else {
                emit(UiState.Error("Gagal menambahkan produk ke keranjang (${response.code()})"))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Terjadi kesalahan koneksi: ${e.localizedMessage ?: "Gagal terhubung ke server"}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun getFallbackProductDetail(productId: Long): ProductDetail {
        val dummyFlash = FlashSaleItem.getDummyFlashSaleList().firstOrNull { it.id == productId.toString() }
        return if (dummyFlash != null) {
            ProductDetail(
                id = productId,
                name = dummyFlash.name,
                price = dummyFlash.flashSalePrice,
                rating = 4.9,
                description = "Produk official dengan kualitas premium dan garansi resmi. Nikmati penawaran khusus dan diskon terbaik hari ini!",
                imageUrl = dummyFlash.imageUrl,
                category = "Flash Sale"
            )
        } else {
            ProductDetail(
                id = productId,
                name = "Produk Pilihan Shopiku #$productId",
                price = 149000.0,
                rating = 4.8,
                description = "Produk original berkualitas tinggi dari penjual resmi dan terpercaya di Shopiku.",
                imageUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&auto=format&fit=crop&q=60",
                category = "Elektronik"
            )
        }
    }

    companion object {
        fun getDummyReviews(productId: Long): List<Review> {
            return listOf(
                Review(
                    id = "rev_${productId}_1",
                    productId = productId,
                    userName = "Budi Santoso",
                    rating = 5,
                    comment = "Barang original, pengiriman sangat cepat dan packing bubble wrap tebal. Kualitas produk mantap!",
                    createdAt = "08/10/2026"
                ),
                Review(
                    id = "rev_${productId}_2",
                    productId = productId,
                    userName = "Siti Rahmawati",
                    rating = 5,
                    comment = "Sesuai deskripsi! Suara jernih, baterai tahan lama dan nyaman digunakan seharian. Recommended seller!",
                    createdAt = "06/10/2026"
                ),
                Review(
                    id = "rev_${productId}_3",
                    productId = productId,
                    userName = "Rian Pratama",
                    rating = 4,
                    comment = "Kualitas oke untuk harga segini. Berfungsi dengan baik tanpa kendala, kurirnya juga ramah.",
                    createdAt = "03/10/2026"
                ),
                Review(
                    id = "rev_${productId}_4",
                    productId = productId,
                    userName = "Dewi Lestari",
                    rating = 5,
                    comment = "Bagus banget, respon penjual cepat dan ramah. Sudah dites berfungsi 100%. Terimakasih Shopiku!",
                    createdAt = "01/10/2026"
                )
            )
        }
    }
}
