package com.example.shopiku.data.remote

import com.example.shopiku.data.model.ProductVariant
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ProductVariantApiService {

    @GET("product_variants?select=*")
    suspend fun getProductVariants(
        @Query("product_id") productIdFilter: String,
        @Query("is_active") isActiveFilter: String = "eq.true",
        @Query("order") order: String = "id.asc"
    ): Response<List<ProductVariant>>
}
