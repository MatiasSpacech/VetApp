package com.veterapp.domain

import com.veterapp.data.local.AuditEntity

data class AuditEvent(
  val id: String,
  val actorId: String?,
  val entity: String,
  val entityId: String?,
  val action: String,
  val payload: String?,
  val createdAt: Long,
)

fun AuditEntity.toDomain(): AuditEvent =
  AuditEvent(
    id = id,
    actorId = actorId,
    entity = entity,
    entityId = entityId,
    action = action,
    payload = payload,
    createdAt = createdAt,
  )
