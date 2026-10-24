package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "sale_cancellations",
  indices = [Index("clinicId"), Index("saleId"), Index("pendingSync")],
)
data class SaleCancellationEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val saleId: String,
  val reason: String,
  val cancelledAt: Long,
  val createdAt: Long,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
