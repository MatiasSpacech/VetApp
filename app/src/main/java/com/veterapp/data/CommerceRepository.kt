package com.veterapp.data

import androidx.room.withTransaction
import com.veterapp.data.local.AreaCostSettingEntity
import com.veterapp.data.local.CashMovementEntity
import com.veterapp.data.local.CashSessionEntity
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.PaymentEntity
import com.veterapp.data.local.PaymentMethodEntity
import com.veterapp.data.local.SaleCancellationEntity
import com.veterapp.data.local.SaleCounterEntity
import com.veterapp.data.local.SaleEntity
import com.veterapp.data.local.SaleItemEntity
import com.veterapp.data.local.SaleWithClient
import com.veterapp.data.local.StockMovementEntity
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.AreaCostSettingDto
import com.veterapp.data.remote.CashMovementDto
import com.veterapp.data.remote.CashSessionDto
import com.veterapp.data.remote.PaymentDto
import com.veterapp.data.remote.PaymentMethodDto
import com.veterapp.data.remote.SaleCancellationDto
import com.veterapp.data.remote.SaleDto
import com.veterapp.data.remote.SaleItemDto
import com.veterapp.data.remote.toDto
import com.veterapp.data.remote.toEntity
import com.veterapp.domain.AreaCostSetting
import com.veterapp.domain.CashMovement
import com.veterapp.domain.CashMovementType
import com.veterapp.domain.CashSession
import com.veterapp.domain.CashSessionDetail
import com.veterapp.domain.CashSessionStatus
import com.veterapp.domain.CatalogItem
import com.veterapp.domain.CatalogKind
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.PaymentMethod
import com.veterapp.domain.Sale
import com.veterapp.domain.SaleDetail
import com.veterapp.domain.SaleDraft
import com.veterapp.domain.SaleLine
import com.veterapp.domain.SaleLineComputed
import com.veterapp.domain.SalePayment
import com.veterapp.domain.SaleStatus
import com.veterapp.domain.StockMovementType
import com.veterapp.domain.calculateSaleTotals
import com.veterapp.domain.saleDayKey
import com.veterapp.domain.toDomain
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

class CommerceRepository(
  private val database: VetDatabase,
  private val settings: AppSettings,
  private val auditRepository: AuditRepository = AuditRepository(database, settings),
) {
  private val paymentMethodDao = database.paymentMethodDao()
  private val saleDao = database.saleDao()
  private val saleItemDao = database.saleItemDao()
  private val paymentDao = database.paymentDao()
  private val cashSessionDao = database.cashSessionDao()
  private val cashMovementDao = database.cashMovementDao()
  private val cancellationDao = database.saleCancellationDao()
  private val counterDao = database.saleCounterDao()
  private val costDao = database.areaCostSettingDao()
  private val itemDao = database.catalogItemDao()
  private val movementDao = database.stockMovementDao()
  private val clientDao = database.clientDao()
  private val outboxDao = database.outboxDao()
  private val json = Json { encodeDefaults = true }

  val paymentMethods: Flow<List<PaymentMethod>> =
    paymentMethodDao.observeAll().map { rows -> rows.map(PaymentMethodEntity::toDomain) }

  val activePaymentMethods: Flow<List<PaymentMethod>> =
    paymentMethodDao.observeActive().map { rows -> rows.map(PaymentMethodEntity::toDomain) }

  val openCashSession: Flow<CashSession?> =
    cashSessionDao.observeOpen().map { it?.toDomain() }

  val cashSessions: Flow<List<CashSession>> =
    cashSessionDao.observeAll().map { rows -> rows.map(CashSessionEntity::toDomain) }

  fun observeSalesBetween(from: Long, to: Long): Flow<List<Sale>> =
    saleDao.observeBetween(from, to).map { rows -> rows.map(SaleWithClient::toDomain) }

  fun observeSaleDetail(saleId: String): Flow<SaleDetail?> =
    combine(
      saleDao.observeBetween(0L, Long.MAX_VALUE),
      saleItemDao.observeForSale(saleId),
      paymentDao.observeForSale(saleId),
      paymentMethodDao.observeAll(),
    ) { _, lines, payments, methods ->
      val saleRow = saleDao.findByIdWithClient(saleId) ?: return@combine null
      val methodNames = methods.associate { it.id to it.name }
      val affectsCash = methods.associate { it.id to it.affectsCash }
      SaleDetail(
        sale = saleRow.toDomain(),
        lines = lines.map { it.toDomain() },
        payments =
          payments.map { payment ->
            SalePayment(
              id = payment.id,
              methodId = payment.methodId,
              methodName = methodNames[payment.methodId] ?: "Medio",
              affectsCash = affectsCash[payment.methodId] ?: false,
              amountCents = payment.amountCents,
              tenderedCents = payment.tenderedCents,
              changeCents = payment.changeCents,
            )
          },
        cancellationReason = cancellationDao.findBySale(saleId)?.reason,
      )
    }

  fun observeCashDetail(sessionId: String): Flow<CashSessionDetail?> =
    combine(cashSessionDao.observeAll(), cashMovementDao.observeForSession(sessionId)) { sessions, movements ->
      val session = sessions.firstOrNull { it.id == sessionId } ?: return@combine null
      val domain = session.toDomain()
      CashSessionDetail(
        session = domain,
        movements = movements.map(CashMovementEntity::toDomain),
        expectedCashCents = domain.expectedCashCents ?: (domain.openingCents + movements.sumOf { it.amountCents }),
      )
    }

  suspend fun ensureDefaults(clinicId: String) {
    DEFAULT_METHODS.forEachIndexed { index, (name, affectsCash) ->
      val id = CatalogRepository.stableUuid("payment_method", clinicId, name)
      if (paymentMethodDao.findById(id) != null) return@forEachIndexed
      val now = System.currentTimeMillis()
      val entity =
        PaymentMethodEntity(
          id = id,
          clinicId = clinicId,
          name = name,
          affectsCash = affectsCash,
          active = true,
          sortOrder = index,
          createdAt = now,
          updatedAt = now,
          revision = 0L,
          deletedAt = null,
          originDeviceId = settings.deviceId,
          pendingSync = true,
        )
      database.withTransaction {
        paymentMethodDao.upsert(entity)
        outboxDao.insert(outbox("payment_method", entity.id, json.encodeToJsonElement(entity.toDto()), null, now))
      }
    }
  }

  suspend fun createPaymentMethod(name: String, affectsCash: Boolean): PaymentMethod {
    val clean = name.trim()
    require(clean.isNotBlank()) { "Completá el nombre del medio de pago" }
    val existing = paymentMethodDao.all()
    require(existing.none { it.deletedAt == null && it.name.equals(clean, ignoreCase = true) }) {
      "Ya existe un medio de pago con ese nombre"
    }
    val now = System.currentTimeMillis()
    val entity =
      PaymentMethodEntity(
        id = UUID.randomUUID().toString(),
        clinicId = settings.currentClinicId,
        name = clean,
        affectsCash = affectsCash,
        active = true,
        sortOrder = existing.size,
        createdAt = now,
        updatedAt = now,
        revision = 0L,
        deletedAt = null,
        originDeviceId = settings.deviceId,
        pendingSync = true,
      )
    database.withTransaction {
      paymentMethodDao.upsert(entity)
      outboxDao.insert(outbox("payment_method", entity.id, json.encodeToJsonElement(entity.toDto()), null, now))
      auditRepository.record(
        entity = "payment_method",
        entityId = entity.id,
        action = "payment_method.create",
        payload = buildJsonObject { put("name", clean) },
        clinicId = entity.clinicId,
      )
    }
    return entity.toDomain()
  }

  suspend fun updatePaymentMethod(methodId: String, name: String, active: Boolean) {
    val existing = paymentMethodDao.findById(methodId) ?: error("El medio de pago no existe")
    val clean = name.trim()
    require(clean.isNotBlank()) { "Completá el nombre del medio de pago" }
    if (existing.active && !active) {
      val remainingCash =
        paymentMethodDao.all().count {
          it.id != methodId && it.active && it.deletedAt == null && it.affectsCash
        }
      if (existing.affectsCash) {
        require(remainingCash >= 1) { "Debe quedar al menos un medio de pago en efectivo activo" }
      }
    }
    val updated = existing.copy(name = clean, active = active, updatedAt = System.currentTimeMillis(), pendingSync = true)
    database.withTransaction {
      paymentMethodDao.upsert(updated)
      outboxDao.insert(outbox("payment_method", updated.id, json.encodeToJsonElement(updated.toDto()), existing.revision, updated.updatedAt))
      auditRepository.record(
        entity = "payment_method",
        entityId = updated.id,
        action = "payment_method.update",
        payload = buildJsonObject { put("name", clean) },
        clinicId = updated.clinicId,
      )
    }
  }

  suspend fun createSale(draft: SaleDraft): SaleDetail {
    draft.validate()?.let { error(it) }
    val session = cashSessionDao.findOpen() ?: error("Abrí la caja antes de vender")
    val clinicId = settings.currentClinicId
    val client = draft.clientId?.let { id ->
      val client = clientDao.findById(id) ?: error("El cliente no existe")
      require(client.deletedAt == null) { "El cliente está archivado" }
      client
    }
    val resolved = mutableListOf<Pair<CatalogItem, com.veterapp.domain.SaleLineDraft>>()
    for (line in draft.lines) {
      val item = itemDao.findById(line.itemId) ?: error("Un ítem de la venta ya no existe")
      require(item.deletedAt == null) { "Un ítem de la venta está archivado" }
      require(item.active) { "${item.name} está inactivo y no se puede vender" }
      require(item.clinicId == clinicId) { "El ítem pertenece a otra clínica" }
      val gross = item.salePriceCents * line.quantity
      require(line.discountCents <= gross) { "El descuento de ${item.name} supera su importe" }
      val domainItem =
        item.toDomain(
          areaName = database.areaDao().findById(item.areaId)?.name ?: "Sin área",
          categoryName = null,
        )
      resolved += domainItem to line
    }
    val today = ClinicTime.today()
    val expensesByArea =
      resolved.map { it.first.areaId }.distinct().associateWith { areaId ->
        costDao.effectiveFor(areaId, today.toString())?.expensesPercentBp ?: 0
      }
    val totals = calculateSaleTotals(resolved, draft.globalDiscountCents, expensesByArea)
    require(draft.globalDiscountCents <= totals.subtotalCents - totals.lines.sumOf { it.lineDiscountCents }) {
      "El descuento global supera el total de la venta"
    }
    val payments = resolvePayments(draft, totals.totalCents)
    val stockChecks = mutableMapOf<String, Int>()
    totals.lines.filter { it.item.trackStock }.forEach { line ->
      val previous = stockChecks[line.item.id] ?: line.item.stockCached
      stockChecks[line.item.id] = previous - line.quantity
    }
    if (!settings.allowNegativeStock) {
      stockChecks.forEach { (itemId, stock) ->
        require(stock >= 0) {
          "Stock insuficiente de ${resolved.first { it.first.id == itemId }.first.name}"
        }
      }
    }
    val now = System.currentTimeMillis()
    val saleId = UUID.randomUUID().toString()
    lateinit var saved: SaleDetail
    database.withTransaction {
      val number = nextNumber(clinicId, today)
      val sale =
        SaleEntity(
          id = saleId,
          clinicId = clinicId,
          number = number,
          clientId = client?.id,
          clientName = client?.name,
          status = SaleStatus.Completed.storage,
          subtotalCents = totals.subtotalCents,
          discountCents = totals.discountCents,
          totalCents = totals.totalCents,
          notes = draft.notes?.trim()?.takeIf { it.isNotBlank() },
          soldAt = now,
          cancelledAt = null,
          createdAt = now,
          updatedAt = now,
          revision = 0L,
          originDeviceId = settings.deviceId,
          pendingSync = true,
        )
      val itemEntities =
        totals.lines.map { line ->
          SaleItemEntity(
            id = UUID.randomUUID().toString(),
            clinicId = clinicId,
            saleId = sale.id,
            itemId = line.item.id,
            itemKind = line.item.kind.storage,
            itemName = line.item.name,
            areaId = line.item.areaId,
            areaName = line.item.areaName,
            quantity = line.quantity,
            unitPriceCents = line.item.salePriceCents,
            unitCostCents = line.item.unitCostCents,
            discountCents = line.discountCents,
            grossCents = line.grossCents,
            netCents = line.netCents,
            expensesPercentBp = line.expensesPercentBp,
            lot = line.item.lot,
            expiresOn = line.item.expiresOn,
            createdAt = now,
            originDeviceId = settings.deviceId,
            pendingSync = true,
          )
        }
      val paymentEntities =
        payments.map { resolvedPayment ->
          PaymentEntity(
            id = UUID.randomUUID().toString(),
            clinicId = clinicId,
            saleId = sale.id,
            methodId = resolvedPayment.method.id,
            amountCents = resolvedPayment.amountCents,
            tenderedCents = resolvedPayment.tenderedCents,
            changeCents = resolvedPayment.changeCents,
            createdAt = now,
            originDeviceId = settings.deviceId,
            pendingSync = true,
          )
        }
      val stockMovements =
        itemEntities.filter { it.itemKind == CatalogKind.Product.storage }.map { line ->
          StockMovementEntity(
            id = UUID.randomUUID().toString(),
            clinicId = clinicId,
            itemId = line.itemId,
            type = StockMovementType.Sale.storage,
            quantityDelta = -line.quantity,
            unitCostCents = line.unitCostCents,
            reason = "Venta ${sale.number}",
            saleId = sale.id,
            saleItemId = line.id,
            saleCancellationId = null,
            createdAt = now,
            originDeviceId = settings.deviceId,
            pendingSync = true,
          )
        }
      val cashMovements =
        paymentEntities.filter { payment ->
          payments.first { it.method.id == payment.methodId }.method.affectsCash
        }.map { payment ->
          CashMovementEntity(
            id = UUID.randomUUID().toString(),
            clinicId = clinicId,
            sessionId = session.id,
            type = CashMovementType.Sale.storage,
            amountCents = payment.amountCents,
            reason = null,
            saleId = sale.id,
            paymentId = payment.id,
            saleCancellationId = null,
            createdAt = now,
            originDeviceId = settings.deviceId,
            pendingSync = true,
          )
        }
      saleDao.upsert(sale)
      saleItemDao.insertAll(itemEntities)
      paymentDao.insertAll(paymentEntities)
      stockMovements.forEach { movement ->
        movementDao.insert(movement)
        val item = itemDao.findById(movement.itemId) ?: error("El ítem de la venta no existe")
        itemDao.upsert(item.copy(stockCached = item.stockCached + movement.quantityDelta, updatedAt = now, pendingSync = true))
      }
      cashMovements.forEach { cashMovementDao.insert(it) }
      outboxDao.insert(
        outbox(
          "sale",
          sale.id,
          buildJsonObject {
            put("sale", json.encodeToJsonElement(SaleDto.serializer(), sale.toDto()))
            put("items", json.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(SaleItemDto.serializer()), itemEntities.map { it.toDto() }))
            put("payments", json.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(PaymentDto.serializer()), paymentEntities.map { it.toDto() }))
            put("stock_movements", json.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(com.veterapp.data.remote.StockMovementDto.serializer()), stockMovements.map { it.toDto() }))
            put("cash_movements", json.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(CashMovementDto.serializer()), cashMovements.map { it.toDto() }))
          },
          null,
          now,
        ),
      )
      auditRepository.record(
        entity = "sale",
        entityId = sale.id,
        action = "sale.create",
        payload =
          buildJsonObject {
            put("number", sale.number)
            put("total_cents", sale.totalCents)
            put("client", sale.clientName)
          },
        clinicId = clinicId,
      )
      saved =
        SaleDetail(
          sale = sale.toDomain(),
          lines = itemEntities.map { it.toDomain() },
          payments =
            paymentEntities.map { payment ->
              val method = payments.first { it.method.id == payment.methodId }.method
              SalePayment(
                id = payment.id,
                methodId = payment.methodId,
                methodName = method.name,
                affectsCash = method.affectsCash,
                amountCents = payment.amountCents,
                tenderedCents = payment.tenderedCents,
                changeCents = payment.changeCents,
              )
            },
          cancellationReason = null,
        )
    }
    return saved
  }

  suspend fun cancelSale(saleId: String, reason: String): SaleDetail {
    val cleanReason = reason.trim()
    require(cleanReason.isNotBlank()) { "Indicá el motivo de la anulación" }
    val sale = saleDao.findById(saleId) ?: error("La venta no existe")
    require(sale.status == SaleStatus.Completed.storage) { "La venta ya está anulada" }
    require(cancellationDao.findBySale(saleId) == null) { "La venta ya está anulada" }
    val payments = paymentDao.forSale(saleId)
    val methods = paymentMethodDao.all().associateBy { it.id }
    val cashPayments = payments.filter { methods[it.methodId]?.affectsCash == true }
    val session =
      if (cashPayments.isEmpty()) {
        null
      } else {
        cashSessionDao.findOpen() ?: error("Abrí la caja para revertir el efectivo")
      }
    val now = System.currentTimeMillis()
    val lines = saleItemDao.forSale(saleId)
    val cancellationId = UUID.randomUUID().toString()
    val cancellation =
      SaleCancellationEntity(
        id = cancellationId,
        clinicId = sale.clinicId,
        saleId = sale.id,
        reason = cleanReason,
        cancelledAt = now,
        createdAt = now,
        originDeviceId = settings.deviceId,
        pendingSync = true,
      )
    val updatedSale = sale.copy(status = SaleStatus.Cancelled.storage, cancelledAt = now, updatedAt = now, pendingSync = true)
    val stockMovements =
      lines.filter { it.itemKind == CatalogKind.Product.storage }.map { line ->
        StockMovementEntity(
          id = UUID.randomUUID().toString(),
          clinicId = sale.clinicId,
          itemId = line.itemId,
          type = StockMovementType.Cancellation.storage,
          quantityDelta = line.quantity,
          unitCostCents = line.unitCostCents,
          reason = "Anulación ${sale.number}",
          saleId = sale.id,
          saleItemId = line.id,
          saleCancellationId = cancellationId,
          createdAt = now,
          originDeviceId = settings.deviceId,
          pendingSync = true,
        )
      }
    val cashMovements =
      if (session != null) {
        cashPayments.map { payment ->
          CashMovementEntity(
            id = UUID.randomUUID().toString(),
            clinicId = sale.clinicId,
            sessionId = session.id,
            type = CashMovementType.Cancellation.storage,
            amountCents = -payment.amountCents,
            reason = "Anulación ${sale.number}",
            saleId = sale.id,
            paymentId = payment.id,
            saleCancellationId = cancellationId,
            createdAt = now,
            originDeviceId = settings.deviceId,
            pendingSync = true,
          )
        }
      } else {
        emptyList()
      }
    database.withTransaction {
      saleDao.upsert(updatedSale)
      cancellationDao.insert(cancellation)
      stockMovements.forEach { movement ->
        movementDao.insert(movement)
        val item = itemDao.findById(movement.itemId) ?: error("El ítem no existe")
        itemDao.upsert(item.copy(stockCached = item.stockCached + movement.quantityDelta, updatedAt = now, pendingSync = true))
      }
      cashMovements.forEach { cashMovementDao.insert(it) }
      outboxDao.insert(
        outbox(
          "sale_cancellation",
          cancellation.id,
          buildJsonObject {
            put("cancellation", json.encodeToJsonElement(SaleCancellationDto.serializer(), cancellation.toDto()))
            put("sale", json.encodeToJsonElement(SaleDto.serializer(), updatedSale.toDto()))
            put("stock_movements", json.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(com.veterapp.data.remote.StockMovementDto.serializer()), stockMovements.map { it.toDto() }))
            put("cash_movements", json.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(CashMovementDto.serializer()), cashMovements.map { it.toDto() }))
          },
          sale.revision,
          now,
        ),
      )
      auditRepository.record(
        entity = "sale",
        entityId = sale.id,
        action = "sale.cancel",
        payload =
          buildJsonObject {
            put("number", sale.number)
            put("reason", cleanReason)
            put("total_cents", sale.totalCents)
          },
        clinicId = sale.clinicId,
      )
    }
    return SaleDetail(
      sale = updatedSale.toDomain(),
      lines = lines.map { it.toDomain() },
      payments =
        payments.map { payment ->
          val method = methods[payment.methodId]
          SalePayment(
            id = payment.id,
            methodId = payment.methodId,
            methodName = method?.name ?: "Medio",
            affectsCash = method?.affectsCash ?: false,
            amountCents = payment.amountCents,
            tenderedCents = payment.tenderedCents,
            changeCents = payment.changeCents,
          )
        },
      cancellationReason = cleanReason,
    )
  }

  suspend fun openCashSession(openingCents: Long, notes: String?): CashSession {
    require(openingCents >= 0) { "El monto inicial no puede ser negativo" }
    require(cashSessionDao.findOpen() == null) { "Ya hay una caja abierta" }
    val now = System.currentTimeMillis()
    val entity =
      CashSessionEntity(
        id = UUID.randomUUID().toString(),
        clinicId = settings.currentClinicId,
        openedAt = now,
        closedAt = null,
        openingCents = openingCents,
        expectedCashCents = null,
        countedCashCents = null,
        differenceCents = null,
        status = CashSessionStatus.Open.storage,
        notes = notes?.trim()?.takeIf { it.isNotBlank() },
        createdAt = now,
        updatedAt = now,
        revision = 0L,
        originDeviceId = settings.deviceId,
        pendingSync = true,
      )
    database.withTransaction {
      cashSessionDao.upsert(entity)
      outboxDao.insert(outbox("cash_session", entity.id, json.encodeToJsonElement(entity.toDto()), null, now))
      auditRepository.record(
        entity = "cash_session",
        entityId = entity.id,
        action = "cash.open",
        payload = buildJsonObject { put("opening_cents", openingCents) },
        clinicId = entity.clinicId,
      )
    }
    return entity.toDomain()
  }

  suspend fun addCashMovement(type: CashMovementType, amountCents: Long, reason: String?): CashMovement {
    require(type in setOf(CashMovementType.Withdrawal, CashMovementType.Expense, CashMovementType.Adjustment)) {
      "El tipo de movimiento no es válido"
    }
    require(amountCents != 0L) { "El importe no puede ser cero" }
    val cleanReason = reason?.trim()
    require(!cleanReason.isNullOrBlank()) { "Indicá el motivo del movimiento" }
    val session = cashSessionDao.findOpen() ?: error("La caja está cerrada")
    val signed =
      when (type) {
        CashMovementType.Withdrawal, CashMovementType.Expense -> -kotlin.math.abs(amountCents)
        else -> amountCents
      }
    val now = System.currentTimeMillis()
    val entity =
      CashMovementEntity(
        id = UUID.randomUUID().toString(),
        clinicId = session.clinicId,
        sessionId = session.id,
        type = type.storage,
        amountCents = signed,
        reason = cleanReason,
        saleId = null,
        paymentId = null,
        saleCancellationId = null,
        createdAt = now,
        originDeviceId = settings.deviceId,
        pendingSync = true,
      )
    database.withTransaction {
      cashMovementDao.insert(entity)
      outboxDao.insert(outbox("cash_movement", entity.id, json.encodeToJsonElement(entity.toDto()), null, now))
      auditRepository.record(
        entity = "cash_movement",
        entityId = entity.id,
        action = "cash.${type.storage}",
        payload = buildJsonObject { put("amount_cents", signed); put("reason", cleanReason) },
        clinicId = session.clinicId,
      )
    }
    return entity.toDomain()
  }

  suspend fun closeCashSession(sessionId: String, countedCents: Long): CashSession {
    require(countedCents >= 0) { "El efectivo contado no puede ser negativo" }
    val session = cashSessionDao.findById(sessionId) ?: error("La caja no existe")
    require(session.status == CashSessionStatus.Open.storage) { "La caja ya está cerrada" }
    val expected = session.openingCents + cashMovementDao.sumForSession(session.id)
    val now = System.currentTimeMillis()
    val updated =
      session.copy(
        closedAt = now,
        expectedCashCents = expected,
        countedCashCents = countedCents,
        differenceCents = countedCents - expected,
        status = CashSessionStatus.Closed.storage,
        updatedAt = now,
        pendingSync = true,
      )
    database.withTransaction {
      cashSessionDao.upsert(updated)
      outboxDao.insert(outbox("cash_session", updated.id, json.encodeToJsonElement(updated.toDto()), session.revision, now))
      auditRepository.record(
        entity = "cash_session",
        entityId = updated.id,
        action = "cash.close",
        payload =
          buildJsonObject {
            put("expected_cents", expected)
            put("counted_cents", countedCents)
            put("difference_cents", countedCents - expected)
          },
        clinicId = session.clinicId,
      )
    }
    return updated.toDomain()
  }

  suspend fun upsertPaymentMethodFromRemote(dto: PaymentMethodDto, clinicId: String) {
    val existing = paymentMethodDao.findById(dto.id)
    if (existing?.pendingSync == true) return
    paymentMethodDao.upsert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun markPaymentMethodSynced(dto: PaymentMethodDto, clinicId: String) {
    val existing = paymentMethodDao.findById(dto.id)
    paymentMethodDao.upsert(
      dto.toEntity(clinicId, existing?.originDeviceId ?: settings.deviceId).copy(pendingSync = false),
    )
  }

  suspend fun upsertSaleFromRemote(dto: SaleDto, clinicId: String) {
    val existing = saleDao.findById(dto.id)
    if (existing?.pendingSync == true) return
    saleDao.upsert(dto.toEntity(clinicId, settings.deviceId))
    adjustCounter(dto)
  }

  suspend fun upsertSaleItemFromRemote(dto: SaleItemDto, clinicId: String) {
    saleItemDao.insertAll(listOf(dto.toEntity(clinicId, settings.deviceId)))
  }

  suspend fun upsertPaymentFromRemote(dto: PaymentDto, clinicId: String) {
    paymentDao.insertAll(listOf(dto.toEntity(clinicId, settings.deviceId)))
  }

  suspend fun upsertCashSessionFromRemote(dto: CashSessionDto, clinicId: String) {
    val existing = cashSessionDao.findById(dto.id)
    if (existing?.pendingSync == true) return
    cashSessionDao.upsert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun markCashSessionSynced(dto: CashSessionDto, clinicId: String) {
    val existing = cashSessionDao.findById(dto.id)
    cashSessionDao.upsert(
      dto.toEntity(clinicId, existing?.originDeviceId ?: settings.deviceId).copy(pendingSync = false),
    )
  }

  suspend fun upsertCashMovementFromRemote(dto: CashMovementDto, clinicId: String) {
    cashMovementDao.insert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun upsertCancellationFromRemote(dto: SaleCancellationDto, clinicId: String) {
    cancellationDao.insert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun upsertAreaCostFromRemote(dto: AreaCostSettingDto, clinicId: String) {
    val existing = costDao.all().firstOrNull { it.id == dto.id }
    if (existing?.pendingSync == true) return
    costDao.insert(dto.toEntity(clinicId, settings.deviceId))
  }

  private suspend fun nextNumber(clinicId: String, date: LocalDate): String {
    val day = saleDayKey(date)
    val counter = counterDao.find(clinicId, day)
    var number = (counter?.lastNumber ?: 0) + 1
    while (true) {
      val formatted = "V-$day-${number.toString().padStart(4, '0')}"
      if (saleDao.countByNumber(formatted) == 0) {
        counterDao.upsert(
          SaleCounterEntity(
            clinicId = clinicId,
            day = day,
            lastNumber = number,
            updatedAt = System.currentTimeMillis(),
          ),
        )
        return formatted
      }
      number++
    }
  }

  private suspend fun adjustCounter(dto: SaleDto) {
    val day = runCatching {
      java.time.Instant.ofEpochMilli(dto.soldAt.let { java.time.Instant.parse(it).toEpochMilli() })
        .atZone(ClinicTime.zone)
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    }.getOrNull() ?: return
    val suffix = dto.number.substringAfterLast("-").toIntOrNull() ?: return
    val existing = counterDao.find(dto.clinicId ?: settings.currentClinicId, day)
    if (existing == null || existing.lastNumber < suffix) {
      counterDao.upsert(
        SaleCounterEntity(
          clinicId = dto.clinicId ?: settings.currentClinicId,
          day = day,
          lastNumber = suffix,
          updatedAt = System.currentTimeMillis(),
        ),
      )
    }
  }

  private data class ResolvedPayment(
    val method: PaymentMethodEntity,
    val amountCents: Long,
    val tenderedCents: Long?,
    val changeCents: Long?,
    val paymentIdHint: String = "",
  )

  private suspend fun resolvePayments(draft: SaleDraft, totalCents: Long): List<ResolvedPayment> {
    if (totalCents == 0L) {
      require(draft.payments.isEmpty()) { "Una venta sin total no lleva pagos" }
      return emptyList()
    }
    require(draft.payments.isNotEmpty()) { "Agregá al menos un medio de pago" }
    val resolved =
      draft.payments.map { payment ->
        require(payment.amountCents > 0) { "Cada pago debe ser mayor que cero" }
        val method = paymentMethodDao.findById(payment.methodId) ?: error("El medio de pago no existe")
        require(method.deletedAt == null && method.active) { "El medio de pago está inactivo" }
        if (method.affectsCash) {
          val tendered = payment.tenderedCents ?: payment.amountCents
          require(tendered >= payment.amountCents) {
            "El efectivo recibido no alcanza para el importe"
          }
          ResolvedPayment(
            method = method,
            amountCents = payment.amountCents,
            tenderedCents = tendered,
            changeCents = tendered - payment.amountCents,
          )
        } else {
          ResolvedPayment(
            method = method,
            amountCents = payment.amountCents,
            tenderedCents = null,
            changeCents = null,
          )
        }
      }
    val sum = resolved.sumOf { it.amountCents }
    require(sum == totalCents) { "Los pagos deben sumar exactamente el total" }
    return resolved
  }

  private fun outbox(
    entityType: String,
    entityId: String,
    payload: kotlinx.serialization.json.JsonElement,
    baseRevision: Long?,
    now: Long,
  ): OutboxEntity =
    OutboxEntity(
      operationId = UUID.randomUUID().toString(),
      entityType = entityType,
      entityId = entityId,
      operation = "UPSERT",
      payload = payload.toString(),
      baseRevision = baseRevision,
      attempts = 0,
      nextAttemptAt = 0L,
      lastError = null,
      createdAt = now,
    )

  companion object {
    val DEFAULT_METHODS =
      listOf(
        "Efectivo" to true,
        "Tarjeta" to false,
        "Transferencia" to false,
      )
  }
}
