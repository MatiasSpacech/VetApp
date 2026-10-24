package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "pet_weights",
  indices = [Index("clinicId"), Index("petId"), Index("measuredAt"), Index("pendingSync")],
)
data class PetWeightEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val petId: String,
  val measuredAt: Long,
  val weightGrams: Int,
  val notes: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val deletedAt: Long?,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
