package com.veterapp.data

import androidx.room.withTransaction
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.PetEntity
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.PetDto
import com.veterapp.data.remote.toDto
import com.veterapp.data.remote.toEntity
import com.veterapp.domain.Pet
import com.veterapp.domain.PetDraft
import com.veterapp.domain.toDomain
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class PetRepository(
  private val database: VetDatabase,
  private val settings: AppSettings,
  private val auditRepository: AuditRepository = AuditRepository(database, settings),
) {
  private val petDao = database.petDao()
  private val outboxDao = database.outboxDao()
  private val json = Json { encodeDefaults = true }

  val pets: Flow<List<Pet>> = petDao.observeActive().map { rows -> rows.map(PetEntity::toDomain) }

  val pendingCount: Flow<Int> = outboxDao.observeCount()

  suspend fun createPet(
    clinicId: String,
    petId: String,
    draft: PetDraft,
    fotoUrl: String?,
  ): Pet {
    val now = System.currentTimeMillis()
    val entity =
      PetEntity(
        id = petId,
        clinicId = clinicId,
        name = draft.name.trim(),
        species = draft.species.trim(),
        breed = draft.breed?.trim()?.takeIf { it.isNotBlank() },
        ownerName = draft.ownerName.trim(),
        notes = draft.notes?.trim()?.takeIf { it.isNotBlank() },
        fotoUrl = fotoUrl,
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
      action = "pet.create",
      payload =
        buildJsonObject {
          put("name", entity.name)
          put("owner_name", entity.ownerName)
        },
    )
    return entity.toDomain()
  }

  suspend fun updatePet(
    petId: String,
    draft: PetDraft,
    fotoUrl: String? = null,
    removePhoto: Boolean = false,
  ): Pet {
    val existing = petDao.findById(petId) ?: error("La mascota ya no existe")
    val now = System.currentTimeMillis()
    val updated =
      existing.copy(
        name = draft.name.trim(),
        species = draft.species.trim(),
        breed = draft.breed?.trim()?.takeIf { it.isNotBlank() },
        ownerName = draft.ownerName.trim(),
        notes = draft.notes?.trim()?.takeIf { it.isNotBlank() },
        fotoUrl =
          when {
            removePhoto -> null
            fotoUrl != null -> fotoUrl
            else -> existing.fotoUrl
          },
        updatedAt = now,
        pendingSync = true,
      )
    val changed =
      buildList {
        if (existing.name != updated.name) add("nombre")
        if (existing.species != updated.species) add("especie")
        if (existing.breed != updated.breed) add("raza")
        if (existing.ownerName != updated.ownerName) add("dueño")
        if (existing.notes != updated.notes) add("notas")
        if (existing.fotoUrl != updated.fotoUrl) add("foto")
      }
    enqueueUpsert(
      entity = updated,
      baseRevision = existing.revision,
      action = "pet.update",
      payload =
        buildJsonObject {
          put("name", updated.name)
          put("owner_name", updated.ownerName)
          put("changed", changed.joinToString(", "))
        },
    )
    return updated.toDomain()
  }

  suspend fun softDelete(petId: String) {
    val existing = petDao.findById(petId) ?: return
    if (existing.deletedAt != null) return
    val now = System.currentTimeMillis()
    val deleted =
      existing.copy(
        deletedAt = now,
        updatedAt = now,
        pendingSync = true,
      )
    enqueueUpsert(
      entity = deleted,
      baseRevision = existing.revision,
      action = "pet.delete",
      payload =
        buildJsonObject {
          put("name", deleted.name)
          put("owner_name", deleted.ownerName)
        },
    )
  }

  private suspend fun enqueueUpsert(
    entity: PetEntity,
    baseRevision: Long?,
    action: String,
    payload: JsonObject,
  ) {
    val now = System.currentTimeMillis()
    val outbox =
      OutboxEntity(
        operationId = UUID.randomUUID().toString(),
        entityType = "pet",
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
      petDao.upsert(entity)
      outboxDao.insert(outbox)
      auditRepository.record(
        entity = "pet",
        entityId = entity.id,
        action = action,
        payload = payload,
        clinicId = entity.clinicId,
      )
    }
  }

  suspend fun findById(id: String): PetEntity? = petDao.findById(id)

  suspend fun hasPendingChanges(petId: String): Boolean = outboxDao.countForEntity(petId) > 0

  suspend fun upsertFromRemote(dto: PetDto, clinicId: String) {
    val existing = petDao.findById(dto.id)
    if (existing?.pendingSync == true) return
    petDao.upsert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun markSynced(dto: PetDto, clinicId: String) {
    val existing = petDao.findById(dto.id)
    val entity = dto.toEntity(clinicId, existing?.originDeviceId ?: settings.deviceId)
    petDao.upsert(entity.copy(pendingSync = false))
  }
}
