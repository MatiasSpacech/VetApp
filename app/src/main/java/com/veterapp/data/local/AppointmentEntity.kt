package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "appointments",
  indices = [
    Index("clinicId"),
    Index("petId"),
    Index("startsAt"),
    Index("status"),
    Index("pendingSync"),
  ],
)
data class AppointmentEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val petId: String,
  val startsAt: Long,
  val endsAt: Long,
  val status: String,
  val reason: String,
  val notes: String?,
  val consultationId: String?,
  val cancelReason: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val deletedAt: Long?,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
