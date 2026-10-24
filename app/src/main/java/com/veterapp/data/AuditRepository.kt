package com.veterapp.data

import com.veterapp.data.local.AuditEntity
import com.veterapp.data.local.VetDatabase
import com.veterapp.domain.AuditEvent
import com.veterapp.domain.toDomain
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject

class AuditRepository(
  private val database: VetDatabase,
  private val settings: AppSettings,
) {
  private val auditDao = database.auditDao()

  val recent: Flow<List<AuditEvent>> =
    auditDao.observeRecent(RECENT_LIMIT).map { rows -> rows.map(AuditEntity::toDomain) }

  suspend fun record(
    entity: String,
    entityId: String?,
    action: String,
    payload: JsonObject? = null,
    clinicId: String? = null,
  ) {
    auditDao.insert(
      AuditEntity(
        id = UUID.randomUUID().toString(),
        clinicId = clinicId ?: settings.clinicId ?: settings.localClinicId,
        actorId = settings.userId,
        entity = entity,
        entityId = entityId,
        action = action,
        payload = payload?.toString(),
        createdAt = System.currentTimeMillis(),
      ),
    )
  }

  suspend fun all(): List<AuditEntity> = auditDao.all()

  private companion object {
    const val RECENT_LIMIT = 100
  }
}
