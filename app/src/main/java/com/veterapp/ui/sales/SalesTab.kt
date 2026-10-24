package com.veterapp.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.GradientHero
import com.veterapp.ui.common.StatTile
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.Sale
import com.veterapp.domain.SaleDetail
import com.veterapp.domain.SaleStatus
import com.veterapp.domain.formatMoney
import com.veterapp.domain.formatSaleTime
import com.veterapp.ui.common.DateField

private enum class SalesMode { Summary, NewSale, Cash }

@Composable
fun SalesTab(
  viewModel: SalesViewModel,
  modifier: Modifier = Modifier,
) {
  var mode by remember { mutableStateOf(SalesMode.Summary) }
  var selectedSale by remember { mutableStateOf<Sale?>(null) }
  var receipt by remember { mutableStateOf<SaleDetail?>(null) }
  var message by remember { mutableStateOf<String?>(null) }
  var day by remember { mutableStateOf(ClinicTime.todayEpochDay()) }
  val openCash by viewModel.openCash.collectAsStateWithLifecycle()
  val methods by viewModel.paymentMethods.collectAsStateWithLifecycle()
  val items by viewModel.items.collectAsStateWithLifecycle()
  val clients by viewModel.clients.collectAsStateWithLifecycle()
  val selectedDate = java.time.LocalDate.ofEpochDay(day)
  val sales by remember(day) { viewModel.observeSales(selectedDate, selectedDate) }
    .collectAsStateWithLifecycle(initialValue = emptyList())

  when (mode) {
    SalesMode.NewSale ->
      SaleFormScreen(
        items = items,
        clients = clients,
        methods = methods,
        hasOpenCash = openCash != null,
        onOpenCash = { mode = SalesMode.Cash },
        onCreate = { draft, onResult ->
          viewModel.createSale(draft) { result ->
            result.onSuccess {
              receipt = it
              mode = SalesMode.Summary
            }
            onResult(result)
          }
        },
        onCancel = { mode = SalesMode.Summary },
        modifier = modifier,
      )
    SalesMode.Cash ->
      CashScreen(
        viewModel = viewModel,
        onBack = { mode = SalesMode.Summary },
        modifier = modifier,
      )
    SalesMode.Summary -> {
      val completed = sales.filter { it.status == SaleStatus.Completed }
      val net = completed.sumOf { it.totalCents }
      val ticket = if (completed.isEmpty()) null else net / completed.size
      LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          Text("Ventas y caja", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Ventas", completed.size.toString(), Modifier.weight(1f))
            StatCard("Facturación", formatMoney(net), Modifier.weight(1f))
          }
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Ticket promedio", ticket?.let(::formatMoney) ?: "sin datos", Modifier.weight(1f))
            StatCard(
              "Caja",
              if (openCash == null) "Cerrada" else "Abierta",
              Modifier.weight(1f),
            )
          }
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { mode = SalesMode.NewSale },
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                painter = painterResource(AppIcons.Add),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text("Nueva venta")
            }
            OutlinedButton(
              onClick = { mode = SalesMode.Cash },
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                painter = painterResource(AppIcons.AccountBalanceWallet),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(if (openCash == null) "Abrir caja" else "Caja")
            }
          }
        }
        item {
          DateField(
            label = "Día",
            epochDay = day,
            onChange = { day = it },
            maxEpochDay = ClinicTime.todayEpochDay(),
          )
        }
        item {
          Text(
            "Ventas del día (${sales.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }
        if (sales.isEmpty()) {
          item {
            Text(
              "No hay ventas registradas para este día.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          items(sales, key = { it.id }) { sale ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              onClick = { selectedSale = sale },
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(Modifier.weight(1f)) {
                  Text(
                    "${sale.number} · ${formatSaleTime(sale.soldAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    sale.clientName ?: "Sin cliente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text(formatMoney(sale.totalCents), style = MaterialTheme.typography.bodyMedium)
                  Text(sale.status.label, style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
        item { Spacer(Modifier.height(24.dp)) }
      }
    }
  }

  receipt?.let { detail ->
    SaleReceiptDialog(detail = detail, onDismiss = { receipt = null })
  }

  selectedSale?.let { sale ->
    val detail by remember(sale.id) { viewModel.observeSaleDetail(sale.id) }
      .collectAsStateWithLifecycle(initialValue = null)
    val current = detail
    if (current != null) {
      SaleDetailDialog(
        detail = current,
        onCancel = { reason ->
          viewModel.cancelSale(sale.id, reason) { result ->
            result
              .onSuccess { message = "Venta anulada" }
              .onFailure { message = it.message }
          }
          selectedSale = null
        },
        onDismiss = { selectedSale = null },
      )
    }
  }

  message?.let { text ->
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { message = null },
      title = { Text("Ventas") },
      text = { Text(text) },
      confirmButton = { TextButton(onClick = { message = null }) { Text("Aceptar") } },
    )
  }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(Modifier.padding(12.dp)) {
      Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(
        label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
