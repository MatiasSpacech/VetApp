package com.veterapp.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.veterapp.ui.common.AppFormSheet
import com.veterapp.domain.CatalogItem
import com.veterapp.domain.StockMovementDraft
import com.veterapp.domain.StockMovementType
import com.veterapp.domain.parseMoneyToCents
import com.veterapp.util.userMessage

@Composable
fun StockMovementDialog(
  item: CatalogItem,
  onSave: (StockMovementDraft, (Result<Unit>) -> Unit) -> Unit,
  onDismiss: () -> Unit,
) {
  var typeStorage by remember { mutableStateOf(StockMovementType.Entry.storage) }
  val type = StockMovementType.fromStorage(typeStorage)
  var quantity by remember { mutableStateOf("") }
  var cost by remember { mutableStateOf("") }
  var reason by remember { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  val signedQuantity =
    when (type) {
      StockMovementType.Loss -> -(quantity.toIntOrNull() ?: 0)
      else -> quantity.toIntOrNull() ?: 0
    }
  val resulting = item.stockCached + signedQuantity

  fun save() {
    val parsed = quantity.toIntOrNull()
    if (parsed == null) {
      error = "Ingresá una cantidad"
      return
    }
    val costCents = if (cost.isBlank()) null else parseMoneyToCents(cost)
    val draft =
      StockMovementDraft(
        type = type,
        quantity = if (type == StockMovementType.Loss) kotlin.math.abs(parsed) else parsed,
        unitCostCents = costCents,
        reason = reason,
      )
    draft.validate(item.trackStock)?.let {
      error = it
      return
    }
    busy = true
    error = null
    onSave(draft) { result ->
      busy = false
      result
        .onSuccess { onDismiss() }
        .onFailure { error = it.userMessage("No se pudo registrar el movimiento") }
    }
  }

  AppFormSheet(
    title = "Movimiento de stock",
    subtitle = item.name,
    confirmLabel = "Registrar movimiento",
    onConfirm = ::save,
    onDismiss = onDismiss,
    busy = busy,
    dirty = false,
  ) {        Text(item.name, style = MaterialTheme.typography.titleSmall)
        Text(
          "Stock actual: ${item.stockCached}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(StockMovementType.Entry, StockMovementType.Adjustment, StockMovementType.Loss).forEach { option ->
            FilterChip(
              selected = type == option,
              onClick = { typeStorage = option.storage },
              label = { Text(option.label) },
            )
          }
        }
        OutlinedTextField(
          value = quantity,
          onValueChange = { value ->
            quantity = value.filter { it.isDigit() || (type == StockMovementType.Adjustment && it == '-') }
          },
          label = { Text(if (type == StockMovementType.Adjustment) "Cantidad con signo *" else "Cantidad *") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        if (type == StockMovementType.Entry) {
          OutlinedTextField(
            value = cost,
            onValueChange = { cost = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
            label = { Text("Costo unitario") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
          )
        }
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text(if (type == StockMovementType.Entry) "Motivo (opcional)" else "Motivo *") },
          minLines = 2,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
        Text(
          "Stock resultante: $resulting",
          style = MaterialTheme.typography.titleSmall,
          color =
            if (resulting < 0) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
        )
        error?.let {
          Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (busy) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Guardando…", style = MaterialTheme.typography.bodySmall)
          }
        }

  }

}
