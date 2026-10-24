package com.veterapp.domain

import com.veterapp.data.local.AreaCostSettingEntity
import com.veterapp.data.local.CashMovementEntity
import com.veterapp.data.local.CashSessionEntity
import com.veterapp.data.local.PaymentMethodEntity
import com.veterapp.data.local.SaleEntity
import com.veterapp.data.local.SaleItemEntity
import com.veterapp.data.local.SaleWithClient
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

enum class SaleStatus(val storage: String, val label: String) {
  Completed("completed", "Completada"),
  Cancelled("cancelled", "Anulada"),
  ;

  companion object {
    fun fromStorage(value: String?): SaleStatus =
      entries.firstOrNull { it.storage == value } ?: Completed
  }
}

enum class CashMovementType(val storage: String, val label: String) {
  Sale("sale", "Venta"),
  Cancellation("cancellation", "Anulación"),
  Withdrawal("withdrawal", "Retiro"),
  Expense("expense", "Gasto"),
  Adjustment("adjustment", "Ajuste"),
  ;

  companion object {
    fun fromStorage(value: String?): CashMovementType =
      entries.firstOrNull { it.storage == value } ?: Adjustment
  }
}

enum class CashSessionStatus(val storage: String, val label: String) {
  Open("open", "Abierta"),
  Closed("closed", "Cerrada"),
  ;

  companion object {
    fun fromStorage(value: String?): CashSessionStatus =
      entries.firstOrNull { it.storage == value } ?: Closed
  }
}

data class PaymentMethod(
  val id: String,
  val clinicId: String,
  val name: String,
  val affectsCash: Boolean,
  val active: Boolean,
  val sortOrder: Int,
  val pendingSync: Boolean,
)

data class SaleLineDraft(
  val itemId: String,
  val quantity: Int,
  val discountCents: Long = 0,
)

data class PaymentDraft(
  val methodId: String,
  val amountCents: Long,
  val tenderedCents: Long? = null,
)

data class SaleDraft(
  val clientId: String?,
  val lines: List<SaleLineDraft>,
  val globalDiscountCents: Long,
  val payments: List<PaymentDraft>,
  val notes: String?,
) {
  fun validate(): String? {
    if (lines.isEmpty()) return "Agregá al menos un ítem"
    if (lines.any { it.quantity <= 0 }) return "La cantidad debe ser mayor que cero"
    if (lines.any { it.discountCents < 0 }) return "El descuento no puede ser negativo"
    if (globalDiscountCents < 0) return "El descuento global no puede ser negativo"
    if (payments.any { it.amountCents < 0 }) return "El importe de un pago no puede ser negativo"
    return null
  }
}

data class SaleLineComputed(
  val item: CatalogItem,
  val quantity: Int,
  val lineDiscountCents: Long,
  val globalDiscountCents: Long,
  val expensesPercentBp: Int,
) {
  val grossCents: Long
    get() = item.salePriceCents * quantity

  val discountCents: Long
    get() = lineDiscountCents + globalDiscountCents

  val netCents: Long
    get() = grossCents - discountCents
}

data class SaleTotals(
  val lines: List<SaleLineComputed>,
  val subtotalCents: Long,
  val discountCents: Long,
  val totalCents: Long,
)

fun calculateSaleTotals(
  lines: List<Pair<CatalogItem, SaleLineDraft>>,
  globalDiscountCents: Long,
  expensesByArea: Map<String, Int>,
): SaleTotals {
  val grosses = lines.map { (item, draft) -> item.salePriceCents * draft.quantity }
  val subtotal = grosses.sum()
  val lineDiscountsTotal = lines.sumOf { it.second.discountCents }
  val available = (subtotal - lineDiscountsTotal).coerceAtLeast(0)
  val global = globalDiscountCents.coerceIn(0, available)
  val bases = lines.mapIndexed { index, (_, draft) ->
    (grosses[index] - draft.discountCents).coerceAtLeast(0)
  }
  val baseTotal = bases.sum()
  var assigned = 0L
  val computed =
    lines.mapIndexed { index, (item, draft) ->
      val share =
        when {
          baseTotal <= 0 -> 0L
          index == lines.lastIndex -> global - assigned
          else -> global * bases[index] / baseTotal
        }
      assigned += share
      SaleLineComputed(
        item = item,
        quantity = draft.quantity,
        lineDiscountCents = draft.discountCents,
        globalDiscountCents = share,
        expensesPercentBp = expensesByArea[item.areaId] ?: 0,
      )
    }
  return SaleTotals(
    lines = computed,
    subtotalCents = subtotal,
    discountCents = computed.sumOf { it.discountCents },
    totalCents = computed.sumOf { it.netCents },
  )
}

data class Sale(
  val id: String,
  val clinicId: String,
  val number: String,
  val clientId: String?,
  val clientName: String?,
  val clientPhone: String?,
  val status: SaleStatus,
  val subtotalCents: Long,
  val discountCents: Long,
  val totalCents: Long,
  val notes: String?,
  val soldAt: Long,
  val cancelledAt: Long?,
  val pendingSync: Boolean,
)

data class SaleLine(
  val id: String,
  val saleId: String,
  val itemId: String,
  val itemKind: CatalogKind,
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
)

data class SalePayment(
  val id: String,
  val methodId: String,
  val methodName: String,
  val affectsCash: Boolean,
  val amountCents: Long,
  val tenderedCents: Long?,
  val changeCents: Long?,
)

data class SaleDetail(
  val sale: Sale,
  val lines: List<SaleLine>,
  val payments: List<SalePayment>,
  val cancellationReason: String?,
)

data class AreaCostSetting(
  val id: String,
  val areaId: String,
  val expensesPercentBp: Int,
  val validFrom: String,
  val pendingSync: Boolean,
)

data class CashSession(
  val id: String,
  val clinicId: String,
  val openedAt: Long,
  val closedAt: Long?,
  val openingCents: Long,
  val expectedCashCents: Long?,
  val countedCashCents: Long?,
  val differenceCents: Long?,
  val status: CashSessionStatus,
  val notes: String?,
  val pendingSync: Boolean,
) {
  val label: String
    get() =
      if (status == CashSessionStatus.Open) {
        "Caja abierta desde ${formatSaleTime(openedAt)}"
      } else {
        "Caja cerrada el ${formatSaleDate(closedAt ?: openedAt)}"
      }
}

data class CashMovement(
  val id: String,
  val sessionId: String,
  val type: CashMovementType,
  val amountCents: Long,
  val reason: String?,
  val saleId: String?,
  val paymentId: String?,
  val saleCancellationId: String?,
  val createdAt: Long,
  val pendingSync: Boolean,
)

data class CashSessionDetail(
  val session: CashSession,
  val movements: List<CashMovement>,
  val expectedCashCents: Long,
)

fun formatMoney(cents: Long): String {
  val format = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
  format.currency = Currency.getInstance("ARS")
  return format.format(cents / 100.0)
}

fun parseMoneyToCents(raw: String): Long? {
  val cleaned = raw.trim().replace("$", "").replace(" ", "")
  if (cleaned.isEmpty()) return null
  val normalized =
    when {
      cleaned.contains(",") && cleaned.contains(".") ->
        cleaned.replace(".", "").replace(",", ".")
      cleaned.contains(",") -> cleaned.replace(",", ".")
      else -> cleaned
    }
  val value = normalized.toBigDecimalOrNull() ?: return null
  if (value < java.math.BigDecimal.ZERO) return null
  return value.multiply(java.math.BigDecimal(100)).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
}

fun parsePercentToBp(raw: String): Int? {
  val cleaned = raw.trim().replace("%", "").replace(",", ".")
  if (cleaned.isBlank()) return null
  val value = cleaned.toBigDecimalOrNull() ?: return null
  if (value < java.math.BigDecimal.ZERO || value > java.math.BigDecimal(100)) return null
  return value.multiply(java.math.BigDecimal(100)).setScale(0, java.math.RoundingMode.HALF_UP).toInt()
}

fun formatPercentBp(bp: Int): String =
  String.format(Locale("es", "AR"), "%.2f%%", bp / 100.0)

fun formatSaleDate(timestamp: Long): String =
  java.time.Instant.ofEpochMilli(timestamp)
    .atZone(ClinicTime.zone)
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun formatSaleDateTime(timestamp: Long): String =
  java.time.Instant.ofEpochMilli(timestamp)
    .atZone(ClinicTime.zone)
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

fun formatSaleTime(timestamp: Long): String =
  java.time.Instant.ofEpochMilli(timestamp)
    .atZone(ClinicTime.zone)
    .format(DateTimeFormatter.ofPattern("HH:mm"))

fun saleDayKey(date: LocalDate = ClinicTime.today()): String =
  date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

fun ensureEffectivePercent(
  settings: List<AreaCostSetting>,
  areaId: String,
  date: LocalDate = ClinicTime.today(),
): AreaCostSetting? =
  settings
    .filter { it.areaId == areaId && it.validFrom <= date.toString() }
    .maxWithOrNull(compareBy<AreaCostSetting>({ it.validFrom }, { it.id }))

fun computeEstimatedResult(
  netCents: Long,
  productCostCents: Long,
  serviceCostCents: Long,
  expensesCents: Long,
): Long = netCents - productCostCents - serviceCostCents - expensesCents

fun PaymentMethodEntity.toDomain(): PaymentMethod =
  PaymentMethod(
    id = id,
    clinicId = clinicId,
    name = name,
    affectsCash = affectsCash,
    active = active,
    sortOrder = sortOrder,
    pendingSync = pendingSync,
  )

fun SaleWithClient.toDomain(): Sale =
  Sale(
    id = sale.id,
    clinicId = sale.clinicId,
    number = sale.number,
    clientId = sale.clientId,
    clientName = sale.clientName,
    clientPhone = clientPhone,
    status = SaleStatus.fromStorage(sale.status),
    subtotalCents = sale.subtotalCents,
    discountCents = sale.discountCents,
    totalCents = sale.totalCents,
    notes = sale.notes,
    soldAt = sale.soldAt,
    cancelledAt = sale.cancelledAt,
    pendingSync = sale.pendingSync,
  )

fun SaleEntity.toDomain(): Sale =
  Sale(
    id = id,
    clinicId = clinicId,
    number = number,
    clientId = clientId,
    clientName = clientName,
    clientPhone = null,
    status = SaleStatus.fromStorage(status),
    subtotalCents = subtotalCents,
    discountCents = discountCents,
    totalCents = totalCents,
    notes = notes,
    soldAt = soldAt,
    cancelledAt = cancelledAt,
    pendingSync = pendingSync,
  )

fun SaleItemEntity.toDomain(): SaleLine =
  SaleLine(
    id = id,
    saleId = saleId,
    itemId = itemId,
    itemKind = CatalogKind.fromStorage(itemKind),
    itemName = itemName,
    areaId = areaId,
    areaName = areaName,
    quantity = quantity,
    unitPriceCents = unitPriceCents,
    unitCostCents = unitCostCents,
    discountCents = discountCents,
    grossCents = grossCents,
    netCents = netCents,
    expensesPercentBp = expensesPercentBp,
    lot = lot,
    expiresOn = expiresOn,
  )

fun CashSessionEntity.toDomain(): CashSession =
  CashSession(
    id = id,
    clinicId = clinicId,
    openedAt = openedAt,
    closedAt = closedAt,
    openingCents = openingCents,
    expectedCashCents = expectedCashCents,
    countedCashCents = countedCashCents,
    differenceCents = differenceCents,
    status = CashSessionStatus.fromStorage(status),
    notes = notes,
    pendingSync = pendingSync,
  )

fun CashMovementEntity.toDomain(): CashMovement =
  CashMovement(
    id = id,
    sessionId = sessionId,
    type = CashMovementType.fromStorage(type),
    amountCents = amountCents,
    reason = reason,
    saleId = saleId,
    paymentId = paymentId,
    saleCancellationId = saleCancellationId,
    createdAt = createdAt,
    pendingSync = pendingSync,
  )

fun AreaCostSettingEntity.toDomain(): AreaCostSetting =
  AreaCostSetting(
    id = id,
    areaId = areaId,
    expensesPercentBp = expensesPercentBp,
    validFrom = validFrom,
    pendingSync = pendingSync,
  )
