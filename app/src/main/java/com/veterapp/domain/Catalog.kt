package com.veterapp.domain

import com.veterapp.data.local.AreaEntity
import com.veterapp.data.local.CatalogItemEntity
import com.veterapp.data.local.CategoryEntity
import com.veterapp.data.local.StockMovementEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CatalogKind(val storage: String, val label: String) {
  Product("product", "Producto"),
  Service("service", "Servicio"),
  ;

  companion object {
    fun fromStorage(value: String?): CatalogKind =
      entries.firstOrNull { it.storage == value } ?: Product
  }
}

enum class CategoryScope(val storage: String, val label: String) {
  Product("product", "Productos"),
  Service("service", "Servicios"),
  Both("both", "Ambos"),
  ;

  companion object {
    fun fromStorage(value: String?): CategoryScope =
      entries.firstOrNull { it.storage == value } ?: Both
  }

  fun accepts(kind: CatalogKind): Boolean =
    this == Both || (this == Product && kind == CatalogKind.Product) ||
      (this == Service && kind == CatalogKind.Service)
}

enum class StockMovementType(val storage: String, val label: String) {
  Entry("entry", "Entrada"),
  Adjustment("adjustment", "Ajuste"),
  Loss("loss", "Pérdida"),
  Sale("sale", "Venta"),
  Cancellation("cancellation", "Anulación"),
  ;

  companion object {
    fun fromStorage(value: String?): StockMovementType =
      entries.firstOrNull { it.storage == value } ?: Adjustment
  }
}

enum class ExpiryStatus(val label: String) {
  Expired("Vencido"),
  ExpiringSoon("Próximo a vencer"),
  Ok("Vigente"),
}

data class Area(
  val id: String,
  val clinicId: String,
  val name: String,
  val active: Boolean,
  val sortOrder: Int,
  val pendingSync: Boolean,
)

data class Category(
  val id: String,
  val clinicId: String,
  val name: String,
  val parentId: String?,
  val scope: CategoryScope,
  val active: Boolean,
  val sortOrder: Int,
  val pendingSync: Boolean,
)

data class CatalogItem(
  val id: String,
  val clinicId: String,
  val kind: CatalogKind,
  val internalCode: String,
  val barcode: String?,
  val name: String,
  val description: String?,
  val categoryId: String?,
  val categoryName: String?,
  val brand: String?,
  val unitCostCents: Long?,
  val salePriceCents: Long,
  val trackStock: Boolean,
  val stockCached: Int,
  val stockMin: Int,
  val areaId: String,
  val areaName: String,
  val lot: String?,
  val expiresOn: String?,
  val durationMinutes: Int?,
  val active: Boolean,
  val pendingSync: Boolean,
) {
  val isLowStock: Boolean
    get() = trackStock && active && stockCached <= stockMin

  fun expiryStatus(today: LocalDate = ClinicTime.today()): ExpiryStatus? {
    val date = expiresOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    return when {
      date.isBefore(today) -> ExpiryStatus.Expired
      !date.isAfter(today.plusDays(30)) -> ExpiryStatus.ExpiringSoon
      else -> ExpiryStatus.Ok
    }
  }
}

data class StockMovement(
  val id: String,
  val clinicId: String,
  val itemId: String,
  val type: StockMovementType,
  val quantityDelta: Int,
  val unitCostCents: Long?,
  val reason: String?,
  val saleId: String?,
  val saleItemId: String?,
  val saleCancellationId: String?,
  val createdAt: Long,
  val pendingSync: Boolean,
)

data class CatalogItemDraft(
  val kind: CatalogKind,
  val internalCode: String,
  val barcode: String?,
  val name: String,
  val description: String?,
  val categoryId: String?,
  val brand: String?,
  val unitCostCents: Long?,
  val salePriceCents: Long,
  val stockMin: Int,
  val areaId: String,
  val lot: String?,
  val expiresOn: String?,
  val durationMinutes: Int?,
  val active: Boolean = true,
  val initialStock: Int = 0,
) {
  fun validate(): String? {
    if (name.isBlank()) return "Completá el nombre del ítem"
    if (areaId.isBlank()) return "Elegí un área"
    if (salePriceCents < 0) return "El precio no puede ser negativo"
    if (unitCostCents != null && unitCostCents < 0) return "El costo no puede ser negativo"
    if (kind == CatalogKind.Product) {
      if (unitCostCents == null) return "El costo es obligatorio en productos"
      if (stockMin < 0) return "El stock mínimo no puede ser negativo"
      if (initialStock < 0) return "El stock inicial no puede ser negativo"
    } else {
      if (durationMinutes != null && durationMinutes <= 0) {
        return "La duración debe ser mayor que cero"
      }
    }
    if (expiresOn != null) {
      runCatching { LocalDate.parse(expiresOn) }.getOrNull()
        ?: return "La fecha de vencimiento no es válida"
    }
    if (kind == CatalogKind.Service && (lot != null || expiresOn != null)) {
      return "Un servicio no puede tener lote ni vencimiento"
    }
    return null
  }
}

data class StockMovementDraft(
  val type: StockMovementType,
  val quantity: Int,
  val unitCostCents: Long?,
  val reason: String?,
) {
  fun validate(trackStock: Boolean): String? {
    if (!trackStock) return "Este ítem no maneja stock"
    if (type !in setOf(StockMovementType.Entry, StockMovementType.Adjustment, StockMovementType.Loss)) {
      return "El tipo de movimiento no es válido para el inventario"
    }
    if (quantity == 0) return "La cantidad no puede ser cero"
    if (type == StockMovementType.Loss && quantity < 0) return "La pérdida debe ser positiva"
    if (type == StockMovementType.Adjustment && reason.isNullOrBlank()) {
      return "El ajuste necesita un motivo"
    }
    if (type == StockMovementType.Loss && reason.isNullOrBlank()) {
      return "La pérdida necesita un motivo"
    }
    if (unitCostCents != null && unitCostCents < 0) return "El costo no puede ser negativo"
    return null
  }

  fun delta(): Int =
    when (type) {
      StockMovementType.Entry -> quantity
      StockMovementType.Loss -> -kotlin.math.abs(quantity)
      StockMovementType.Adjustment -> quantity
      else -> 0
    }
}

fun CatalogItemEntity.toDomain(areaName: String, categoryName: String?): CatalogItem =
  CatalogItem(
    id = id,
    clinicId = clinicId,
    kind = CatalogKind.fromStorage(kind),
    internalCode = internalCode,
    barcode = barcode,
    name = name,
    description = description,
    categoryId = categoryId,
    categoryName = categoryName,
    brand = brand,
    unitCostCents = unitCostCents,
    salePriceCents = salePriceCents,
    trackStock = trackStock,
    stockCached = stockCached,
    stockMin = stockMin,
    areaId = areaId,
    areaName = areaName,
    lot = lot,
    expiresOn = expiresOn,
    durationMinutes = durationMinutes,
    active = active,
    pendingSync = pendingSync,
  )

fun AreaEntity.toDomain(): Area =
  Area(
    id = id,
    clinicId = clinicId,
    name = name,
    active = active,
    sortOrder = sortOrder,
    pendingSync = pendingSync,
  )

fun CategoryEntity.toDomain(): Category =
  Category(
    id = id,
    clinicId = clinicId,
    name = name,
    parentId = parentId,
    scope = CategoryScope.fromStorage(scope),
    active = active,
    sortOrder = sortOrder,
    pendingSync = pendingSync,
  )

fun StockMovementEntity.toDomain(): StockMovement =
  StockMovement(
    id = id,
    clinicId = clinicId,
    itemId = itemId,
    type = StockMovementType.fromStorage(type),
    quantityDelta = quantityDelta,
    unitCostCents = unitCostCents,
    reason = reason,
    saleId = saleId,
    saleItemId = saleItemId,
    saleCancellationId = saleCancellationId,
    createdAt = createdAt,
    pendingSync = pendingSync,
  )

fun normalizeBarcode(raw: String?): String? =
  raw?.filter { it.isLetterOrDigit() }?.takeIf { it.isNotBlank() }

fun daysUntilExpiry(expiresOn: String?, today: LocalDate = ClinicTime.today()): Long? {
  val date = expiresOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
  return ChronoUnit.DAYS.between(today, date)
}
