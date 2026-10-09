package com.example.shopiku.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // URL Supabase Postgrest REST API
    private const val BASE_URL = "https://xmlzyswyxiqwmunypgrt.supabase.co/rest/v1/"
    private const val SUPABASE_KEY = "sb_publishable_KqKpIj7K6luVc6Z2ngMFOg_z8iTdRyC"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer $SUPABASE_KEY")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val apiService: ShopeeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ShopeeApiService::class.java)
    }
}
