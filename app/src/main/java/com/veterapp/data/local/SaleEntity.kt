package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "sales",
  indices = [
    Index("clinicId"),
    Index("clientId"),
    Index("soldAt"),
    Index("status"),
    Index("pendingSync"),
    Index(value = ["clinicId", "number"], unique = true),
  ],
)
data class SaleEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val number: String,
  val clientId: String?,
  val clientName: String?,
  val status: String,
  val subtotalCents: Long,
  val discountCents: Long,
  val totalCents: Long,
  val notes: String?,
  val soldAt: Long,
  val cancelledAt: Long?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
