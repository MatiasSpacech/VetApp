package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "catalog_items",
  indices = [
    Index("clinicId"),
    Index("kind"),
    Index("name"),
    Index("categoryId"),
    Index("areaId"),
    Index("internalCode"),
    Index("barcode"),
    Index("active"),
    Index("pendingSync"),
    Index(value = ["clinicId", "internalCode"], unique = true),
    Index(value = ["clinicId", "barcode"], unique = true),
  ],
)
data class CatalogItemEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val kind: String,
  val internalCode: String,
  val barcode: String?,
  val name: String,
  val description: String?,
  val categoryId: String?,
  val brand: String?,
  val unitCostCents: Long?,
  val salePriceCents: Long,
  val trackStock: Boolean,
  val stockCached: Int,
  val stockMin: Int,
  val areaId: String,
  val lot: String?,
  val expiresOn: String?,
  val durationMinutes: Int?,
  val active: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val deletedAt: Long?,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
