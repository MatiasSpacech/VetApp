package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "cash_movements",
  indices = [
    Index("clinicId"),
    Index("sessionId"),
    Index("createdAt"),
    Index("pendingSync"),
  ],
)
data class CashMovementEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val sessionId: String,
  val type: String,
  val amountCents: Long,
  val reason: String?,
  val saleId: String?,
  val paymentId: String?,
  val saleCancellationId: String?,
  val createdAt: Long,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
