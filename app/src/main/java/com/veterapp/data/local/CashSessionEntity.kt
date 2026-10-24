package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "cash_sessions",
  indices = [Index("clinicId"), Index("status"), Index("pendingSync")],
)
data class CashSessionEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val openedAt: Long,
  val closedAt: Long?,
  val openingCents: Long,
  val expectedCashCents: Long?,
  val countedCashCents: Long?,
  val differenceCents: Long?,
  val status: String,
  val notes: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
