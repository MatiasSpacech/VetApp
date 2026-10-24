package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "vaccination_notifications",
  indices = [
    Index(value = ["vaccinationId", "dueOn", "milestone"], unique = true),
    Index("notifiedAt"),
  ],
)
data class VaccinationNotificationEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val vaccinationId: String,
  val petId: String,
  val dueOn: Long,
  val milestone: String,
  val notifiedAt: Long,
)
