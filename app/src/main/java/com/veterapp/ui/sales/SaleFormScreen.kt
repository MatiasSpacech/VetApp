package com.veterapp.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.veterapp.domain.CatalogItem
import com.veterapp.domain.Client
import com.veterapp.domain.PaymentDraft
import com.veterapp.domain.PaymentMethod
import com.veterapp.domain.SaleDetail
import com.veterapp.domain.SaleDraft
import com.veterapp.domain.SaleLineDraft
import com.veterapp.domain.formatMoney
import com.veterapp.domain.parseMoneyToCents
import com.veterapp.ui.clients.ClientPickerDialog
import com.veterapp.util.userMessage

private data class CartLine(
  val item: CatalogItem,
  val quantity: Int,
  val discountCents: Long,
)

@Composable
fun SaleFormScreen(
  items: List<CatalogItem>,
  clients: List<Client>,
  methods: List<PaymentMethod>,
  hasOpenCash: Boolean,
  onOpenCash: () -> Unit,
  onCreate: (SaleDraft, (Result<SaleDetail>) -> Unit) -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var search by remember { mutableStateOf("") }
  var cart by remember { mutableStateOf<List<CartLine>>(emptyList()) }
  var client by remember { mutableStateOf<Client?>(null) }
  var showClientPicker by remember { mutableStateOf(false) }
  var globalDiscount by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var showPayment by remember { mutableStateOf(false) }
  var paymentAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
  var tendered by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  val visibleItems =
    items.filter { item ->
      item.active &&
        (search.isBlank() ||
          item.name.contains(search, ignoreCase = true) ||
          item.internalCode.contains(search, ignoreCase = true) ||
          item.barcode?.contains(search, ignoreCase = true) == true)
    }
  val gross = cart.sumOf { it.item.salePriceCents * it.quantity }
  val lineDiscounts = cart.sumOf { it.discountCents }
  val globalDiscountCents = parseMoneyToCents(globalDiscount) ?: 0L
  val total = (gross - lineDiscounts - globalDiscountCents).coerceAtLeast(0)
  val cashMethod = methods.firstOrNull { it.affectsCash }

  fun add(item: CatalogItem) {
    if (!item.active) return
    val existing = cart.firstOrNull { it.item.id == item.id }
    cart =
      if (existing == null) {
        cart + CartLine(item, 1, 0)
      } else {
        cart.map {
          if (it.item.id == item.id) it.copy(quantity = it.quantity + 1) else it
        }
      }
  }

  fun buildDraft(): SaleDraft? {
    if (cart.isEmpty()) {
      error = "Agregá al menos un ítem"
      return null
    }
    if (globalDiscountCents > gross - lineDiscounts) {
      error = "El descuento global supera el total"
      return null
    }
    val payments =
      methods.mapNotNull { method ->
        val amount = parseMoneyToCents(paymentAmounts[method.id].orEmpty()) ?: return@mapNotNull null
        if (amount <= 0) return@mapNotNull null
        val tenderedCents =
          if (method.affectsCash) parseMoneyToCents(tendered[method.id].orEmpty()) ?: amount else null
        PaymentDraft(methodId = method.id, amountCents = amount, tenderedCents = tenderedCents)
      }
    if (total > 0 && payments.sumOf { it.amountCents } != total) {
      error = "Los pagos deben sumar ${formatMoney(total)}"
      return null
    }
    return SaleDraft(
      clientId = client?.id,
      lines = cart.map { SaleLineDraft(itemId = it.item.id, quantity = it.quantity, discountCents = it.discountCents) },
      globalDiscountCents = globalDiscountCents,
      payments = payments,
      notes = notes,
    )
  }

  if (showPayment) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
      Text("Cobro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      Spacer(Modifier.height(8.dp))
      Text("Total a cobrar: ${formatMoney(total)}", style = MaterialTheme.typography.titleMedium)
      if (methods.isEmpty()) {
        Text(
          "No hay medios de pago activos. Configuralos en Ajustes > Economía.",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }
      Spacer(Modifier.height(12.dp))
      methods.forEach { method ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
          Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(method.name, style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
              value = paymentAmounts[method.id].orEmpty(),
              onValueChange = { value -> paymentAmounts = paymentAmounts + (method.id to value) },
              label = { Text("Importe aplicado") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.fillMaxWidth(),
            )
            if (method.affectsCash) {
              OutlinedTextField(
                value = tendered[method.id].orEmpty(),
                onValueChange = { value -> tendered = tendered + (method.id to value) },
                label = { Text("Efectivo recibido (opcional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
              )
              val applied = parseMoneyToCents(paymentAmounts[method.id].orEmpty()) ?: 0
              val received = parseMoneyToCents(tendered[method.id].orEmpty()) ?: applied
              if (received > applied) {
                Text("Vuelto: ${formatMoney(received - applied)}", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
        Spacer(Modifier.height(8.dp))
      }
      if (!hasOpenCash) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
          Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("La caja está cerrada", style = MaterialTheme.typography.titleSmall)
            Text(
              "Abrí la caja antes de confirmar la venta.",
              style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = onOpenCash) { Text("Abrir caja") }
          }
        }
      }
      error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
      }
      Spacer(Modifier.height(12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { showPayment = false }, enabled = !busy, modifier = Modifier.weight(1f)) {
          Text("Volver")
        }
        OutlinedButton(
          onClick = {
            val draft = buildDraft() ?: return@OutlinedButton
            busy = true
            error = null
            onCreate(draft) { result ->
              busy = false
              result
                .onFailure { error = it.userMessage("No se pudo confirmar la venta") }
            }
          },
          enabled = !busy && hasOpenCash,
          modifier = Modifier.weight(1f),
        ) {
          Text(if (busy) "Guardando…" else "Confirmar venta")
        }
      }
      if (cashMethod == null && methods.isNotEmpty()) {
        Text(
          "No hay un medio en efectivo activo; la caja no se moverá por esta venta.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(Modifier.height(24.dp))
    }
    return
  }

  Column(Modifier.fillMaxSize().padding(16.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("Nueva venta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
      TextButton(onClick = onCancel) { Text("Volver") }
    }
    OutlinedButton(onClick = { showClientPicker = true }) {
      Text("Cliente: ${client?.name ?: "Sin cliente (opcional)"}")
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
      value = search,
      onValueChange = { search = it },
      label = { Text("Buscar ítem") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      LazyColumn(modifier = Modifier.weight(1f)) {
        items(visibleItems, key = { it.id }) { item ->
          Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            onClick = { add(item) },
          ) {
            Column(Modifier.padding(10.dp)) {
              Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              Text(
                "${item.areaName} · ${formatMoney(item.salePriceCents)}" +
                  if (item.trackStock) " · Stock ${item.stockCached}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
      Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Text("Carrito (${cart.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (cart.isEmpty()) {
          Text(
            "Tocá un ítem para agregarlo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        cart.forEach { line ->
          Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(line.item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              Text(
                "${formatMoney(line.item.salePriceCents)} · ${line.item.areaName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                  onClick = {
                    cart =
                      cart.map {
                        if (it.item.id == line.item.id) it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) else it
                      }
                  },
                ) { Text("−") }
                Text(line.quantity.toString(), style = MaterialTheme.typography.bodyMedium)
                TextButton(
                  onClick = {
                    cart =
                      cart.map {
                        if (it.item.id == line.item.id) it.copy(quantity = it.quantity + 1) else it
                      }
                  },
                ) { Text("+") }
                TextButton(
                  onClick = { cart = cart.filterNot { it.item.id == line.item.id } },
                ) {
                  Text("Quitar", color = MaterialTheme.colorScheme.error)
                }
              }
              OutlinedTextField(
                value = if (line.discountCents == 0L) "" else (line.discountCents / 100.0).toString(),
                onValueChange = { value ->
                  val cents = parseMoneyToCents(value) ?: 0L
                  cart = cart.map { if (it.item.id == line.item.id) it.copy(discountCents = cents) else it }
                },
                label = { Text("Descuento de línea") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        OutlinedTextField(
          value = globalDiscount,
          onValueChange = { globalDiscount = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
          label = { Text("Descuento global") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notas") },
          minLines = 1,
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text("Bruto: ${formatMoney(gross)}", style = MaterialTheme.typography.bodySmall)
        Text("Descuentos: ${formatMoney(lineDiscounts + globalDiscountCents)}", style = MaterialTheme.typography.bodySmall)
        Text("Total: ${formatMoney(total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
          onClick = {
            error = null
            showPayment = true
          },
          enabled = cart.isNotEmpty(),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Cobrar")
        }
        error?.let {
          Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
      }
    }
  }

  if (showClientPicker) {
    ClientPickerDialog(
      clients = clients,
      selectedClientId = client?.id,
      title = "Elegir cliente",
      onSelect = {
        client = it
        showClientPicker = false
      },
      onDismiss = { showClientPicker = false },
    )
  }
}
