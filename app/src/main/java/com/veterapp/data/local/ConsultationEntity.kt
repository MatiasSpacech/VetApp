package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "consultations",
  indices = [Index("clinicId"), Index("petId"), Index("consultedAt"), Index("pendingSync")],
)
data class ConsultationEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val petId: String,
  val consultedAt: Long,
  val reason: String,
  val exam: String?,
  val diagnosis: String?,
  val treatment: String?,
  val medication: String?,
  val instructions: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val deletedAt: Long?,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
