package com.example.shopiku.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://xmlzyswyxiqwmunypgrt.supabase.co",
        supabaseKey = "sb_publishable_KqKpIj7K6luVc6Z2ngMFOg_z8iTdRyC"
    ) {
        install(Postgrest)
    }
}
