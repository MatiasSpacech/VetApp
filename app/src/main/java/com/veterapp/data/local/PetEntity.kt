package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "pets",
  indices = [Index("clinicId"), Index("updatedAt"), Index("pendingSync")],
)
data class PetEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val name: String,
  val species: String,
  val breed: String?,
  val ownerName: String,
  val notes: String?,
  val fotoUrl: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val deletedAt: Long?,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
