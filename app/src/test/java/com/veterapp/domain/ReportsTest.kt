package com.veterapp.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportsTest {
  @Test
  fun previousPeriodUsesSameLength() {
    val (from, to) = ReportPeriods.previousPeriod(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 24))
    assertEquals(LocalDate.of(2026, 9, 7), from)
    assertEquals(LocalDate.of(2026, 9, 30), to)
  }

  @Test
  fun variationHandlesZeroBase() {
    assertEquals("sin datos", formatVariation(0, 0))
    assertEquals("sin base", formatVariation(100, 0))
    assertEquals("+50,0%", formatVariation(150, 100))
    assertEquals("-50,0%", formatVariation(50, 100))
  }

  @Test
  fun statusCountsSumValues() {
    val counts = StatusCounts(mapOf("scheduled" to 2, "attended" to 3))
    assertEquals(2, counts.of("scheduled"))
    assertEquals(0, counts.of("cancelled"))
    assertEquals(5, counts.total)
  }

  @Test
  fun periodMetricsComputesResultAndTicket() {
    val metrics =
      PeriodMetrics(
        from = LocalDate.of(2026, 10, 1),
        to = LocalDate.of(2026, 10, 24),
        grossCents = 10000,
        discountCents = 1000,
        netCents = 9000,
        productCostCents = 2000,
        serviceCostCents = 500,
        expensesCents = 1000,
        salesCount = 3,
        cancelledCount = 1,
      )
    assertEquals(5500, metrics.resultCents)
    assertEquals(3000L, metrics.ticketAverageCents)
  }

  @Test
  fun areaShareIsZeroWithoutSales() {
    val area =
      AreaMetrics("a", "Farmacia", 0, 0, 0, 0, 0, 0, 0)
    assertEquals(0.0, area.sharePercent, 0.0)
  }
}
