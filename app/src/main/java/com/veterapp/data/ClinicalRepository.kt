package com.veterapp.data

import androidx.room.withTransaction
import com.veterapp.data.local.ConsultationAddendumEntity
import com.veterapp.data.local.ConsultationEntity
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.ConsultationAddendumDto
import com.veterapp.data.remote.ConsultationDto
import com.veterapp.data.remote.toDto
import com.veterapp.data.remote.toEntity
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.Consultation
import com.veterapp.domain.ConsultationAddendum
import com.veterapp.domain.ConsultationAddendumDraft
import com.veterapp.domain.ConsultationDraft
import com.veterapp.domain.toDomain
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ClinicalRepository(
  private val database: VetDatabase,
  private val settings: AppSettings,
  private val auditRepository: AuditRepository = AuditRepository(database, settings),
) {
  private val consultationDao = database.consultationDao()
  private val addendumDao = database.addendumDao()
  private val petDao = database.petDao()
  private val outboxDao = database.outboxDao()
  private val json = Json { encodeDefaults = true }

  fun observeConsultations(petId: String): Flow<List<Consultation>> =
    consultationDao.observeForPet(petId).map { rows -> rows.map(ConsultationEntity::toDomain) }

  fun observeAddenda(petId: String): Flow<List<ConsultationAddendum>> =
    addendumDao.observeForPet(petId).map { rows -> rows.map(ConsultationAddendumEntity::toDomain) }

  fun observeAddendaForConsultation(consultationId: String): Flow<List<ConsultationAddendum>> =
    addendumDao.observeForConsultation(consultationId).map { rows ->
      rows.map(ConsultationAddendumEntity::toDomain)
    }

  suspend fun createConsultation(petId: String, draft: ConsultationDraft): Consultation {
    draft.validate()?.let { error(it) }
    val pet = petDao.findById(petId) ?: error("La mascota no existe")
    require(pet.deletedAt == null) { "La mascota está archivada" }
    val now = System.currentTimeMillis()
    val entity =
      ConsultationEntity(
        id = UUID.randomUUID().toString(),
        clinicId = pet.clinicId,
        petId = pet.id,
        consultedAt = ClinicTime.instantAtNoon(draft.consultedOn),
        reason = draft.reason.trim(),
        exam = draft.exam.clean(),
        diagnosis = draft.diagnosis.clean(),
        treatment = draft.treatment.clean(),
        medication = draft.medication.clean(),
        instructions = draft.instructions.clean(),
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
      action = "consultation.create",
      payload =
        buildJsonObject {
          put("pet_id", entity.petId)
          put("reason", entity.reason)
          put("consulted_at", entity.consultedAt)
        },
    )
    return entity.toDomain()
  }

  suspend fun addAddendum(
    consultationId: String,
    draft: ConsultationAddendumDraft,
  ): ConsultationAddendum {
    draft.validate()?.let { error(it) }
    val consultation = consultationDao.findById(consultationId)
      ?: error("La consulta ya no existe")
    require(consultation.deletedAt == null) { "La consulta está archivada" }
    val now = System.currentTimeMillis()
    val entity =
      ConsultationAddendumEntity(
        id = UUID.randomUUID().toString(),
        clinicId = consultation.clinicId,
        consultationId = consultation.id,
        reason = draft.reason.trim(),
        text = draft.text.trim(),
        authorId = settings.userId,
        createdAt = now,
        updatedAt = now,
        revision = 0L,
        deletedAt = null,
        originDeviceId = settings.deviceId,
        pendingSync = true,
      )
    val outbox =
      OutboxEntity(
        operationId = UUID.randomUUID().toString(),
        entityType = "consultation_addendum",
        entityId = entity.id,
        operation = "UPSERT",
        payload = json.encodeToString(entity.toDto()),
        baseRevision = null,
        attempts = 0,
        nextAttemptAt = 0L,
        lastError = null,
        createdAt = now,
      )
    database.withTransaction {
      addendumDao.upsert(entity)
      outboxDao.insert(outbox)
      auditRepository.record(
        entity = "consultation",
        entityId = consultation.id,
        action = "consultation.addendum",
        payload =
          buildJsonObject {
            put("addendum_id", entity.id)
            put("reason", entity.reason)
          },
        clinicId = entity.clinicId,
      )
    }
    return entity.toDomain()
  }

  suspend fun findConsultation(id: String): ConsultationEntity? = consultationDao.findById(id)

  suspend fun upsertConsultationFromRemote(dto: ConsultationDto, clinicId: String) {
    val existing = consultationDao.findById(dto.id)
    if (existing?.pendingSync == true) return
    consultationDao.upsert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun markConsultationSynced(dto: ConsultationDto, clinicId: String) {
    val existing = consultationDao.findById(dto.id)
    val entity = dto.toEntity(clinicId, existing?.originDeviceId ?: settings.deviceId)
    consultationDao.upsert(entity.copy(pendingSync = false))
  }

  suspend fun upsertAddendumFromRemote(dto: ConsultationAddendumDto, clinicId: String) {
    val existing = addendumDao.findById(dto.id)
    if (existing?.pendingSync == true) return
    addendumDao.upsert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun markAddendumSynced(dto: ConsultationAddendumDto, clinicId: String) {
    val existing = addendumDao.findById(dto.id)
    val entity = dto.toEntity(clinicId, existing?.originDeviceId ?: settings.deviceId)
    addendumDao.upsert(entity.copy(pendingSync = false))
  }

  private suspend fun enqueueUpsert(
    entity: ConsultationEntity,
    baseRevision: Long?,
    action: String,
    payload: JsonObject,
  ) {
    val now = System.currentTimeMillis()
    val outbox =
      OutboxEntity(
        operationId = UUID.randomUUID().toString(),
        entityType = "consultation",
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
      consultationDao.upsert(entity)
      outboxDao.insert(outbox)
      auditRepository.record(
        entity = "consultation",
        entityId = entity.id,
        action = action,
        payload = payload,
        clinicId = entity.clinicId,
      )
    }
  }

  private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotBlank() }
}
