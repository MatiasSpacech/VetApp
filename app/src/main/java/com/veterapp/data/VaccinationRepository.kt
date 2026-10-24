package com.veterapp.data

import androidx.room.withTransaction
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.VaccinationEntity
import com.veterapp.data.local.VaccinationWithPet
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.VaccinationDto
import com.veterapp.data.remote.toDto
import com.veterapp.data.remote.toEntity
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.Vaccination
import com.veterapp.domain.VaccinationAlert
import com.veterapp.domain.VaccinationDraft
import com.veterapp.domain.VaccinationReminderCandidate
import com.veterapp.domain.latestCandidatesPerPetAndVaccine
import com.veterapp.domain.toDomain
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class VaccinationRepository(
  private val database: VetDatabase,
  private val settings: AppSettings,
  private val auditRepository: AuditRepository = AuditRepository(database, settings),
) {
  private val vaccinationDao = database.vaccinationDao()
  private val petDao = database.petDao()
  private val outboxDao = database.outboxDao()
  private val json = Json { encodeDefaults = true }

  fun observeForPet(petId: String): Flow<List<Vaccination>> =
    vaccinationDao.observeForPet(petId).map { rows -> rows.map(VaccinationEntity::toDomain) }

  suspend fun allForPet(petId: String): List<Vaccination> =
    vaccinationDao.allForPet(petId).map(VaccinationEntity::toDomain)

  fun alerts(): Flow<List<VaccinationAlert>> =
    vaccinationDao.observeActiveWithPet().map { rows -> buildAlerts(rows, ClinicTime.today()) }

  fun alertsFor(today: LocalDate): Flow<List<VaccinationAlert>> =
    vaccinationDao.observeActiveWithPet().map { rows -> buildAlerts(rows, today) }

  suspend fun reminderCandidates(): List<VaccinationReminderCandidate> =
    vaccinationDao.activeWithPet().map { row ->
      VaccinationReminderCandidate(row.vaccination.toDomain(), row.petName)
    }

  suspend fun latestFor(petId: String, vaccineName: String): Vaccination? =
    vaccinationDao.findLatestFor(petId, vaccineName)?.toDomain()

  suspend fun createVaccination(petId: String, draft: VaccinationDraft): Vaccination {
    draft.validate()?.let { error(it) }
    val pet = petDao.findById(petId) ?: error("La mascota no existe")
    require(pet.deletedAt == null) { "La mascota está archivada" }
    val now = System.currentTimeMillis()
    val entity =
      VaccinationEntity(
        id = UUID.randomUUID().toString(),
        clinicId = pet.clinicId,
        petId = pet.id,
        vaccineName = draft.vaccineName.trim(),
        appliedOn = draft.appliedOn,
        nextDueOn = draft.nextDueOn(),
        lot = draft.lot.clean(),
        notes = draft.notes.clean(),
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
      action = "vaccination.create",
      payload =
        buildJsonObject {
          put("pet_id", entity.petId)
          put("vaccine_name", entity.vaccineName)
          put("applied_on", entity.appliedOn)
          put("next_due_on", entity.nextDueOn)
        },
    )
    return entity.toDomain()
  }

  suspend fun reschedule(vaccinationId: String, newDueOn: Long, reason: String?) {
    val existing = vaccinationDao.findById(vaccinationId)
      ?: error("La vacunación ya no existe")
    require(existing.deletedAt == null) { "La vacunación está archivada" }
    require(newDueOn > existing.appliedOn) {
      "El próximo aviso debe ser posterior a la aplicación"
    }
    if (existing.nextDueOn == newDueOn) return
    val updated =
      existing.copy(
        nextDueOn = newDueOn,
        updatedAt = System.currentTimeMillis(),
        pendingSync = true,
      )
    enqueueUpsert(
      entity = updated,
      baseRevision = existing.revision,
      action = "vaccination.reschedule",
      payload =
        buildJsonObject {
          put("pet_id", updated.petId)
          put("vaccine_name", updated.vaccineName)
          put("from", existing.nextDueOn)
          put("to", newDueOn)
          put("reason", reason?.trim()?.takeIf { it.isNotBlank() })
        },
    )
  }

  suspend fun upsertFromRemote(dto: VaccinationDto, clinicId: String) {
    val existing = vaccinationDao.findById(dto.id)
    if (existing?.pendingSync == true) return
    vaccinationDao.upsert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun markSynced(dto: VaccinationDto, clinicId: String) {
    val existing = vaccinationDao.findById(dto.id)
    val entity = dto.toEntity(clinicId, existing?.originDeviceId ?: settings.deviceId)
    vaccinationDao.upsert(entity.copy(pendingSync = false))
  }

  private fun buildAlerts(
    rows: List<VaccinationWithPet>,
    today: LocalDate,
  ): List<VaccinationAlert> =
    rows
      .map { VaccinationReminderCandidate(it.vaccination.toDomain(), it.petName) }
      .latestCandidatesPerPetAndVaccine()
      .mapNotNull { candidate ->
        val dueOn = candidate.vaccination.nextDueOn ?: return@mapNotNull null
        VaccinationAlert(
          vaccination = candidate.vaccination,
          petName = candidate.petName,
          daysUntilDue = dueOn - today.toEpochDay(),
        )
      }
      .filter { it.daysUntilDue <= ALERT_WINDOW_DAYS }
      .sortedWith(compareBy<VaccinationAlert>({ it.daysUntilDue }, { it.petName.lowercase() }))

  private suspend fun enqueueUpsert(
    entity: VaccinationEntity,
    baseRevision: Long?,
    action: String,
    payload: JsonObject,
  ) {
    val now = System.currentTimeMillis()
    val outbox =
      OutboxEntity(
        operationId = UUID.randomUUID().toString(),
        entityType = "vaccination",
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
      vaccinationDao.upsert(entity)
      outboxDao.insert(outbox)
      auditRepository.record(
        entity = "vaccination",
        entityId = entity.id,
        action = action,
        payload = payload,
        clinicId = entity.clinicId,
      )
    }
  }

  private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotBlank() }

  private companion object {
    const val ALERT_WINDOW_DAYS = 30L
  }
}
