package com.veterapp.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.veterapp.data.ReportResult
import com.veterapp.domain.CashSessionDetail
import com.veterapp.domain.formatMoney
import com.veterapp.domain.formatSaleDateTime
import com.veterapp.ui.common.formatDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportExporter(private val context: Context) {
  suspend fun exportCsv(uri: Uri, report: ReportResult): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val content = report.toCsv()
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
          stream.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("No se pudo escribir el archivo")
        Unit
      }
    }

  suspend fun exportCashPdf(uri: Uri, detail: CashSessionDetail): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val titlePaint = Paint().apply {
          textSize = 18f
          isFakeBoldText = true
        }
        val textPaint = Paint().apply { textSize = 11f }
        var y = 48f
        canvas.drawText("VeterApp - Cierre de caja", 40f, y, titlePaint)
        y += 24f
        canvas.drawText("Apertura: ${formatSaleDateTime(detail.session.openedAt)}", 40f, y, textPaint)
        y += 16f
        detail.session.closedAt?.let {
          canvas.drawText("Cierre: ${formatSaleDateTime(it)}", 40f, y, textPaint)
          y += 16f
        }
        canvas.drawText("Monto inicial: ${formatMoney(detail.session.openingCents)}", 40f, y, textPaint)
        y += 16f
        canvas.drawText("Efectivo esperado: ${formatMoney(detail.expectedCashCents)}", 40f, y, textPaint)
        y += 16f
        detail.session.countedCashCents?.let {
          canvas.drawText("Efectivo contado: ${formatMoney(it)}", 40f, y, textPaint)
          y += 16f
        }
        detail.session.differenceCents?.let {
          canvas.drawText("Diferencia: ${formatMoney(it)}", 40f, y, textPaint)
          y += 16f
        }
        y += 8f
        canvas.drawText("Movimientos", 40f, y, titlePaint.apply { textSize = 14f })
        y += 20f
        detail.movements.forEach { movement ->
          if (y > 800f) return@forEach
          val label =
            "${formatDateTime(movement.createdAt)} · ${movement.type.label} · " +
              "${formatMoney(movement.amountCents)}${movement.reason?.let { " · $it" }.orEmpty()}"
          canvas.drawText(label.take(95), 40f, y, textPaint)
          y += 14f
        }
        document.finishPage(page)
        context.contentResolver.openOutputStream(uri, "wt")?.use { document.writeTo(it) }
          ?: error("No se pudo escribir el PDF")
        document.close()
        Unit
      }
    }
}
