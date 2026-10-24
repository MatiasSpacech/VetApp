package com.veterapp.sync

import com.veterapp.data.AppSettings
import com.veterapp.data.AuthRepository
import com.veterapp.data.PetRepository
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.PetDto
import com.veterapp.data.remote.SupabaseClientProvider
import com.veterapp.util.isNetworkFailure
import com.veterapp.util.userMessage
import io.github.jan.supabase.postgrest.postgrest
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class SyncRepository(
  private val database: VetDatabase,
  private val settings: AppSettings,
  private val clientProvider: SupabaseClientProvider,
  private val authRepository: AuthRepository,
  private val petRepository: PetRepository,
) {
  private val outboxDao = database.outboxDao()
  private val json = Json { ignoreUnknownKeys = true }

  private val _lastSyncAt = MutableStateFlow(settings.lastSyncAt)
  val lastSyncAt: StateFlow<Long> = _lastSyncAt.asStateFlow()

  private val _lastError = MutableStateFlow<String?>(null)
  val lastError: StateFlow<String?> = _lastError.asStateFlow()

  suspend fun syncNow(): SyncResult {
    if (!clientProvider.isConfigured || clientProvider.client == null) return SyncResult.NotConfigured
    if (authRepository.currentUserId() == null) return SyncResult.NotAuthenticated

    _lastError.value = null
    return try {
      val clinicId = authRepository.ensureClinicId()
      pushPending(clinicId)
      pullPets(clinicId)
      val now = System.currentTimeMillis()
      settings.lastSyncAt = now
      _lastSyncAt.value = now
      _lastError.value = null
      SyncResult.Success
    } catch (error: Throwable) {
      val message = error.toSyncMessage()
      _lastError.value = message
      SyncResult.Failure(message)
    }
  }

  private suspend fun pushPending(clinicId: String) {
    val items = outboxDao.due(System.currentTimeMillis(), limit = 50)
    for (item in items) {
      try {
        applyPetOperation(item, clinicId)
        outboxDao.delete(item.operationId)
      } catch (error: Throwable) {
        val attempts = item.attempts + 1
        val backoff = min(MAX_BACKOFF_MS, BASE_BACKOFF_MS shl min(attempts, 6))
        outboxDao.update(
          item.copy(
            attempts = attempts,
            nextAttemptAt = System.currentTimeMillis() + backoff,
            lastError = error.toSyncMessage(),
          ),
        )
        throw error
      }
    }
  }

  private suspend fun applyPetOperation(item: OutboxEntity, clinicId: String) {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    val petPayload = json.parseToJsonElement(item.payload).jsonObject
    val parameters =
      buildJsonObject {
        put("p_operation_id", item.operationId)
        put("p_clinic_id", clinicId)
        put("p_device_id", settings.deviceId)
        put("p_pet", petPayload)
      }

    val rows =
      client.postgrest
        .rpc("apply_pet_operation", parameters)
        .decodeList<PetDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió la mascota")
    petRepository.markSynced(saved, clinicId)
  }

  private suspend fun pullPets(clinicId: String) {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    val rows =
      client.postgrest
        .from("pets")
        .select {
          filter { eq("clinic_id", clinicId) }
        }
        .decodeList<PetDto>()
    rows.forEach { petRepository.upsertFromRemote(it, clinicId) }
  }

  sealed interface SyncResult {
    data object Success : SyncResult

    data object NotConfigured : SyncResult

    data object NotAuthenticated : SyncResult

    data class Failure(val message: String) : SyncResult
  }

  private fun Throwable.toSyncMessage(): String =
    if (isNetworkFailure()) "Sin conexión con Supabase"
    else userMessage("No se pudo sincronizar")

  private companion object {
    const val BASE_BACKOFF_MS = 5_000L
    const val MAX_BACKOFF_MS = 5 * 60_000L
  }
}
