package com.veterapp.data

import androidx.room.withTransaction
import com.veterapp.data.local.AppointmentEntity
import com.veterapp.data.local.AppointmentWithPet
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.AppointmentDto
import com.veterapp.data.remote.toDto
import com.veterapp.data.remote.toEntity
import com.veterapp.domain.Appointment
import com.veterapp.domain.AppointmentDraft
import com.veterapp.domain.AppointmentSaveResult
import com.veterapp.domain.AppointmentStatus
import com.veterapp.domain.AppointmentWarning
import com.veterapp.domain.PetStatus
import com.veterapp.domain.toDomain
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AppointmentRepository(
  private val database: VetDatabase,
  private val settings: AppSettings,
  private val auditRepository: AuditRepository = AuditRepository(database, settings),
) {
  private val appointmentDao = database.appointmentDao()
  private val petDao = database.petDao()
  private val consultationDao = database.consultationDao()
  private val outboxDao = database.outboxDao()
  private val json = Json { encodeDefaults = true }

  fun observeBetween(from: Long, to: Long): Flow<List<Appointment>> =
    appointmentDao.observeBetween(from, to).map { rows -> rows.map(AppointmentWithPet::toDomain) }

  suspend fun findById(id: String): Appointment? =
    appointmentDao.findByIdWithPet(id)?.toDomain()

  suspend fun create(draft: AppointmentDraft, force: Boolean = false): AppointmentSaveResult {
    draft.validate()?.let { return AppointmentSaveResult.Failure(it) }
    val pet = petDao.findById(draft.petId)
      ?: return AppointmentSaveResult.Failure("La mascota no existe")
    if (pet.deletedAt != null) return AppointmentSaveResult.Failure("La mascota está archivada")
    if (pet.status == PetStatus.Deceased.storage) {
      return AppointmentSaveResult.Failure("La mascota está marcada como fallecida")
    }
    val warning = warningFor(draft, excludeId = "")
    if (!force && warning.hasWarnings()) return AppointmentSaveResult.Warning(warning)

    val now = System.currentTimeMillis()
    val entity =
      AppointmentEntity(
        id = UUID.randomUUID().toString(),
        clinicId = pet.clinicId,
        petId = pet.id,
        startsAt = draft.startsAt,
        endsAt = draft.endsAt,
        status = AppointmentStatus.Scheduled.storage,
        reason = draft.reason.trim(),
        notes = draft.notes.clean(),
        consultationId = null,
        cancelReason = null,
        createdAt = now,
        updatedAt = now,
        revision = 0L,
        deletedAt = null,
        originDeviceId = settings.deviceId,
        pendingSync = true,
      )
    enqueueUpsert(
      entity = entity,
      baseRevision = null,
      action = "appointment.create",
      payload =
        buildJsonObject {
          put("pet_id", entity.petId)
          put("starts_at", entity.startsAt)
          put("ends_at", entity.endsAt)
          put("status", entity.status)
        },
    )
    return AppointmentSaveResult.Success(requireAppointment(entity.id))
  }

  suspend fun update(
    appointmentId: String,
    draft: AppointmentDraft,
    force: Boolean = false,
  ): AppointmentSaveResult {
    val existing = appointmentDao.findById(appointmentId)
      ?: return AppointmentSaveResult.Failure("El turno ya no existe")
    if (existing.deletedAt != null) return AppointmentSaveResult.Failure("El turno está archivado")
    if (!AppointmentStatus.fromStorage(existing.status).isOpen) {
      return AppointmentSaveResult.Failure("Solo se pueden editar turnos programados o confirmados")
    }
    draft.validate()?.let { return AppointmentSaveResult.Failure(it) }
    val pet = petDao.findById(draft.petId)
      ?: return AppointmentSaveResult.Failure("La mascota no existe")
    if (pet.deletedAt != null) return AppointmentSaveResult.Failure("La mascota está archivada")
    if (pet.status == PetStatus.Deceased.storage) {
      return AppointmentSaveResult.Failure("La mascota está marcada como fallecida")
    }
    val warning = warningFor(draft, excludeId = existing.id)
    if (!force && warning.hasWarnings()) return AppointmentSaveResult.Warning(warning)

    val updated =
      existing.copy(
        petId = pet.id,
        clinicId = pet.clinicId,
        startsAt = draft.startsAt,
        endsAt = draft.endsAt,
        reason = draft.reason.trim(),
        notes = draft.notes.clean(),
        updatedAt = System.currentTimeMillis(),
        pendingSync = true,
      )
    enqueueUpsert(
      entity = updated,
      baseRevision = existing.revision,
      action = "appointment.update",
      payload =
        buildJsonObject {
          put("pet_id", updated.petId)
          put("starts_at", updated.startsAt)
          put("ends_at", updated.endsAt)
          put("from_starts_at", existing.startsAt)
          put("to_starts_at", updated.startsAt)
        },
    )
    return AppointmentSaveResult.Success(requireAppointment(updated.id))
  }

  suspend fun confirm(appointmentId: String) {
    changeStatus(
      appointmentId = appointmentId,
      target = AppointmentStatus.Confirmed,
      action = "appointment.confirm",
      allowed = setOf(AppointmentStatus.Scheduled),
    )
  }

  suspend fun cancel(appointmentId: String, reason: String?) {
    changeStatus(
      appointmentId = appointmentId,
      target = AppointmentStatus.Cancelled,
      action = "appointment.cancel",
      allowed = setOf(AppointmentStatus.Scheduled, AppointmentStatus.Confirmed),
      cancelReason = reason.clean(),
    )
  }

  suspend fun markNoShow(appointmentId: String) {
    changeStatus(
      appointmentId = appointmentId,
      target = AppointmentStatus.NoShow,
      action = "appointment.no_show",
      allowed = setOf(AppointmentStatus.Scheduled, AppointmentStatus.Confirmed),
    )
  }

  suspend fun markAttended(appointmentId: String, consultationId: String) {
    require(consultationId.isNotBlank()) { "Falta la consulta vinculada" }
    changeStatus(
      appointmentId = appointmentId,
      target = AppointmentStatus.Attended,
      action = "appointment.attend",
      allowed = setOf(AppointmentStatus.Scheduled, AppointmentStatus.Confirmed),
      consultationId = consultationId,
    )
  }

  suspend fun upsertFromRemote(dto: AppointmentDto, clinicId: String) {
    val existing = appointmentDao.findById(dto.id)
    if (existing?.pendingSync == true) return
    appointmentDao.upsert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun markSynced(dto: AppointmentDto, clinicId: String) {
    val existing = appointmentDao.findById(dto.id)
    val entity = dto.toEntity(clinicId, existing?.originDeviceId ?: settings.deviceId)
    appointmentDao.upsert(entity.copy(pendingSync = false))
  }

  private suspend fun changeStatus(
    appointmentId: String,
    target: AppointmentStatus,
    action: String,
    allowed: Set<AppointmentStatus>,
    cancelReason: String? = null,
    consultationId: String? = null,
  ) {
    val existing = appointmentDao.findById(appointmentId)
      ?: error("El turno ya no existe")
    require(existing.deletedAt == null) { "El turno está archivado" }
    val current = AppointmentStatus.fromStorage(existing.status)
    require(current in allowed) { "El turno no se puede cambiar desde ${current.label.lowercase()}" }

    if (target == AppointmentStatus.Attended) {
      val consultation = consultationDao.findById(consultationId.orEmpty())
        ?: error("La consulta vinculada no existe")
      require(consultation.deletedAt == null) { "La consulta vinculada está archivada" }
      require(consultation.petId == existing.petId) { "La consulta pertenece a otra mascota" }
      require(consultation.clinicId == existing.clinicId) { "La consulta pertenece a otra clínica" }
    }

    val updated =
      existing.copy(
        status = target.storage,
        cancelReason = cancelReason,
        consultationId = consultationId ?: existing.consultationId,
        updatedAt = System.currentTimeMillis(),
        pendingSync = true,
      )
    enqueueUpsert(
      entity = updated,
      baseRevision = existing.revision,
      action = action,
      payload =
        buildJsonObject {
          put("pet_id", updated.petId)
          put("from", existing.status)
          put("to", updated.status)
          put("reason", cancelReason)
          put("consultation_id", updated.consultationId)
        },
    )
  }

  private suspend fun warningFor(
    draft: AppointmentDraft,
    excludeId: String,
  ): AppointmentWarning {
    val conflicts =
      appointmentDao.overlapping(
        startsAt = draft.startsAt,
        endsAt = draft.endsAt,
        excludeId = excludeId,
      ).map(AppointmentWithPet::toDomain)
    val outside = !settings.businessHours().isWithin(draft.startsAt, draft.endsAt)
    return AppointmentWarning(conflicts = conflicts, outsideBusinessHours = outside)
  }

  private suspend fun requireAppointment(id: String): Appointment =
    appointmentDao.findByIdWithPet(id)?.toDomain()
      ?: error("No se pudo leer el turno guardado")

  private suspend fun enqueueUpsert(
    entity: AppointmentEntity,
    baseRevision: Long?,
    action: String,
    payload: JsonObject,
  ) {
    val now = System.currentTimeMillis()
    val outbox =
      OutboxEntity(
        operationId = UUID.randomUUID().toString(),
        entityType = "appointment",
        entityId = entity.id,
        operation = "UPSERT",
        payload = json.encodeToString(entity.toDto()),
        baseRevision = baseRevision,
        attempts = 0,
        nextAttemptAt = 0L,
        lastError = null,
        createdAt = now,
      )
    database.withTransaction {
      appointmentDao.upsert(entity)
      outboxDao.insert(outbox)
      auditRepository.record(
        entity = "appointment",
        entityId = entity.id,
        action = action,
        payload = payload,
        clinicId = entity.clinicId,
      )
    }
  }

  private fun AppointmentWarning.hasWarnings(): Boolean =
    conflicts.isNotEmpty() || outsideBusinessHours

  private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotBlank() }
}
