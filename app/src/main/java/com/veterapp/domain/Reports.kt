package com.veterapp.domain

import java.time.LocalDate
import java.time.YearMonth

data class PeriodMetrics(
  val from: LocalDate,
  val to: LocalDate,
  val grossCents: Long,
  val discountCents: Long,
  val netCents: Long,
  val productCostCents: Long,
  val serviceCostCents: Long,
  val expensesCents: Long,
  val salesCount: Int,
  val cancelledCount: Int,
) {
  val resultCents: Long
    get() = computeEstimatedResult(netCents, productCostCents, serviceCostCents, expensesCents)

  val ticketAverageCents: Long?
    get() = if (salesCount == 0) null else netCents / salesCount
}

data class AreaMetrics(
  val areaId: String,
  val areaName: String,
  val grossCents: Long,
  val discountCents: Long,
  val netCents: Long,
  val productCostCents: Long,
  val serviceCostCents: Long,
  val expensesCents: Long,
  val totalNetCents: Long,
) {
  val resultCents: Long
    get() = computeEstimatedResult(netCents, productCostCents, serviceCostCents, expensesCents)

  val sharePercent: Double
    get() = if (totalNetCents == 0L) 0.0 else netCents * 100.0 / totalNetCents
}

data class PaymentMethodMetrics(
  val methodId: String,
  val methodName: String,
  val amountCents: Long,
  val saleCount: Int,
)

data class TopItemMetrics(
  val itemId: String,
  val itemName: String,
  val kind: CatalogKind,
  val quantity: Long,
  val netCents: Long,
)

data class DailySeriesPoint(
  val date: LocalDate,
  val netCents: Long,
  val resultCents: Long,
  val salesCount: Int,
)

data class StatusCounts(val counts: Map<String, Int>) {
  fun of(status: String): Int = counts[status] ?: 0

  val total: Int
    get() = counts.values.sum()
}

data class InventoryAlerts(
  val lowStock: List<CatalogItem>,
  val expiring: List<CatalogItem>,
  val expired: List<CatalogItem>,
)

data class VaccineReport(
  val overdue: Int,
  val dueSoon: Int,
  val upcoming: Int,
)

data class DashboardData(
  val today: PeriodMetrics,
  val month: PeriodMetrics,
  val year: PeriodMetrics,
  val yesterday: PeriodMetrics,
  val previousMonth: PeriodMetrics,
  val previousYear: PeriodMetrics,
  val todayAppointments: List<Appointment>,
  val appointmentCounts: StatusCounts,
  val lowStock: List<CatalogItem>,
  val expiring: List<CatalogItem>,
  val expired: List<CatalogItem>,
  val vaccines: VaccineReport,
  val newClients: Int,
  val newPets: Int,
  val openCash: CashSession?,
  val topAreas: List<AreaMetrics>,
)

object ReportPeriods {
  fun day(date: LocalDate = ClinicTime.today()): Pair<Long, Long> =
    ClinicTime.startOfDay(date.toEpochDay()) to ClinicTime.startOfDay(date.toEpochDay() + 1)

  fun month(date: LocalDate = ClinicTime.today()): Pair<Long, Long> {
    val yearMonth = YearMonth.from(date)
    return ClinicTime.startOfDay(yearMonth.atDay(1).toEpochDay()) to
      ClinicTime.startOfDay(yearMonth.plusMonths(1).atDay(1).toEpochDay())
  }

  fun year(date: LocalDate = ClinicTime.today()): Pair<Long, Long> =
    ClinicTime.startOfDay(LocalDate.of(date.year, 1, 1).toEpochDay()) to
      ClinicTime.startOfDay(LocalDate.of(date.year + 1, 1, 1).toEpochDay())

  fun range(from: LocalDate, to: LocalDate): Pair<Long, Long> =
    ClinicTime.startOfDay(from.toEpochDay()) to ClinicTime.startOfDay(to.toEpochDay() + 1)

  fun previousPeriod(from: LocalDate, to: LocalDate): Pair<LocalDate, LocalDate> {
    val days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1
    return from.minusDays(days) to from.minusDays(1)
  }

  fun sameRangePreviousYear(from: LocalDate, to: LocalDate): Pair<LocalDate, LocalDate> =
    from.minusYears(1) to to.minusYears(1)
}

fun formatVariation(current: Long, previous: Long): String {
  if (previous == 0L) return if (current == 0L) "sin datos" else "sin base"
  val variation = (current - previous) * 100.0 / kotlin.math.abs(previous)
  val sign = if (variation >= 0) "+" else ""
  return String.format(java.util.Locale("es", "AR"), "%s%.1f%%", sign, variation)
}
