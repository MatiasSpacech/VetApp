package com.veterapp.ui.sales

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.res.painterResource
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.StatusPill
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veterapp.domain.SaleDetail
import com.veterapp.domain.SaleStatus
import com.veterapp.domain.formatMoney
import com.veterapp.domain.formatSaleDateTime

@Composable
fun SaleReceiptDialog(
  detail: SaleDetail,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val receiptText = buildReceiptText(detail)
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          painter = painterResource(AppIcons.Receipt),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text("Comprobante ${detail.sale.number}")
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            painter = painterResource(AppIcons.Info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            "Comprobante interno no fiscal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(formatSaleDateTime(detail.sale.soldAt), style = MaterialTheme.typography.bodySmall)
        detail.sale.clientName?.let { Text("Cliente: $it", style = MaterialTheme.typography.bodySmall) }
        HorizontalDivider()
        detail.lines.forEach { line ->
          Text(line.itemName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Text(
            "${line.quantity} x ${formatMoney(line.unitPriceCents)} · ${line.areaName} · ${formatMoney(line.netCents)}",
            style = MaterialTheme.typography.bodySmall,
          )
        }
        HorizontalDivider()
        Text("Subtotal: ${formatMoney(detail.sale.subtotalCents)}", style = MaterialTheme.typography.bodySmall)
        Text("Descuentos: ${formatMoney(detail.sale.discountCents)}", style = MaterialTheme.typography.bodySmall)
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              "Total",
              style = MaterialTheme.typography.titleSmall,
              modifier = Modifier.weight(1f),
            )
            Text(
              formatMoney(detail.sale.totalCents),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
        detail.payments.forEach { payment ->
          Text(
            "${payment.methodName}: ${formatMoney(payment.amountCents)}" +
              (payment.changeCents?.takeIf { it > 0 }?.let { " · Vuelto ${formatMoney(it)}" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val intent =
            Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, receiptText)
            }
          runCatching { context.startActivity(Intent.createChooser(intent, "Compartir comprobante")) }
        },
      ) {
        Icon(
          painter = painterResource(AppIcons.Share),
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Compartir")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
  )
}

@Composable
fun SaleDetailDialog(
  detail: SaleDetail,
  onCancel: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var showCancel by remember { mutableStateOf(false) }
  var reason by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          painter = painterResource(AppIcons.Receipt),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text("Venta ${detail.sale.number}")
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(formatSaleDateTime(detail.sale.soldAt), style = MaterialTheme.typography.bodySmall)
        StatusPill(
          label = detail.sale.status.label,
          icon = if (detail.sale.status == SaleStatus.Cancelled) AppIcons.Close else AppIcons.CheckCircle,
          container =
            if (detail.sale.status == SaleStatus.Cancelled) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.secondaryContainer,
          contentColor =
            if (detail.sale.status == SaleStatus.Cancelled) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
        )
        detail.sale.clientName?.let { Text("Cliente: $it", style = MaterialTheme.typography.bodySmall) }
        HorizontalDivider()
        detail.lines.forEach { line ->
          Text("${line.itemName} · ${line.quantity} x ${formatMoney(line.unitPriceCents)}", style = MaterialTheme.typography.bodySmall)
          Text("${line.areaName} · ${formatMoney(line.netCents)}", style = MaterialTheme.typography.labelSmall)
        }
        HorizontalDivider()
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Total", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
              formatMoney(detail.sale.totalCents),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
        detail.payments.forEach { payment ->
          Text(
            "${payment.methodName}: ${formatMoney(payment.amountCents)}" +
              (payment.changeCents?.takeIf { it > 0 }?.let { " · Vuelto ${formatMoney(it)}" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
          )
        }
        detail.cancellationReason?.let {
          Text("Motivo de anulación: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (detail.sale.status == SaleStatus.Completed) {
          OutlinedButton(onClick = { showCancel = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(
              painter = painterResource(AppIcons.Delete),
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Anular venta", color = MaterialTheme.colorScheme.error)
          }
        }
        detail.sale.notes?.let { Text("Notas: $it", style = MaterialTheme.typography.bodySmall) }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
  )

  if (showCancel) {
    AlertDialog(
      onDismissRequest = { showCancel = false },
      title = { Text("Anular venta") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Se revertirán el stock y el efectivo. La venta no se borra; queda anulada y auditada.",
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Motivo *") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (reason.isBlank()) return@TextButton
            showCancel = false
            onCancel(reason)
          },
        ) {
          Text("Anular", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = { TextButton(onClick = { showCancel = false }) { Text("Volver") } },
    )
  }
}

fun buildReceiptText(detail: SaleDetail): String {
  val builder = StringBuilder()
  builder.append("VeterApp - Comprobante interno no fiscal\n")
  builder.append("Venta ${detail.sale.number}\n")
  builder.append("${formatSaleDateTime(detail.sale.soldAt)}\n")
  detail.sale.clientName?.let { builder.append("Cliente: $it\n") }
  builder.append('\n')
  detail.lines.forEach { line ->
    builder.append("${line.quantity} x ${line.itemName} (${line.areaName}) = ${formatMoney(line.netCents)}\n")
  }
  builder.append('\n')
  builder.append("Subtotal: ${formatMoney(detail.sale.subtotalCents)}\n")
  builder.append("Descuentos: ${formatMoney(detail.sale.discountCents)}\n")
  builder.append("Total: ${formatMoney(detail.sale.totalCents)}\n")
  detail.payments.forEach { payment ->
    builder.append("${payment.methodName}: ${formatMoney(payment.amountCents)}")
    payment.changeCents?.takeIf { it > 0 }?.let { builder.append(" (vuelto ${formatMoney(it)})") }
    builder.append('\n')
  }
  return builder.toString()
}
