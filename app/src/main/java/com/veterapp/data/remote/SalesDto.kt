package com.veterapp.data.remote

import com.veterapp.data.local.AreaCostSettingEntity
import com.veterapp.data.local.CashMovementEntity
import com.veterapp.data.local.CashSessionEntity
import com.veterapp.data.local.PaymentEntity
import com.veterapp.data.local.PaymentMethodEntity
import com.veterapp.data.local.SaleCancellationEntity
import com.veterapp.data.local.SaleEntity
import com.veterapp.data.local.SaleItemEntity
import com.veterapp.domain.CashMovementType
import com.veterapp.domain.CashSessionStatus
import com.veterapp.domain.SaleStatus
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  val name: String,
  @SerialName("affects_cash") val affectsCash: Boolean? = null,
  val active: Boolean? = null,
  @SerialName("sort_order") val sortOrder: Int? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class SaleDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  val number: String,
  @SerialName("client_id") val clientId: String? = null,
  @SerialName("client_name") val clientName: String? = null,
  val status: String? = null,
  @SerialName("subtotal_cents") val subtotalCents: Long,
  @SerialName("discount_cents") val discountCents: Long,
  @SerialName("total_cents") val totalCents: Long,
  val notes: String? = null,
  @SerialName("sold_at") val soldAt: String,
  @SerialName("cancelled_at") val cancelledAt: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class SaleItemDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  @SerialName("sale_id") val saleId: String,
  @SerialName("item_id") val itemId: String,
  @SerialName("item_kind") val itemKind: String,
  @SerialName("item_name") val itemName: String,
  @SerialName("area_id") val areaId: String,
  @SerialName("area_name") val areaName: String,
  val quantity: Int,
  @SerialName("unit_price_cents") val unitPriceCents: Long,
  @SerialName("unit_cost_cents") val unitCostCents: Long? = null,
  @SerialName("discount_cents") val discountCents: Long,
  @SerialName("gross_cents") val grossCents: Long,
  @SerialName("net_cents") val netCents: Long,
  @SerialName("expenses_percent_bp") val expensesPercentBp: Int,
  val lot: String? = null,
  @SerialName("expires_on") val expiresOn: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class PaymentDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  @SerialName("sale_id") val saleId: String,
  @SerialName("method_id") val methodId: String,
  @SerialName("amount_cents") val amountCents: Long,
  @SerialName("tendered_cents") val tenderedCents: Long? = null,
  @SerialName("change_cents") val changeCents: Long? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class CashSessionDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  @SerialName("opened_at") val openedAt: String,
  @SerialName("closed_at") val closedAt: String? = null,
  @SerialName("opening_cents") val openingCents: Long,
  @SerialName("expected_cash_cents") val expectedCashCents: Long? = null,
  @SerialName("counted_cash_cents") val countedCashCents: Long? = null,
  @SerialName("difference_cents") val differenceCents: Long? = null,
  val status: String? = null,
  val notes: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class CashMovementDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  @SerialName("session_id") val sessionId: String,
  val type: String,
  @SerialName("amount_cents") val amountCents: Long,
  val reason: String? = null,
  @SerialName("sale_id") val saleId: String? = null,
  @SerialName("payment_id") val paymentId: String? = null,
  @SerialName("sale_cancellation_id") val saleCancellationId: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class SaleCancellationDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  @SerialName("sale_id") val saleId: String,
  val reason: String,
  @SerialName("cancelled_at") val cancelledAt: String,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class AreaCostSettingDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  @SerialName("area_id") val areaId: String,
  @SerialName("expenses_percent_bp") val expensesPercentBp: Int,
  @SerialName("valid_from") val validFrom: String,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

fun PaymentMethodEntity.toDto(): PaymentMethodDto =
  PaymentMethodDto(
    id = id,
    clinicId = clinicId,
    name = name,
    affectsCash = affectsCash,
    active = active,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
    originDeviceId = originDeviceId,
  )

fun PaymentMethodDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): PaymentMethodEntity =
  PaymentMethodEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    name = name,
    affectsCash = affectsCash ?: false,
    active = active ?: true,
    sortOrder = sortOrder ?: 0,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    deletedAt = deletedAt?.toEpochMillisOrNull(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

fun SaleEntity.toDto(): SaleDto =
  SaleDto(
    id = id,
    clinicId = clinicId,
    number = number,
    clientId = clientId,
    clientName = clientName,
    status = status,
    subtotalCents = subtotalCents,
    discountCents = discountCents,
    totalCents = totalCents,
    notes = notes,
    soldAt = Instant.ofEpochMilli(soldAt).toString(),
    cancelledAt = cancelledAt?.let { Instant.ofEpochMilli(it).toString() },
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    originDeviceId = originDeviceId,
  )

fun SaleDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): SaleEntity =
  SaleEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    number = number,
    clientId = clientId,
    clientName = clientName,
    status = SaleStatus.fromStorage(status).storage,
    subtotalCents = subtotalCents,
    discountCents = discountCents,
    totalCents = totalCents,
    notes = notes,
    soldAt = soldAt.toEpochMillisOrNow(),
    cancelledAt = cancelledAt?.toEpochMillisOrNull(),
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

fun SaleItemEntity.toDto(): SaleItemDto =
  SaleItemDto(
    id = id,
    clinicId = clinicId,
    saleId = saleId,
    itemId = itemId,
    itemKind = itemKind,
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
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    originDeviceId = originDeviceId,
  )

fun SaleItemDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): SaleItemEntity =
  SaleItemEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    saleId = saleId,
    itemId = itemId,
    itemKind = itemKind,
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
    createdAt = createdAt.toEpochMillisOrNow(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

fun PaymentEntity.toDto(): PaymentDto =
  PaymentDto(
    id = id,
    clinicId = clinicId,
    saleId = saleId,
    methodId = methodId,
    amountCents = amountCents,
    tenderedCents = tenderedCents,
    changeCents = changeCents,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    originDeviceId = originDeviceId,
  )

fun PaymentDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): PaymentEntity =
  PaymentEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    saleId = saleId,
    methodId = methodId,
    amountCents = amountCents,
    tenderedCents = tenderedCents,
    changeCents = changeCents,
    createdAt = createdAt.toEpochMillisOrNow(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

fun CashSessionEntity.toDto(): CashSessionDto =
  CashSessionDto(
    id = id,
    clinicId = clinicId,
    openedAt = Instant.ofEpochMilli(openedAt).toString(),
    closedAt = closedAt?.let { Instant.ofEpochMilli(it).toString() },
    openingCents = openingCents,
    expectedCashCents = expectedCashCents,
    countedCashCents = countedCashCents,
    differenceCents = differenceCents,
    status = status,
    notes = notes,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    originDeviceId = originDeviceId,
  )

fun CashSessionDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): CashSessionEntity =
  CashSessionEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    openedAt = openedAt.toEpochMillisOrNow(),
    closedAt = closedAt?.toEpochMillisOrNull(),
    openingCents = openingCents,
    expectedCashCents = expectedCashCents,
    countedCashCents = countedCashCents,
    differenceCents = differenceCents,
    status = CashSessionStatus.fromStorage(status).storage,
    notes = notes,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

fun CashMovementEntity.toDto(): CashMovementDto =
  CashMovementDto(
    id = id,
    clinicId = clinicId,
    sessionId = sessionId,
    type = type,
    amountCents = amountCents,
    reason = reason,
    saleId = saleId,
    paymentId = paymentId,
    saleCancellationId = saleCancellationId,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    originDeviceId = originDeviceId,
  )

fun CashMovementDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): CashMovementEntity =
  CashMovementEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    sessionId = sessionId,
    type = CashMovementType.fromStorage(type).storage,
    amountCents = amountCents,
    reason = reason,
    saleId = saleId,
    paymentId = paymentId,
    saleCancellationId = saleCancellationId,
    createdAt = createdAt.toEpochMillisOrNow(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

fun SaleCancellationEntity.toDto(): SaleCancellationDto =
  SaleCancellationDto(
    id = id,
    clinicId = clinicId,
    saleId = saleId,
    reason = reason,
    cancelledAt = Instant.ofEpochMilli(cancelledAt).toString(),
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    originDeviceId = originDeviceId,
  )

fun SaleCancellationDto.toEntity(
  fallbackClinicId: String,
  fallbackDeviceId: String,
): SaleCancellationEntity =
  SaleCancellationEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    saleId = saleId,
    reason = reason,
    cancelledAt = cancelledAt.toEpochMillisOrNow(),
    createdAt = createdAt.toEpochMillisOrNow(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

fun AreaCostSettingEntity.toDto(): AreaCostSettingDto =
  AreaCostSettingDto(
    id = id,
    clinicId = clinicId,
    areaId = areaId,
    expensesPercentBp = expensesPercentBp,
    validFrom = validFrom,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    originDeviceId = originDeviceId,
  )

fun AreaCostSettingDto.toEntity(
  fallbackClinicId: String,
  fallbackDeviceId: String,
): AreaCostSettingEntity =
  AreaCostSettingEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    areaId = areaId,
    expensesPercentBp = expensesPercentBp,
    validFrom = validFrom,
    createdAt = createdAt.toEpochMillisOrNow(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )
