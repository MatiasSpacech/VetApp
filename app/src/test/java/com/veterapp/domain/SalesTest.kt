package com.veterapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesTest {
  private fun item(id: String, price: Long, area: String = "area-1"): CatalogItem =
    CatalogItem(
      id = id,
      clinicId = "clinic-1",
      kind = CatalogKind.Product,
      internalCode = "C-$id",
      barcode = null,
      name = "Ítem $id",
      description = null,
      categoryId = null,
      categoryName = null,
      brand = null,
      unitCostCents = 100,
      salePriceCents = price,
      trackStock = true,
      stockCached = 10,
      stockMin = 0,
      areaId = area,
      areaName = area,
      lot = null,
      expiresOn = null,
      durationMinutes = null,
      active = true,
      pendingSync = false,
    )

  @Test
  fun calculatesTotalsWithLineAndGlobalDiscounts() {
    val lines =
      listOf(
        item("a", 1000) to SaleLineDraft("a", 2, 200),
        item("b", 500) to SaleLineDraft("b", 1, 0),
      )
    val totals = calculateSaleTotals(lines, globalDiscountCents = 300, expensesByArea = mapOf("area-1" to 1000))
    assertEquals(2500, totals.subtotalCents)
    assertEquals(500, totals.discountCents)
    assertEquals(2000, totals.totalCents)
    assertEquals(1000, totals.lines[0].expensesPercentBp)
  }

  @Test
  fun globalDiscountProratesAndLastLineGetsRounding() {
    val lines =
      listOf(
        item("a", 333) to SaleLineDraft("a", 1),
        item("b", 333) to SaleLineDraft("b", 1),
        item("c", 334) to SaleLineDraft("c", 1),
      )
    val totals = calculateSaleTotals(lines, globalDiscountCents = 100, expensesByArea = emptyMap())
    assertEquals(100, totals.discountCents)
    assertEquals(900, totals.totalCents)
    assertEquals(100, totals.lines.sumOf { it.globalDiscountCents })
  }

  @Test
  fun globalDiscountCannotExceedAvailableAmount() {
    val lines = listOf(item("a", 1000) to SaleLineDraft("a", 1, 600))
    val totals = calculateSaleTotals(lines, globalDiscountCents = 900, expensesByArea = emptyMap())
    assertEquals(1000, totals.discountCents)
    assertEquals(0, totals.totalCents)
  }

  @Test
  fun saleDraftValidation() {
    val empty = SaleDraft(null, emptyList(), 0, emptyList(), null)
    assertTrue(empty.validate()!!.contains("ítem"))
    val invalidQuantity = SaleDraft(null, listOf(SaleLineDraft("a", 0)), 0, emptyList(), null)
    assertTrue(invalidQuantity.validate()!!.contains("cantidad"))
  }

  @Test
  fun parsesMoneyAndPercentInLocalFormat() {
    assertEquals(12345L, parseMoneyToCents("123,45"))
    assertEquals(123456L, parseMoneyToCents("$1.234,56"))
    assertNull(parseMoneyToCents("abc"))
    assertEquals(2150, parsePercentToBp("21,5"))
    assertNull(parsePercentToBp("101"))
    assertEquals("21,50%", formatPercentBp(2150))
  }

  @Test
  fun estimatedResultUsesFrozenValues() {
    assertEquals(500, computeEstimatedResult(2000, 800, 200, 500))
  }

  @Test
  fun effectiveAreaCostUsesLatestApplicableDate() {
    val settings =
      listOf(
        AreaCostSetting("1", "area-1", 1000, "2026-01-01", false),
        AreaCostSetting("2", "area-1", 2000, "2026-06-01", false),
        AreaCostSetting("3", "area-1", 3000, "2027-01-01", false),
      )
    assertEquals(2000, ensureEffectivePercent(settings, "area-1", java.time.LocalDate.of(2026, 10, 24))!!.expensesPercentBp)
    assertNull(ensureEffectivePercent(settings, "area-1", java.time.LocalDate.of(2025, 12, 31)))
  }
}
