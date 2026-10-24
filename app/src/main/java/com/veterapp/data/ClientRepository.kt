package com.veterapp.data

import androidx.room.withTransaction
import com.veterapp.data.local.ClientEntity
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.ClientDto
import com.veterapp.data.remote.toDto
import com.veterapp.data.remote.toEntity
import com.veterapp.domain.Client
import com.veterapp.domain.ClientDraft
import com.veterapp.domain.ClientSaveResult
import com.veterapp.domain.matches
import com.veterapp.domain.toDomain
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ClientRepository(
  private val database: VetDatabase,
  private val settings: AppSettings,
  private val auditRepository: AuditRepository = AuditRepository(database, settings),
) {
  private val clientDao = database.clientDao()
  private val petDao = database.petDao()
  private val outboxDao = database.outboxDao()
  private val json = Json { encodeDefaults = true }

  val clients: Flow<List<Client>> =
    clientDao.observeActive().map { rows -> rows.map(ClientEntity::toDomain) }

  fun observeById(id: String): Flow<Client?> =
    clientDao.observeById(id).map { it?.toDomain() }

  suspend fun findById(id: String): ClientEntity? = clientDao.findById(id)

  suspend fun create(draft: ClientDraft, force: Boolean = false): ClientSaveResult {
    validate(draft)?.let { return ClientSaveResult.Failure(it) }
    val duplicates = findDuplicates(draft, excludeId = null)
    if (duplicates.isNotEmpty() && !force) return ClientSaveResult.Duplicate(duplicates)

    val now = System.currentTimeMillis()
    val entity =
      ClientEntity(
        id = UUID.randomUUID().toString(),
        clinicId = settings.currentClinicId,
        name = draft.name.trim(),
        phone = draft.phone.clean(),
        document = draft.document.clean(),
        email = draft.email.clean(),
        address = draft.address.clean(),
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
      action = "client.create",
      payload =
        buildJsonObject {
          put("name", entity.name)
          put("phone", entity.phone)
          put("document", entity.document)
        },
    )
    return ClientSaveResult.Success(entity.toDomain())
  }

  suspend fun update(
    clientId: String,
    draft: ClientDraft,
    force: Boolean = false,
  ): ClientSaveResult {
    val existing = clientDao.findById(clientId)
      ?: return ClientSaveResult.Failure("El cliente ya no existe")
    validate(draft)?.let { return ClientSaveResult.Failure(it) }
    val duplicates = findDuplicates(draft, excludeId = clientId)
    if (duplicates.isNotEmpty() && !force) return ClientSaveResult.Duplicate(duplicates)

    val updated =
      existing.copy(
        name = draft.name.trim(),
        phone = draft.phone.clean(),
        document = draft.document.clean(),
        email = draft.email.clean(),
        address = draft.address.clean(),
        notes = draft.notes.clean(),
        updatedAt = System.currentTimeMillis(),
        pendingSync = true,
      )
    val changed =
      buildList {
        if (existing.name != updated.name) add("nombre")
        if (existing.phone != updated.phone) add("teléfono")
        if (existing.document != updated.document) add("documento")
        if (existing.email != updated.email) add("email")
        if (existing.address != updated.address) add("dirección")
        if (existing.notes != updated.notes) add("notas")
      }
    enqueueUpsert(
      entity = updated,
      baseRevision = existing.revision,
      action = "client.update",
      payload =
        buildJsonObject {
          put("name", updated.name)
          put("changed", changed.joinToString(", "))
        },
    )
    return ClientSaveResult.Success(updated.toDomain())
  }

  suspend fun archive(clientId: String): Result<Unit> {
    val existing = clientDao.findById(clientId)
      ?: return Result.failure(IllegalArgumentException("El cliente ya no existe"))
    if (existing.deletedAt != null) return Result.success(Unit)

    val petCount = petDao.countActiveForClient(clientId)
    if (petCount > 0) {
      return Result.failure(
        IllegalArgumentException(
          "El cliente tiene $petCount mascota(s) registrada(s). Cambiá su propietario antes de archivarlo.",
        ),
      )
    }

    val updated =
      existing.copy(
        deletedAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        pendingSync = true,
      )
    enqueueUpsert(
      entity = updated,
      baseRevision = existing.revision,
      action = "client.delete",
      payload = buildJsonObject { put("name", updated.name) },
    )
    return Result.success(Unit)
  }

  suspend fun findDuplicates(draft: ClientDraft, excludeId: String?): List<Client> =
    clientDao.active()
      .map(ClientEntity::toDomain)
      .filter { it.id != excludeId && it.matches(draft) }

  suspend fun upsertFromRemote(dto: ClientDto, clinicId: String) {
    val existing = clientDao.findById(dto.id)
    if (existing?.pendingSync == true) return
    clientDao.upsert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun markSynced(dto: ClientDto, clinicId: String) {
    val existing = clientDao.findById(dto.id)
    val entity = dto.toEntity(clinicId, existing?.originDeviceId ?: settings.deviceId)
    clientDao.upsert(entity.copy(pendingSync = false))
  }

  private suspend fun enqueueUpsert(
    entity: ClientEntity,
    baseRevision: Long?,
    action: String,
    payload: JsonObject,
  ) {
    val now = System.currentTimeMillis()
    val outbox =
      OutboxEntity(
        operationId = UUID.randomUUID().toString(),
        entityType = "client",
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
      clientDao.upsert(entity)
      outboxDao.insert(outbox)
      auditRepository.record(
        entity = "client",
        entityId = entity.id,
        action = action,
        payload = payload,
        clinicId = entity.clinicId,
      )
    }
  }

  private fun validate(draft: ClientDraft): String? {
    if (draft.name.isBlank()) return "Completá el nombre del cliente"
    val email = draft.email?.trim()
    if (!email.isNullOrBlank() && !email.contains("@")) return "El email no parece válido"
    return null
  }

  private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotBlank() }
}
