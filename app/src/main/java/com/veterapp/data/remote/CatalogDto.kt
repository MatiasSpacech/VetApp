package com.veterapp.data.remote

import com.veterapp.data.local.AreaEntity
import com.veterapp.data.local.CatalogItemEntity
import com.veterapp.data.local.CategoryEntity
import com.veterapp.data.local.StockMovementEntity
import com.veterapp.domain.CatalogKind
import com.veterapp.domain.CategoryScope
import com.veterapp.domain.StockMovementType
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AreaDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  val name: String,
  val active: Boolean? = null,
  @SerialName("sort_order") val sortOrder: Int? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class CategoryDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  val name: String,
  @SerialName("parent_id") val parentId: String? = null,
  val scope: String? = null,
  val active: Boolean? = null,
  @SerialName("sort_order") val sortOrder: Int? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class CatalogItemDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  val kind: String,
  @SerialName("internal_code") val internalCode: String,
  val barcode: String? = null,
  val name: String,
  val description: String? = null,
  @SerialName("category_id") val categoryId: String? = null,
  val brand: String? = null,
  @SerialName("unit_cost_cents") val unitCostCents: Long? = null,
  @SerialName("sale_price_cents") val salePriceCents: Long,
  @SerialName("track_stock") val trackStock: Boolean? = null,
  @SerialName("stock_cached") val stockCached: Int? = null,
  @SerialName("stock_min") val stockMin: Int? = null,
  @SerialName("area_id") val areaId: String,
  val lot: String? = null,
  @SerialName("expires_on") val expiresOn: String? = null,
  @SerialName("duration_minutes") val durationMinutes: Int? = null,
  val active: Boolean? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class StockMovementDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  @SerialName("item_id") val itemId: String,
  val type: String,
  @SerialName("quantity_delta") val quantityDelta: Int,
  @SerialName("unit_cost_cents") val unitCostCents: Long? = null,
  val reason: String? = null,
  @SerialName("sale_id") val saleId: String? = null,
  @SerialName("sale_item_id") val saleItemId: String? = null,
  @SerialName("sale_cancellation_id") val saleCancellationId: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

fun AreaEntity.toDto(): AreaDto =
  AreaDto(
    id = id,
    clinicId = clinicId,
    name = name,
    active = active,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
    originDeviceId = originDeviceId,
  )

fun AreaDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): AreaEntity =
  AreaEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    name = name,
    active = active ?: true,
    sortOrder = sortOrder ?: 0,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    deletedAt = deletedAt?.toEpochMillisOrNull(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

fun CategoryEntity.toDto(): CategoryDto =
  CategoryDto(
    id = id,
    clinicId = clinicId,
    name = name,
    parentId = parentId,
    scope = scope,
    active = active,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
    originDeviceId = originDeviceId,
  )

fun CategoryDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): CategoryEntity =
  CategoryEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    name = name,
    parentId = parentId,
    scope = CategoryScope.fromStorage(scope).storage,
    active = active ?: true,
    sortOrder = sortOrder ?: 0,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    deletedAt = deletedAt?.toEpochMillisOrNull(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

fun CatalogItemEntity.toDto(): CatalogItemDto =
  CatalogItemDto(
    id = id,
    clinicId = clinicId,
    kind = kind,
    internalCode = internalCode,
    barcode = barcode,
    name = name,
    description = description,
    categoryId = categoryId,
    brand = brand,
    unitCostCents = unitCostCents,
    salePriceCents = salePriceCents,
    trackStock = trackStock,
    stockCached = stockCached,
    stockMin = stockMin,
    areaId = areaId,
    lot = lot,
    expiresOn = expiresOn,
    durationMinutes = durationMinutes,
    active = active,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
    originDeviceId = originDeviceId,
  )

fun CatalogItemDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): CatalogItemEntity {
  val kind = CatalogKind.fromStorage(kind)
  return CatalogItemEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    kind = kind.storage,
    internalCode = internalCode,
    barcode = barcode,
    name = name,
    description = description,
    categoryId = categoryId,
    brand = brand,
    unitCostCents = unitCostCents,
    salePriceCents = salePriceCents,
    trackStock = trackStock ?: (kind == CatalogKind.Product),
    stockCached = stockCached ?: 0,
    stockMin = stockMin ?: 0,
    areaId = areaId,
    lot = lot,
    expiresOn = expiresOn,
    durationMinutes = durationMinutes,
    active = active ?: true,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    deletedAt = deletedAt?.toEpochMillisOrNull(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )
}

fun StockMovementEntity.toDto(): StockMovementDto =
  StockMovementDto(
    id = id,
    clinicId = clinicId,
    itemId = itemId,
    type = type,
    quantityDelta = quantityDelta,
    unitCostCents = unitCostCents,
    reason = reason,
    saleId = saleId,
    saleItemId = saleItemId,
    saleCancellationId = saleCancellationId,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    originDeviceId = originDeviceId,
  )

fun StockMovementDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): StockMovementEntity =
  StockMovementEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    itemId = itemId,
    type = StockMovementType.fromStorage(type).storage,
    quantityDelta = quantityDelta,
    unitCostCents = unitCostCents,
    reason = reason,
    saleId = saleId,
    saleItemId = saleItemId,
    saleCancellationId = saleCancellationId,
    createdAt = createdAt.toEpochMillisOrNow(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )
