package com.veterapp.sync

import com.veterapp.data.AppSettings
import com.veterapp.data.AppointmentRepository
import com.veterapp.data.AuthRepository
import com.veterapp.data.ClientRepository
import com.veterapp.data.ClinicalRepository
import com.veterapp.data.PetRepository
import com.veterapp.data.VaccinationRepository
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.AppointmentDto
import com.veterapp.data.remote.ClientDto
import com.veterapp.data.remote.ConsultationAddendumDto
import com.veterapp.data.remote.ConsultationDto
import com.veterapp.data.remote.PetDto
import com.veterapp.data.remote.SupabaseClientProvider
import com.veterapp.data.remote.VaccinationDto
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
  private val clientRepository: ClientRepository,
  private val clinicalRepository: ClinicalRepository,
  private val vaccinationRepository: VaccinationRepository,
  private val appointmentRepository: AppointmentRepository,
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
      pullClients(clinicId)
      pullPets(clinicId)
      pullConsultations(clinicId)
      pullAddenda(clinicId)
      pullVaccinations(clinicId)
      pullAppointments(clinicId)
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
        when (item.entityType) {
          "client" -> applyClientOperation(item, clinicId)
          "pet" -> applyPetOperation(item, clinicId)
          "consultation" -> applyConsultationOperation(item, clinicId)
          "consultation_addendum" -> applyAddendumOperation(item, clinicId)
          "vaccination" -> applyVaccinationOperation(item, clinicId)
          "appointment" -> applyAppointmentOperation(item, clinicId)
          else -> error("Entidad de sincronización desconocida: ${item.entityType}")
        }
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

  private suspend fun applyClientOperation(item: OutboxEntity, clinicId: String) {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    val clientPayload = json.parseToJsonElement(item.payload).jsonObject
    val parameters =
      buildJsonObject {
        put("p_operation_id", item.operationId)
        put("p_clinic_id", clinicId)
        put("p_device_id", settings.deviceId)
        put("p_client", clientPayload)
      }

    val rows =
      client.postgrest
        .rpc("apply_client_operation", parameters)
        .decodeList<ClientDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el cliente")
    clientRepository.markSynced(saved, clinicId)
  }

  private suspend fun applyConsultationOperation(item: OutboxEntity, clinicId: String) {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    val payload = json.parseToJsonElement(item.payload).jsonObject
    val parameters =
      buildJsonObject {
        put("p_operation_id", item.operationId)
        put("p_clinic_id", clinicId)
        put("p_device_id", settings.deviceId)
        put("p_consultation", payload)
      }

    val rows =
      client.postgrest
        .rpc("apply_consultation_operation", parameters)
        .decodeList<ConsultationDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió la consulta")
    clinicalRepository.markConsultationSynced(saved, clinicId)
  }

  private suspend fun applyAddendumOperation(item: OutboxEntity, clinicId: String) {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    val payload = json.parseToJsonElement(item.payload).jsonObject
    val parameters =
      buildJsonObject {
        put("p_operation_id", item.operationId)
        put("p_clinic_id", clinicId)
        put("p_device_id", settings.deviceId)
        put("p_addendum", payload)
      }

    val rows =
      client.postgrest
        .rpc("apply_addendum_operation", parameters)
        .decodeList<ConsultationAddendumDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el addendum")
    clinicalRepository.markAddendumSynced(saved, clinicId)
  }

  private suspend fun applyVaccinationOperation(item: OutboxEntity, clinicId: String) {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    val payload = json.parseToJsonElement(item.payload).jsonObject
    val parameters =
      buildJsonObject {
        put("p_operation_id", item.operationId)
        put("p_clinic_id", clinicId)
        put("p_device_id", settings.deviceId)
        put("p_vaccination", payload)
      }

    val rows =
      client.postgrest
        .rpc("apply_vaccination_operation", parameters)
        .decodeList<VaccinationDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió la vacunación")
    vaccinationRepository.markSynced(saved, clinicId)
  }

  private suspend fun applyAppointmentOperation(item: OutboxEntity, clinicId: String) {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    val payload = json.parseToJsonElement(item.payload).jsonObject
    val parameters =
      buildJsonObject {
        put("p_operation_id", item.operationId)
        put("p_clinic_id", clinicId)
        put("p_device_id", settings.deviceId)
        put("p_appointment", payload)
      }

    val rows =
      client.postgrest
        .rpc("apply_appointment_operation", parameters)
        .decodeList<AppointmentDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el turno")
    appointmentRepository.markSynced(saved, clinicId)
  }

  private suspend fun pullClients(clinicId: String) {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    val rows =
      client.postgrest
        .from("clients")
        .select {
          filter { eq("clinic_id", clinicId) }
        }
        .decodeList<ClientDto>()
    rows.forEach { clientRepository.upsertFromRemote(it, clinicId) }
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

  private suspend fun pullConsultations(clinicId: String) {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    val rows =
      client.postgrest
        .from("consultations")
        .select {
          filter { eq("clinic_id", clinicId) }
        }
        .decodeList<ConsultationDto>()
    rows.forEach { clinicalRepository.upsertConsultationFromRemote(it, clinicId) }
  }

  private suspend fun pullAddenda(clinicId: String) {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    val rows =
      client.postgrest
        .from("consultation_addenda")
        .select {
          filter { eq("clinic_id", clinicId) }
        }
        .decodeList<ConsultationAddendumDto>()
    rows.forEach { clinicalRepository.upsertAddendumFromRemote(it, clinicId) }
  }

  private suspend fun pullVaccinations(clinicId: String) {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    val rows =
      client.postgrest
        .from("vaccinations")
        .select {
          filter { eq("clinic_id", clinicId) }
        }
        .decodeList<VaccinationDto>()
    rows.forEach { vaccinationRepository.upsertFromRemote(it, clinicId) }
  }

  private suspend fun pullAppointments(clinicId: String) {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    val rows =
      client.postgrest
        .from("appointments")
        .select {
          filter { eq("clinic_id", clinicId) }
        }
        .decodeList<AppointmentDto>()
    rows.forEach { appointmentRepository.upsertFromRemote(it, clinicId) }
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
