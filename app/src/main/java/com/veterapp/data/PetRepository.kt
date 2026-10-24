package com.veterapp.data

import androidx.room.withTransaction
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.PetEntity
import com.veterapp.data.local.PetOwnerHistoryEntity
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.PetDto
import com.veterapp.data.remote.toDto
import com.veterapp.data.remote.toEntity
import com.veterapp.domain.Pet
import com.veterapp.domain.PetDraft
import com.veterapp.domain.PetOwnerHistory
import com.veterapp.domain.PetStatus
import com.veterapp.domain.toDomain
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
  private val clientDao = database.clientDao()
  private val outboxDao = database.outboxDao()
  private val ownerHistoryDao = database.ownerHistoryDao()
  private val json = Json { encodeDefaults = true }

  val pets: Flow<List<Pet>> =
    combine(petDao.observeActive(), clientDao.observeActive()) { rows, clients ->
      val names = clients.associate { it.id to it.name }
      rows.map { it.toDomain(names) }
    }

  val pendingCount: Flow<Int> = database.outboxDao().observeCount()

  fun observePet(petId: String): Flow<Pet?> =
    combine(petDao.observeById(petId), clientDao.observeActive()) { row, clients ->
      row?.toDomain(clients.associate { it.id to it.name })
    }

  fun observePetsForClient(clientId: String): Flow<List<Pet>> =
    combine(petDao.observeForClient(clientId), clientDao.observeActive()) { rows, clients ->
      val names = clients.associate { it.id to it.name }
      rows.map { it.toDomain(names) }
    }

  fun observeOwnerHistory(petId: String): Flow<List<PetOwnerHistory>> =
    ownerHistoryDao.observeForPet(petId).map { rows ->
      rows.map(PetOwnerHistoryEntity::toDomain)
    }

  suspend fun createPet(
    clientId: String,
    petId: String,
    draft: PetDraft,
    fotoUrl: String?,
  ): Pet {
    val client = clientDao.findById(clientId) ?: error("Elegí un cliente válido")
    require(client.deletedAt == null) { "El cliente seleccionado está archivado" }
    val now = System.currentTimeMillis()
    val entity =
      PetEntity(
        id = petId,
        clinicId = client.clinicId,
        name = draft.name.trim(),
        species = draft.species.trim(),
        breed = draft.breed?.trim()?.takeIf { it.isNotBlank() },
        ownerName = client.name,
        notes = draft.notes?.trim()?.takeIf { it.isNotBlank() },
        fotoUrl = fotoUrl,
        createdAt = now,
        updatedAt = now,
        revision = 0L,
        deletedAt = null,
        originDeviceId = settings.deviceId,
        pendingSync = true,
        clientId = client.id,
        status = draft.status.storage,
      )
    enqueueUpsert(
      entity = entity,
      baseRevision = null,
      action = "pet.create",
      payload =
        buildJsonObject {
          put("name", entity.name)
          put("owner_name", entity.ownerName)
          put("client_id", entity.clientId)
          put("status", entity.status)
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
    val ownerName = existing.clientId?.let { clientDao.findById(it)?.name } ?: existing.ownerName
    val now = System.currentTimeMillis()
    val updated =
      existing.copy(
        name = draft.name.trim(),
        species = draft.species.trim(),
        breed = draft.breed?.trim()?.takeIf { it.isNotBlank() },
        ownerName = ownerName,
        notes = draft.notes?.trim()?.takeIf { it.isNotBlank() },
        fotoUrl =
          when {
            removePhoto -> null
            fotoUrl != null -> fotoUrl
            else -> existing.fotoUrl
          },
        status = draft.status.storage,
        updatedAt = now,
        pendingSync = true,
      )
    val changed =
      buildList {
        if (existing.name != updated.name) add("nombre")
        if (existing.species != updated.species) add("especie")
        if (existing.breed != updated.breed) add("raza")
        if (existing.notes != updated.notes) add("notas")
        if (existing.fotoUrl != updated.fotoUrl) add("foto")
        if (existing.status != updated.status) add("estado")
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

  suspend fun changeStatus(petId: String, status: PetStatus) {
    val existing = petDao.findById(petId) ?: error("La mascota ya no existe")
    if (existing.status == status.storage) return
    val updated =
      existing.copy(
        status = status.storage,
        updatedAt = System.currentTimeMillis(),
        pendingSync = true,
      )
    enqueueUpsert(
      entity = updated,
      baseRevision = existing.revision,
      action = "pet.status",
      payload =
        buildJsonObject {
          put("name", updated.name)
          put("from", existing.status)
          put("to", status.storage)
        },
    )
  }

  suspend fun changeOwner(petId: String, newClientId: String, reason: String?) {
    val existing = petDao.findById(petId) ?: error("La mascota ya no existe")
    if (existing.clientId == newClientId) return
    val newClient = clientDao.findById(newClientId) ?: error("El nuevo cliente no existe")
    require(newClient.deletedAt == null) { "El nuevo cliente está archivado" }
    require(newClient.clinicId == existing.clinicId) { "El cliente pertenece a otra clínica" }
    val previousClient = existing.clientId?.let { clientDao.findById(it) }
    val previousOwnerName = previousClient?.name ?: existing.ownerName
    val now = System.currentTimeMillis()

    val updated =
      existing.copy(
        clientId = newClient.id,
        ownerName = newClient.name,
        updatedAt = now,
        pendingSync = true,
      )
    val history =
      PetOwnerHistoryEntity(
        id = UUID.randomUUID().toString(),
        clinicId = existing.clinicId,
        petId = existing.id,
        previousClientId = existing.clientId,
        previousOwnerName = previousOwnerName,
        newClientId = newClient.id,
        newOwnerName = newClient.name,
        reason = reason?.trim()?.takeIf { it.isNotBlank() },
        actorId = settings.userId,
        changedAt = now,
      )
    val outbox =
      OutboxEntity(
        operationId = UUID.randomUUID().toString(),
        entityType = "pet",
        entityId = updated.id,
        operation = "UPSERT",
        payload = json.encodeToString(updated.toDto()),
        baseRevision = existing.revision,
        attempts = 0,
        nextAttemptAt = 0L,
        lastError = null,
        createdAt = now,
      )

    database.withTransaction {
      petDao.upsert(updated)
      ownerHistoryDao.insert(history)
      outboxDao.insert(outbox)
      auditRepository.record(
        entity = "pet",
        entityId = updated.id,
        action = "pet.owner.change",
        payload =
          buildJsonObject {
            put("name", updated.name)
            put("previous_owner", previousOwnerName)
            put("new_owner", newClient.name)
            put("reason", history.reason)
          },
        clinicId = updated.clinicId,
      )
    }
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

  private fun PetEntity.toDomain(clients: Map<String, String>): Pet =
    toDomain().copy(ownerName = clientId?.let { clients[it] } ?: ownerName)
}
