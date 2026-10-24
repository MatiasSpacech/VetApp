package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "sync_outbox",
  indices = [Index("entityId"), Index("createdAt"), Index("nextAttemptAt")],
)
data class OutboxEntity(
  @PrimaryKey val operationId: String,
  val entityType: String,
  val entityId: String,
  val operation: String,
  val payload: String,
  val baseRevision: Long?,
  val attempts: Int,
  val nextAttemptAt: Long,
  val lastError: String?,
  val createdAt: Long,
)
