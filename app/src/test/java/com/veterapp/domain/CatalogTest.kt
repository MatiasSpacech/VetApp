package com.veterapp.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
  private fun item(
    id: String = "item-1",
    kind: CatalogKind = CatalogKind.Product,
    price: Long = 1000,
    stock: Int = 5,
    min: Int = 1,
    expires: String? = null,
    areaId: String = "area-1",
  ): CatalogItem =
    CatalogItem(
      id = id,
      clinicId = "clinic-1",
      kind = kind,
      internalCode = "CODE-$id",
      barcode = null,
      name = "Ítem $id",
      description = null,
      categoryId = null,
      categoryName = null,
      brand = null,
      unitCostCents = 500,
      salePriceCents = price,
      trackStock = kind == CatalogKind.Product,
      stockCached = stock,
      stockMin = min,
      areaId = areaId,
      areaName = "Área",
      lot = null,
      expiresOn = expires,
      durationMinutes = null,
      active = true,
      pendingSync = false,
    )

  @Test
  fun productDraftRequiresCostAreaAndPrice() {
    val base =
      CatalogItemDraft(
        kind = CatalogKind.Product,
        internalCode = "",
        barcode = null,
        name = "Alimento",
        description = null,
        categoryId = null,
        brand = null,
        unitCostCents = 100,
        salePriceCents = 200,
        stockMin = 0,
        areaId = "area-1",
        lot = null,
        expiresOn = null,
        durationMinutes = null,
      )
    assertNull(base.validate())
    assertTrue(base.copy(areaId = "").validate()!!.contains("área"))
    assertTrue(base.copy(unitCostCents = null).validate()!!.contains("costo"))
    assertTrue(base.copy(salePriceCents = -1).validate()!!.contains("precio"))
  }

  @Test
  fun serviceCannotHaveLotOrExpiry() {
    val draft =
      CatalogItemDraft(
        kind = CatalogKind.Service,
        internalCode = "",
        barcode = null,
        name = "Consulta",
        description = null,
        categoryId = null,
        brand = null,
        unitCostCents = null,
        salePriceCents = 5000,
        stockMin = 0,
        areaId = "area-1",
        lot = "L1",
        expiresOn = null,
        durationMinutes = 30,
      )
    assertTrue(draft.validate()!!.contains("lote"))
  }

  @Test
  fun stockMovementSignsAndReasons() {
    val entry = StockMovementDraft(StockMovementType.Entry, 5, 100, null)
    assertEquals(5, entry.delta())
    assertNull(entry.validate(true))
    val loss = StockMovementDraft(StockMovementType.Loss, 3, null, "Rotura")
    assertEquals(-3, loss.delta())
    assertNull(loss.validate(true))
    val adjustment = StockMovementDraft(StockMovementType.Adjustment, -2, null, null)
    assertTrue(adjustment.validate(true)!!.contains("motivo"))
    assertTrue(StockMovementDraft(StockMovementType.Entry, 1, null, null).validate(false) != null)
  }

  @Test
  fun lowStockAndExpiryBoundaries() {
    val today = LocalDate.of(2026, 10, 24)
    assertTrue(item(stock = 1, min = 1).isLowStock)
    assertFalse(item(stock = 2, min = 1).isLowStock)
    assertEquals(ExpiryStatus.Expired, item(expires = "2026-10-23").expiryStatus(today))
    assertEquals(ExpiryStatus.ExpiringSoon, item(expires = "2026-10-24").expiryStatus(today))
    assertEquals(ExpiryStatus.ExpiringSoon, item(expires = "2026-11-23").expiryStatus(today))
    assertEquals(ExpiryStatus.Ok, item(expires = "2026-11-24").expiryStatus(today))
  }

  @Test
  fun barcodeNormalizationKeepsOnlyLettersAndDigits() {
    assertEquals("ABC123", normalizeBarcode(" ABC-123 "))
    assertNull(normalizeBarcode("  "))
  }

  @Test
  fun categoryScopeFiltersItems() {
    assertTrue(CategoryScope.Both.accepts(CatalogKind.Product))
    assertTrue(CategoryScope.Both.accepts(CatalogKind.Service))
    assertTrue(CategoryScope.Product.accepts(CatalogKind.Product))
    assertFalse(CategoryScope.Product.accepts(CatalogKind.Service))
  }
}
