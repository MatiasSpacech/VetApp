package com.veterapp.data

import com.veterapp.data.local.AreaTotalsRow
import com.veterapp.data.local.CashSessionEntity
import com.veterapp.data.local.MethodTotalsRow
import com.veterapp.data.local.ReportDao
import com.veterapp.data.local.StatusCountRow
import com.veterapp.data.local.TotalsRow
import com.veterapp.data.local.VetDatabase
import com.veterapp.domain.Appointment
import com.veterapp.domain.AreaMetrics
import com.veterapp.domain.CatalogItem
import com.veterapp.domain.CatalogKind
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.DailySeriesPoint
import com.veterapp.domain.DashboardData
import com.veterapp.domain.ExpiryStatus
import com.veterapp.domain.InventoryAlerts
import com.veterapp.domain.PaymentMethodMetrics
import com.veterapp.domain.PeriodMetrics
import com.veterapp.domain.ReportPeriods
import com.veterapp.domain.Sale
import com.veterapp.domain.StatusCounts
import com.veterapp.domain.TopItemMetrics
import com.veterapp.domain.VaccineReport
import com.veterapp.domain.VaccinationReminderCandidate
import com.veterapp.domain.formatMoney
import com.veterapp.domain.latestCandidatesPerPetAndVaccine
import com.veterapp.domain.toDomain
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ReportType(val title: String) {
  Sales("Facturación"),
  Areas("Ventas por área"),
  Costs("Costos y resultado"),
  Payments("Medios de pago"),
  TopItems("Más vendidos"),
  Inventory("Inventario"),
  Appointments("Turnos"),
  Vaccines("Vacunaciones"),
  ClientsPets("Clientes y mascotas"),
  Cash("Caja"),
  Evolution("Evolución"),
}

data class ReportTableRow(
  val cells: List<String>,
  val saleId: String? = null,
  val itemId: String? = null,
  val petId: String? = null,
  val appointmentId: String? = null,
  val sessionId: String? = null,
)

data class ReportFilters(
  val areaId: String? = null,
  val methodId: String? = null,
  val kind: CatalogKind? = null,
) {
  val isEmpty: Boolean
    get() = areaId == null && methodId == null && kind == null
}

data class ReportResult(
  val type: ReportType,
  val periodLabel: String,
  val summary: List<Pair<String, String>>,
  val columns: List<String>,
  val rows: List<ReportTableRow>,
  val emptyMessage: String = "Sin datos para el período",
) {
  fun toCsv(): String {
    val builder = StringBuilder("\uFEFF")
    builder.append(type.title).append('\n')
    builder.append(periodLabel).append('\n').append('\n')
    summary.forEach { (label, value) ->
      builder.append(escape(label)).append(';').append(escape(value)).append('\n')
    }
    builder.append('\n')
    builder.append(columns.joinToString(";") { escape(it) }).append('\n')
    rows.forEach { row ->
      builder.append(row.cells.joinToString(";") { escape(it) }).append('\n')
    }
    return builder.toString()
  }

  private fun escape(value: String): String {
    val clean = value.replace("\"", "\"\"")
    return if (clean.contains(';') || clean.contains('"') || clean.contains('\n')) "\"$clean\"" else clean
  }
}

class ReportRepository(
  private val database: VetDatabase,
  private val settings: AppSettings,
) {
  private val reportDao: ReportDao = database.reportDao()

  fun observeRecentCashSessions(): Flow<List<CashSessionEntity>> =
    reportDao.observeRecentCashSessions()

  suspend fun dashboard(): DashboardData {
    val today = ClinicTime.today()
    val (todayFrom, todayTo) = ReportPeriods.day(today)
    val (monthFrom, monthTo) = ReportPeriods.month(today)
    val (yearFrom, yearTo) = ReportPeriods.year(today)
    val (yesterdayFrom, yesterdayTo) = ReportPeriods.day(today.minusDays(1))
    val (previousMonthFrom, previousMonthTo) = ReportPeriods.previousPeriod(
      java.time.LocalDate.of(today.year, today.month, 1),
      today,
    )
    val (previousYearFrom, previousYearTo) =
      ReportPeriods.sameRangePreviousYear(
        java.time.LocalDate.of(today.year, 1, 1),
        today,
      )
    val (previousMonthMillisFrom, previousMonthMillisTo) =
      ReportPeriods.range(previousMonthFrom, previousMonthTo)
    val (previousYearMillisFrom, previousYearMillisTo) =
      ReportPeriods.range(previousYearFrom, previousYearTo)
    val areaRows = reportDao.areaTotals(todayFrom, todayTo, null, null, null)
    val totalNet = areaRows.sumOf { it.netCents }
    val items = catalogItems()
    val vaccines = vaccineReport()
    val counts = reportDao.appointmentCounts(todayFrom, todayTo)
    return DashboardData(
      today = metrics(todayFrom, todayTo, today, today),
      month = metrics(monthFrom, monthTo, today.withDayOfMonth(1), today),
      year = metrics(yearFrom, yearTo, LocalDate.of(today.year, 1, 1), today),
      yesterday = metrics(yesterdayFrom, yesterdayTo, today.minusDays(1), today.minusDays(1)),
      previousMonth = metrics(previousMonthMillisFrom, previousMonthMillisTo, previousMonthFrom, previousMonthTo),
      previousYear = metrics(previousYearMillisFrom, previousYearMillisTo, previousYearFrom, previousYearTo),
      todayAppointments = database.appointmentDao().between(todayFrom, todayTo).map { it.toDomain() },
      appointmentCounts = StatusCounts(counts.associate { it.status to it.total }),
      lowStock = items.filter { it.isLowStock },
      expiring = items.filter { it.expiryStatus() == ExpiryStatus.ExpiringSoon },
      expired = items.filter { it.expiryStatus() == ExpiryStatus.Expired },
      vaccines = vaccines,
      newClients = reportDao.newClients(monthFrom, monthTo),
      newPets = reportDao.newPets(monthFrom, monthTo),
      openCash = database.cashSessionDao().findOpen()?.toDomain(),
      topAreas =
        areaRows.map { row ->
          AreaMetrics(
            areaId = row.areaId,
            areaName = row.areaName,
            grossCents = row.grossCents,
            discountCents = row.discountCents,
            netCents = row.netCents,
            productCostCents = row.productCostCents,
            serviceCostCents = row.serviceCostCents,
            expensesCents = row.expensesCents,
            totalNetCents = totalNet,
          )
        },
    )
  }

  suspend fun report(
    type: ReportType,
    from: LocalDate,
    to: LocalDate,
    filters: ReportFilters = ReportFilters(),
  ): ReportResult {
    val (fromMillis, toMillis) = ReportPeriods.range(from, to)
    val label = "${formatLocalDate(from)} al ${formatLocalDate(to)}"
    return when (type) {
      ReportType.Sales -> salesReport(fromMillis, toMillis, label, filters)
      ReportType.Areas -> areasReport(fromMillis, toMillis, label, filters)
      ReportType.Costs -> costsReport(fromMillis, toMillis, label, filters)
      ReportType.Payments -> paymentsReport(fromMillis, toMillis, label, filters)
      ReportType.TopItems -> topItemsReport(fromMillis, toMillis, label, filters)
      ReportType.Inventory -> inventoryReport(label, filters)
      ReportType.Appointments -> appointmentsReport(fromMillis, toMillis, label)
      ReportType.Vaccines -> vaccinesReport(label)
      ReportType.ClientsPets -> clientsPetsReport(fromMillis, toMillis, label)
      ReportType.Cash -> cashReport(fromMillis, toMillis, label)
      ReportType.Evolution -> evolutionReport(fromMillis, toMillis, label, filters)
    }
  }

  suspend fun metricsFor(from: LocalDate, to: LocalDate): PeriodMetrics {
    val (fromMillis, toMillis) = ReportPeriods.range(from, to)
    return metrics(fromMillis, toMillis, from, to)
  }

  private suspend fun metrics(
    fromMillis: Long,
    toMillis: Long,
    fromDate: LocalDate,
    toDate: LocalDate,
    filters: ReportFilters = ReportFilters(),
  ): PeriodMetrics {
    val totals =
      reportDao.totals(
        fromMillis,
        toMillis,
        filters.areaId,
        filters.methodId,
        filters.kind?.storage,
      )
    return PeriodMetrics(
      from = fromDate,
      to = toDate,
      grossCents = totals.grossCents,
      discountCents = totals.discountCents,
      netCents = totals.netCents,
      productCostCents = totals.productCostCents,
      serviceCostCents = totals.serviceCostCents,
      expensesCents = totals.expensesCents,
      salesCount =
        reportDao.completedCount(
          fromMillis,
          toMillis,
          filters.areaId,
          filters.methodId,
          filters.kind?.storage,
        ),
      cancelledCount =
        reportDao.cancelledCount(
          fromMillis,
          toMillis,
          filters.areaId,
          filters.methodId,
          filters.kind?.storage,
        ),
    )
  }

  private suspend fun salesReport(
    from: Long,
    to: Long,
    label: String,
    filters: ReportFilters,
  ): ReportResult {
    val totals = metrics(from, to, ClinicTime.dateOf(from), ClinicTime.dateOf(to - 1), filters)
    val sales = filteredSales(from, to, filters)
    return ReportResult(
      type = ReportType.Sales,
      periodLabel = label,
      summary =
        listOf(
          "Facturación bruta" to formatMoney(totals.grossCents),
          "Descuentos" to formatMoney(totals.discountCents),
          "Facturación neta" to formatMoney(totals.netCents),
          "Operaciones" to totals.salesCount.toString(),
          "Ticket promedio" to (totals.ticketAverageCents?.let(::formatMoney) ?: "sin datos"),
          "Anuladas" to totals.cancelledCount.toString(),
        ),
      columns = listOf("Número", "Fecha", "Cliente", "Estado", "Total"),
      rows =
        sales.map { sale ->
          ReportTableRow(
            cells =
              listOf(
                sale.number,
                formatSaleDateTimeForReport(sale.soldAt),
                sale.clientName ?: "Sin cliente",
                sale.status.label,
                formatMoney(sale.totalCents),
              ),
            saleId = sale.id,
          )
        },
      emptyMessage = "No hay ventas completadas en el período",
    )
  }

  private suspend fun areasReport(
    from: Long,
    to: Long,
    label: String,
    filters: ReportFilters,
  ): ReportResult {
    val rows = reportDao.areaTotals(from, to, filters.areaId, filters.methodId, filters.kind?.storage)
    val total = rows.sumOf { it.netCents }
    return ReportResult(
      type = ReportType.Areas,
      periodLabel = label,
      summary =
        listOf(
          "Facturación neta" to formatMoney(total),
          "Áreas con ventas" to rows.size.toString(),
        ),
      columns = listOf("Área", "Facturación neta", "Participación"),
      rows =
        rows.map { row ->
          ReportTableRow(
            cells =
              listOf(
                row.areaName,
                formatMoney(row.netCents),
                if (total == 0L) "0,0%" else String.format(java.util.Locale("es", "AR"), "%.1f%%", row.netCents * 100.0 / total),
              ),
          )
        },
      emptyMessage = "No hay ventas por área en el período",
    )
  }

  private suspend fun costsReport(
    from: Long,
    to: Long,
    label: String,
    filters: ReportFilters,
  ): ReportResult {
    val rows = reportDao.areaTotals(from, to, filters.areaId, filters.methodId, filters.kind?.storage)
    val total = rows.sumOf { it.netCents }
    return ReportResult(
      type = ReportType.Costs,
      periodLabel = label,
      summary =
        listOf(
          "Facturación neta" to formatMoney(total),
          "Resultado estimado" to
            formatMoney(
              rows.sumOf {
                it.netCents - it.productCostCents - it.serviceCostCents - it.expensesCents
              },
            ),
        ),
      columns = listOf("Área", "Costo productos", "Costos directos", "Gastos estimados", "Resultado estimado"),
      rows =
        rows.map { row ->
          ReportTableRow(
            cells =
              listOf(
                row.areaName,
                formatMoney(row.productCostCents),
                formatMoney(row.serviceCostCents),
                formatMoney(row.expensesCents),
                formatMoney(row.netCents - row.productCostCents - row.serviceCostCents - row.expensesCents),
              ),
          )
        },
      emptyMessage = "No hay costos para el período",
    )
  }

  private suspend fun paymentsReport(
    from: Long,
    to: Long,
    label: String,
    filters: ReportFilters,
  ): ReportResult {
    val rows = reportDao.methodTotals(from, to, filters.areaId, filters.methodId, filters.kind?.storage)
    return ReportResult(
      type = ReportType.Payments,
      periodLabel = label,
      summary = listOf("Total cobrado" to formatMoney(rows.sumOf { it.amountCents })),
      columns = listOf("Medio de pago", "Importe", "Operaciones"),
      rows =
        rows.map { row ->
          ReportTableRow(cells = listOf(row.methodName, formatMoney(row.amountCents), row.saleCount.toString()))
        },
      emptyMessage = "No hay pagos registrados en el período",
    )
  }

  private suspend fun topItemsReport(
    from: Long,
    to: Long,
    label: String,
    filters: ReportFilters,
  ): ReportResult {
    val rows =
      reportDao.topItems(
        from,
        to,
        filters.areaId,
        filters.methodId,
        filters.kind?.storage,
        50,
      )
    return ReportResult(
      type = ReportType.TopItems,
      periodLabel = label,
      summary = listOf("Ítems con ventas" to rows.size.toString()),
      columns = listOf("Ítem", "Tipo", "Cantidad", "Facturación neta"),
      rows =
        rows.map { row ->
          ReportTableRow(
            cells =
              listOf(
                row.itemName,
                CatalogKind.fromStorage(row.itemKind).label,
                row.quantity.toString(),
                formatMoney(row.netCents),
              ),
            itemId = row.itemId,
          )
        },
      emptyMessage = "No hay ítems vendidos en el período",
    )
  }

  private suspend fun inventoryReport(label: String, filters: ReportFilters): ReportResult {
    val items =
      catalogItems()
        .filter { filters.areaId == null || it.areaId == filters.areaId }
        .filter { filters.kind == null || it.kind == filters.kind }
    return ReportResult(
      type = ReportType.Inventory,
      periodLabel = label,
      summary =
        listOf(
          "Productos activos" to items.count { it.kind == CatalogKind.Product && it.active }.toString(),
          "Stock bajo" to items.count { it.isLowStock }.toString(),
          "Vencidos" to items.count { it.expiryStatus() == ExpiryStatus.Expired }.toString(),
          "Próximos a vencer" to items.count { it.expiryStatus() == ExpiryStatus.ExpiringSoon }.toString(),
        ),
      columns = listOf("Ítem", "Área", "Stock", "Mínimo", "Vencimiento", "Estado"),
      rows =
        items.map { item ->
          ReportTableRow(
            cells =
              listOf(
                item.name,
                item.areaName,
                item.stockCached.toString(),
                item.stockMin.toString(),
                item.expiresOn ?: "—",
                when {
                  item.expiryStatus() == ExpiryStatus.Expired -> "Vencido"
                  item.expiryStatus() == ExpiryStatus.ExpiringSoon -> "Próximo a vencer"
                  item.isLowStock -> "Stock bajo"
                  item.active -> "Activo"
                  else -> "Inactivo"
                },
              ),
            itemId = item.id,
          )
        },
      emptyMessage = "No hay productos en el inventario",
    )
  }

  private suspend fun appointmentsReport(from: Long, to: Long, label: String): ReportResult {
    val counts = reportDao.appointmentCounts(from, to)
    val appointments = database.appointmentDao().between(from, to).map { it.toDomain() }
    return ReportResult(
      type = ReportType.Appointments,
      periodLabel = label,
      summary =
        listOf(
          "Programados" to (counts.firstOrNull { it.status == "scheduled" }?.total ?: 0).toString(),
          "Confirmados" to (counts.firstOrNull { it.status == "confirmed" }?.total ?: 0).toString(),
          "Atendidos" to (counts.firstOrNull { it.status == "attended" }?.total ?: 0).toString(),
          "Cancelados" to (counts.firstOrNull { it.status == "cancelled" }?.total ?: 0).toString(),
          "Ausentes" to (counts.firstOrNull { it.status == "no_show" }?.total ?: 0).toString(),
        ),
      columns = listOf("Fecha", "Hora", "Mascota", "Propietario", "Estado"),
      rows =
        appointments.map { appointment ->
          ReportTableRow(
            cells =
              listOf(
                formatDate(appointment.startsAt),
                formatTimeForReport(appointment.startsAt),
                appointment.petName,
                appointment.ownerName,
                appointment.status.label,
              ),
            appointmentId = appointment.id,
          )
        },
      emptyMessage = "No hay turnos en el período",
    )
  }

  private suspend fun vaccinesReport(label: String): ReportResult {
    val report = vaccineReport()
    val rows = vaccinationRows()
    return ReportResult(
      type = ReportType.Vaccines,
      periodLabel = label,
      summary =
        listOf(
          "Vencidas" to report.overdue.toString(),
          "Por vencer" to report.dueSoon.toString(),
          "Próximas" to report.upcoming.toString(),
        ),
      columns = listOf("Mascota", "Vacuna", "Próxima dosis", "Estado"),
      rows = rows,
      emptyMessage = "No hay vacunaciones con próxima dosis",
    )
  }

  private suspend fun clientsPetsReport(from: Long, to: Long, label: String): ReportResult {
    val clients = reportDao.newClients(from, to)
    val pets = reportDao.newPets(from, to)
    return ReportResult(
      type = ReportType.ClientsPets,
      periodLabel = label,
      summary =
        listOf(
          "Clientes nuevos" to clients.toString(),
          "Mascotas registradas" to pets.toString(),
        ),
      columns = listOf("Indicador", "Cantidad"),
      rows =
        listOf(
          ReportTableRow(cells = listOf("Clientes nuevos", clients.toString())),
          ReportTableRow(cells = listOf("Mascotas registradas", pets.toString())),
        ),
      emptyMessage = "Sin altas en el período",
    )
  }

  private suspend fun cashReport(from: Long, to: Long, label: String): ReportResult {
    val sessions = reportDao.cashSessions(from, to)
    val open = database.cashSessionDao().findOpen()
    val all = if (open != null && sessions.none { it.id == open.id }) sessions + open else sessions
    return ReportResult(
      type = ReportType.Cash,
      periodLabel = label,
      summary =
        listOf(
          "Cajas" to all.size.toString(),
          "Diferencia total" to formatMoney(all.sumOf { it.differenceCents ?: 0L }),
        ),
      columns = listOf("Apertura", "Estado", "Inicial", "Esperado", "Contado", "Diferencia"),
      rows =
        all.map { session ->
          ReportTableRow(
            cells =
              listOf(
                formatSaleDateTimeForReport(session.openedAt),
                if (session.status == "open") "Abierta" else "Cerrada",
                formatMoney(session.openingCents),
                session.expectedCashCents?.let(::formatMoney) ?: "—",
                session.countedCashCents?.let(::formatMoney) ?: "—",
                session.differenceCents?.let(::formatMoney) ?: "—",
              ),
            sessionId = session.id,
          )
        },
      emptyMessage = "No hay cajas en el período",
    )
  }

  private suspend fun evolutionReport(
    from: Long,
    to: Long,
    label: String,
    filters: ReportFilters,
  ): ReportResult {
    val rows = reportDao.dailySeries(from, to, filters.areaId, filters.methodId, filters.kind?.storage)
    return ReportResult(
      type = ReportType.Evolution,
      periodLabel = label,
      summary = listOf("Días con ventas" to rows.size.toString()),
      columns = listOf("Día", "Facturación neta", "Resultado estimado", "Operaciones"),
      rows =
        rows.map { row ->
          ReportTableRow(
            cells =
              listOf(
                row.day,
                formatMoney(row.netCents),
                formatMoney(row.netCents - row.productCostCents - row.serviceCostCents - row.expensesCents),
                row.saleCount.toString(),
              ),
          )
        },
      emptyMessage = "No hay evolución para el período",
    )
  }

  private suspend fun filteredSales(
    from: Long,
    to: Long,
    filters: ReportFilters,
  ): List<Sale> {
    val rows = database.saleDao().completedBetween(from, to)
    if (filters.isEmpty) return rows.map { it.toDomain() }
    val ids = rows.map { it.id }
    if (ids.isEmpty()) return emptyList()
    val items = database.saleItemDao().forSales(ids)
    val payments = database.paymentDao().forSales(ids)
    val matching =
      ids.filter { saleId ->
        val saleItems = items.filter { it.saleId == saleId }
        val salePayments = payments.filter { it.saleId == saleId }
        (filters.areaId == null || saleItems.any { it.areaId == filters.areaId }) &&
          (filters.kind == null || saleItems.any { it.itemKind == filters.kind.storage }) &&
          (filters.methodId == null || salePayments.any { it.methodId == filters.methodId })
      }.toSet()
    return rows.filter { it.id in matching }.map { it.toDomain() }
  }

  private suspend fun catalogItems(): List<CatalogItem> {
    val items = database.catalogItemDao().all().filter { it.deletedAt == null }
    val areas = database.areaDao().all().associate { it.id to it.name }
    val categories = database.categoryDao().all().associate { it.id to it.name }
    return items.map {
      it.toDomain(
        areaName = areas[it.areaId] ?: "Sin área",
        categoryName = it.categoryId?.let { id -> categories[id] },
      )
    }
  }

  private suspend fun vaccineReport(): VaccineReport {
    val rows = database.vaccinationDao().activeWithPet()
    val today = ClinicTime.today()
    var overdue = 0
    var dueSoon = 0
    var upcoming = 0
    rows
      .map { VaccinationReminderCandidate(it.vaccination.toDomain(), it.petName) }
      .latestCandidatesPerPetAndVaccine()
      .forEach { candidate ->
        val dueOn = candidate.vaccination.nextDueOn ?: return@forEach
        val days = dueOn - today.toEpochDay()
        when {
          days < 0 -> overdue++
          days <= settings.reminderWeekDays -> dueSoon++
          days <= settings.reminderMonthDays -> upcoming++
        }
      }
    return VaccineReport(overdue = overdue, dueSoon = dueSoon, upcoming = upcoming)
  }

  private suspend fun vaccinationRows(): List<ReportTableRow> {
    val rows = database.vaccinationDao().activeWithPet()
    val today = ClinicTime.today()
    return rows
      .map { VaccinationReminderCandidate(it.vaccination.toDomain(), it.petName) }
      .latestCandidatesPerPetAndVaccine()
      .mapNotNull { candidate ->
        val dueOn = candidate.vaccination.nextDueOn ?: return@mapNotNull null
        val days = dueOn - today.toEpochDay()
        val status =
          when {
            days < 0 -> "Vencida"
            days <= settings.reminderWeekDays -> "Por vencer"
            days <= settings.reminderMonthDays -> "Próxima"
            else -> "Programada"
          }
        ReportTableRow(
          cells = listOf(candidate.petName, candidate.vaccination.vaccineName, formatEpochDayForReport(dueOn), status),
          petId = candidate.vaccination.petId,
        )
      }
  }

  private fun formatDate(timestamp: Long): String =
    java.time.Instant.ofEpochMilli(timestamp)
      .atZone(ClinicTime.zone)
      .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

  private fun formatLocalDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

  private fun formatTimeForReport(timestamp: Long): String =
    java.time.Instant.ofEpochMilli(timestamp)
      .atZone(ClinicTime.zone)
      .format(DateTimeFormatter.ofPattern("HH:mm"))

  private fun formatSaleDateTimeForReport(timestamp: Long): String =
    java.time.Instant.ofEpochMilli(timestamp)
      .atZone(ClinicTime.zone)
      .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

  private fun formatEpochDayForReport(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}
