package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "pet_owner_history",
  indices = [Index("clinicId"), Index("petId"), Index("changedAt")],
)
data class PetOwnerHistoryEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val petId: String,
  val previousClientId: String?,
  val previousOwnerName: String,
  val newClientId: String,
  val newOwnerName: String,
  val reason: String?,
  val actorId: String?,
  val changedAt: Long,
)
