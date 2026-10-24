package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "vaccinations",
  indices = [
    Index("clinicId"),
    Index("petId"),
    Index("appliedOn"),
    Index("nextDueOn"),
    Index("pendingSync"),
  ],
)
data class VaccinationEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val petId: String,
  val vaccineName: String,
  val appliedOn: Long,
  val nextDueOn: Long?,
  val lot: String?,
  val notes: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val deletedAt: Long?,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
