package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "consultation_addenda",
  indices = [Index("clinicId"), Index("consultationId"), Index("createdAt"), Index("pendingSync")],
)
data class ConsultationAddendumEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val consultationId: String,
  val reason: String,
  val text: String,
  val authorId: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val deletedAt: Long?,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
