package com.veterapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veterapp.data.local.VetDatabase
import com.veterapp.domain.CatalogItemDraft
import com.veterapp.domain.CatalogKind
import com.veterapp.domain.PaymentDraft
import com.veterapp.domain.SaleDraft
import com.veterapp.domain.SaleLineDraft
import com.veterapp.domain.SaleStatus
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommerceRepositoryTest {
  @Test
  fun createsSaleSplitsPaymentsCancelsAndClosesCash() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val database = Room.inMemoryDatabaseBuilder(context, VetDatabase::class.java).build()
      val settings = AppSettings(context)
      settings.clinicId = "local-clinic"
      settings.userId = null
      settings.allowNegativeStock = false
      val audit = AuditRepository(database, settings)
      val catalog = CatalogRepository(database, settings, audit)
      val commerce = CommerceRepository(database, settings, audit)
      val clinicId = "local-clinic"
      catalog.ensureDefaults(clinicId)
      commerce.ensureDefaults(clinicId)

      val areas = catalog.areas.first().associateBy { it.name }
      val productArea = areas.getValue("Farmacia")
      val serviceArea = areas.getValue("Consultorio")
      val product =
        catalog.createItem(
          CatalogItemDraft(
            kind = CatalogKind.Product,
            internalCode = "",
            barcode = "123456",
            name = "Antiparasitario",
            description = null,
            categoryId = null,
            brand = null,
            unitCostCents = 3000,
            salePriceCents = 5000,
            stockMin = 2,
            areaId = productArea.id,
            lot = "L-1",
            expiresOn = "2027-12-31",
            durationMinutes = null,
            initialStock = 10,
          ),
        )
      val service =
        catalog.createItem(
          CatalogItemDraft(
            kind = CatalogKind.Service,
            internalCode = "",
            barcode = null,
            name = "Consulta",
            description = null,
            categoryId = null,
            brand = null,
            unitCostCents = 1000,
            salePriceCents = 8000,
            stockMin = 0,
            areaId = serviceArea.id,
            lot = null,
            expiresOn = null,
            durationMinutes = 30,
          ),
        )
      assertEquals(10, product.stockCached)

      val methods = commerce.activePaymentMethods.first().associateBy { it.name }
      val cashMethod = methods.getValue("Efectivo")
      val cardMethod = methods.getValue("Tarjeta")
      val session = commerce.openCashSession(10_000, null)

      val sale =
        commerce.createSale(
          SaleDraft(
            clientId = null,
            lines =
              listOf(
                SaleLineDraft(product.id, 2, 0),
                SaleLineDraft(service.id, 1, 0),
              ),
            globalDiscountCents = 1000,
            payments =
              listOf(
                PaymentDraft(cashMethod.id, 7_000, 10_000),
                PaymentDraft(cardMethod.id, 10_000),
              ),
            notes = null,
          ),
        )

      assertEquals(18_000L, sale.sale.subtotalCents)
      assertEquals(1_000L, sale.sale.discountCents)
      assertEquals(17_000L, sale.sale.totalCents)
      assertEquals(8, catalog.items.first().first { it.id == product.id }.stockCached)
      assertEquals(3_000L, sale.payments.first { it.methodId == cashMethod.id }.changeCents)
      val expectedAfterSale = commerce.observeCashDetail(session.id).first()!!.expectedCashCents
      assertEquals(17_000L, expectedAfterSale)

      val reports = ReportRepository(database, settings)
      val today = com.veterapp.domain.ClinicTime.today()
      val allReport = reports.report(ReportType.Sales, today, today)
      assertEquals(1, allReport.rows.size)
      val productReport =
        reports.report(
          ReportType.Sales,
          today,
          today,
          ReportFilters(areaId = productArea.id),
        )
      assertEquals(1, productReport.rows.size)
      val serviceOnlyReport =
        reports.report(
          ReportType.Sales,
          today,
          today,
          ReportFilters(kind = com.veterapp.domain.CatalogKind.Service),
        )
      assertEquals(1, serviceOnlyReport.rows.size)
      val cashReport =
        reports.report(
          ReportType.Sales,
          today,
          today,
          ReportFilters(methodId = cashMethod.id),
        )
      assertEquals(1, cashReport.rows.size)
      val paymentsReport =
        reports.report(
          ReportType.Payments,
          today,
          today,
          ReportFilters(methodId = cardMethod.id),
        )
      assertEquals(1, paymentsReport.rows.size)
      assertEquals(2, reports.report(ReportType.Areas, today, today).rows.size)
      assertEquals(2, reports.report(ReportType.Costs, today, today).rows.size)
      assertEquals(2, reports.report(ReportType.TopItems, today, today).rows.size)

      commerce.cancelSale(sale.sale.id, "Error de cobro")
      val afterCancel = commerce.observeSaleDetail(sale.sale.id).first()!!
      assertEquals(SaleStatus.Cancelled, afterCancel.sale.status)
      assertTrue(afterCancel.cancellationReason!!.isNotBlank())
      assertEquals(10, catalog.items.first().first { it.id == product.id }.stockCached)
      val expectedAfterCancel = commerce.observeCashDetail(session.id).first()!!.expectedCashCents
      assertEquals(10_000L, expectedAfterCancel)

      val closed = commerce.closeCashSession(session.id, 10_100)
      assertEquals(10_000L, closed.expectedCashCents)
      assertEquals(10_100L, closed.countedCashCents)
      assertEquals(100L, closed.differenceCents)
    }
  }

  @Test
  fun blocksSaleWithoutStockAndRequiresOpenCash() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val database = Room.inMemoryDatabaseBuilder(context, VetDatabase::class.java).build()
      val settings = AppSettings(context)
      settings.clinicId = "local-clinic"
      settings.userId = null
      settings.allowNegativeStock = false
      val audit = AuditRepository(database, settings)
      val catalog = CatalogRepository(database, settings, audit)
      val commerce = CommerceRepository(database, settings, audit)
      catalog.ensureDefaults("local-clinic")
      commerce.ensureDefaults("local-clinic")
      val area = catalog.areas.first().first()
      val item =
        catalog.createItem(
          CatalogItemDraft(
            kind = CatalogKind.Product,
            internalCode = "P-${UUID.randomUUID()}",
            barcode = null,
            name = "Producto",
            description = null,
            categoryId = null,
            brand = null,
            unitCostCents = 100,
            salePriceCents = 200,
            stockMin = 0,
            areaId = area.id,
            lot = null,
            expiresOn = null,
            durationMinutes = null,
            initialStock = 1,
          ),
        )
      val method = commerce.activePaymentMethods.first().first()
      val draft =
        SaleDraft(
          null,
          listOf(SaleLineDraft(item.id, 2)),
          0,
          listOf(PaymentDraft(method.id, 400)),
          null,
        )
      val noCash = runCatching { commerce.createSale(draft) }
      assertTrue(noCash.isFailure)

      commerce.openCashSession(0, null)
      val noStock = runCatching { commerce.createSale(draft) }
      assertTrue(noStock.isFailure)
      assertTrue(catalog.items.first().first { it.id == item.id }.stockCached == 1)
    }
  }
}
