package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "audit_log",
  indices = [Index("createdAt"), Index(value = ["entity", "entityId"])],
)
data class AuditEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val actorId: String?,
  val entity: String,
  val entityId: String?,
  val action: String,
  val payload: String?,
  val createdAt: Long,
)
