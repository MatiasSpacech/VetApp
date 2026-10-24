package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "sale_items",
  indices = [
    Index("clinicId"),
    Index("saleId"),
    Index("itemId"),
    Index("areaId"),
    Index("pendingSync"),
  ],
)
data class SaleItemEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val saleId: String,
  val itemId: String,
  val itemKind: String,
  val itemName: String,
  val areaId: String,
  val areaName: String,
  val quantity: Int,
  val unitPriceCents: Long,
  val unitCostCents: Long?,
  val discountCents: Long,
  val grossCents: Long,
  val netCents: Long,
  val expensesPercentBp: Int,
  val lot: String?,
  val expiresOn: String?,
  val createdAt: Long,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
