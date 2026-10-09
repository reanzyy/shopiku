package com.example.shopiku.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinxSerializer
import kotlinx.serialization.json.Json

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://xmlzyswyxiqwmunypgrt.supabase.co",
        supabaseKey = "sb_publishable_KqKpIj7K6luVc6Z2ngMFOg_z8iTdRyC"
    ) {
        defaultSerializer = KotlinxSerializer(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        })
        install(Postgrest)
    }
}
