package com.veterapp.data

import com.veterapp.data.remote.ClinicIdDto
import com.veterapp.data.remote.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class AuthRepository(
  private val clientProvider: SupabaseClientProvider,
  private val settings: AppSettings,
) {
  val isConfigured: Boolean = clientProvider.isConfigured && clientProvider.client != null

  val sessionStatus: Flow<SessionStatus> =
    clientProvider.client?.auth?.sessionStatus ?: flowOf(SessionStatus.NotAuthenticated())

  fun currentUserId(): String? = clientProvider.client?.auth?.currentUserOrNull()?.id

  suspend fun signIn(email: String, password: String) {
    val client = requireClient()
    client.auth.signInWith(Email) {
      this.email = email.trim()
      this.password = password
    }
  }

  suspend fun signOut() {
    clientProvider.client?.let { client -> runCatching { client.auth.signOut() } }
    settings.clinicId = null
    settings.userId = null
  }

  suspend fun ensureClinicId(): String {
    settings.userId = currentUserId()
    settings.clinicId?.let { return it }
    val client = requireClient()
    val rows = client.postgrest.rpc("bootstrap_clinic").decodeList<ClinicIdDto>()
    val clinicId = rows.firstOrNull()?.clinicId ?: error("Supabase no devolvió una clínica")
    settings.clinicId = clinicId
    return clinicId
  }

  private fun requireClient() = clientProvider.client ?: error("Supabase no está configurado")
}
