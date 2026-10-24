package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "stock_movements",
  indices = [Index("clinicId"), Index("itemId"), Index("createdAt"), Index("pendingSync")],
)
data class StockMovementEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val itemId: String,
  val type: String,
  val quantityDelta: Int,
  val unitCostCents: Long?,
  val reason: String?,
  val saleId: String?,
  val saleItemId: String?,
  val saleCancellationId: String?,
  val createdAt: Long,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
