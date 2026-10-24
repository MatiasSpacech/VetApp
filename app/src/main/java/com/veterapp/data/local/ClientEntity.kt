package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "clients",
  indices = [Index("clinicId"), Index("name"), Index("updatedAt"), Index("pendingSync")],
)
data class ClientEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val name: String,
  val phone: String?,
  val document: String?,
  val email: String?,
  val address: String?,
  val notes: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val deletedAt: Long?,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
