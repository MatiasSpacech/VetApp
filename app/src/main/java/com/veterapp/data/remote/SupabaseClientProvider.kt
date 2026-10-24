package com.veterapp.data.remote

import com.veterapp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

class SupabaseClientProvider {
  private val url = BuildConfig.SUPABASE_URL.trim()
  private val key = BuildConfig.SUPABASE_ANON_KEY.trim()

  val isConfigured: Boolean = url.isNotBlank() && key.isNotBlank()

  val client: SupabaseClient? =
    if (isConfigured) {
      runCatching {
          createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
            install(Auth)
            install(Postgrest)
            install(Storage)
          }
        }
        .getOrNull()
    } else {
      null
    }
}
