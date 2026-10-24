package com.veterapp.ui.inventory

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.veterapp.ui.common.AppFormSheet
import com.veterapp.domain.Area
import com.veterapp.domain.CatalogItem
import com.veterapp.domain.CatalogItemDraft
import com.veterapp.domain.CatalogKind
import com.veterapp.domain.Category
import com.veterapp.domain.CategoryScope
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.formatMoney
import com.veterapp.domain.parseMoneyToCents
import com.veterapp.ui.common.DateField
import com.veterapp.util.userMessage

@Composable
fun CatalogItemFormDialog(
  item: CatalogItem?,
  areas: List<Area>,
  categories: List<Category>,
  onSave: (CatalogItemDraft, (Result<Unit>) -> Unit) -> Unit,
  onDismiss: () -> Unit,
) {
  var kindStorage by rememberSaveable(item?.id) {
    mutableStateOf(item?.kind?.storage ?: CatalogKind.Product.storage)
  }
  val kind = CatalogKind.fromStorage(kindStorage)
  var name by rememberSaveable(item?.id) { mutableStateOf(item?.name.orEmpty()) }
  var internalCode by rememberSaveable(item?.id) { mutableStateOf(item?.internalCode.orEmpty()) }
  var barcode by rememberSaveable(item?.id) { mutableStateOf(item?.barcode.orEmpty()) }
  var description by rememberSaveable(item?.id) { mutableStateOf(item?.description.orEmpty()) }
  var brand by rememberSaveable(item?.id) { mutableStateOf(item?.brand.orEmpty()) }
  var areaId by rememberSaveable(item?.id) { mutableStateOf(item?.areaId ?: areas.firstOrNull()?.id.orEmpty()) }
  var categoryId by rememberSaveable(item?.id) { mutableStateOf(item?.categoryId) }
  var cost by rememberSaveable(item?.id) {
    mutableStateOf(item?.unitCostCents?.let { formatPlainMoney(it) }.orEmpty())
  }
  var price by rememberSaveable(item?.id) {
    mutableStateOf(item?.salePriceCents?.let { formatPlainMoney(it) }.orEmpty())
  }
  var stockMin by rememberSaveable(item?.id) { mutableStateOf(item?.stockMin?.toString() ?: "0") }
  var initialStock by rememberSaveable(item?.id) { mutableStateOf("0") }
  var lot by rememberSaveable(item?.id) { mutableStateOf(item?.lot.orEmpty()) }
  var expiresOn by rememberSaveable(item?.id) {
    mutableStateOf(item?.expiresOn?.let { runCatching { java.time.LocalDate.parse(it).toEpochDay() }.getOrNull() })
  }
  var duration by rememberSaveable(item?.id) {
    mutableStateOf(item?.durationMinutes?.toString().orEmpty())
  }
  var active by rememberSaveable(item?.id) { mutableStateOf(item?.active ?: true) }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  val kindFilteredCategories = categories.filter { it.active }
  val topCategories =
    kindFilteredCategories.filter {
      it.parentId == null && it.scope.accepts(kind)
    }
  val childCategories = kindFilteredCategories.filter { it.parentId == categoryId }
  val selectedArea = areas.firstOrNull { it.id == areaId }

  fun save() {
    val costCents = if (cost.isBlank()) null else parseMoneyToCents(cost)
    val priceCents = parseMoneyToCents(price)
    if (priceCents == null) {
      error = "Ingresá un precio válido"
      return
    }
    if (kind == CatalogKind.Product && (cost.isBlank() || costCents == null)) {
      error = "Ingresá el costo del producto"
      return
    }
    var selectedCategory = categoryId
    if (selectedCategory != null) {
      val category = categories.firstOrNull { it.id == selectedCategory }
      if (category == null || !category.scope.accepts(kind)) {
        selectedCategory = null
      }
    }
    val draft =
      CatalogItemDraft(
        kind = kind,
        internalCode = internalCode,
        barcode = barcode,
        name = name,
        description = description,
        categoryId = selectedCategory,
        brand = brand,
        unitCostCents = if (kind == CatalogKind.Product) costCents else costCents,
        salePriceCents = priceCents,
        stockMin = stockMin.toIntOrNull() ?: 0,
        areaId = areaId,
        lot = if (kind == CatalogKind.Product) lot else null,
        expiresOn = if (kind == CatalogKind.Product) expiresOn?.let { java.time.LocalDate.ofEpochDay(it).toString() } else null,
        durationMinutes = if (kind == CatalogKind.Service) duration.toIntOrNull() else null,
        active = active,
        initialStock = if (item == null) (initialStock.toIntOrNull() ?: 0) else 0,
      )
    draft.validate()?.let {
      error = it
      return
    }
    busy = true
    error = null
    onSave(draft) { result ->
      busy = false
      result
        .onSuccess { onDismiss() }
        .onFailure { error = it.userMessage("No se pudo guardar el ítem") }
    }
  }

  AppFormSheet(
    title = if (item == null) "Nuevo ítem" else "Editar ítem",
    subtitle = "Productos y servicios comparten el mismo catálogo.",
    confirmLabel = if (item == null) "Guardar ítem" else "Guardar cambios",
    onConfirm = ::save,
    onDismiss = onDismiss,
    busy = busy,
    dirty = false,
  ) {        if (item == null) {
          Text("Tipo", style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogKind.entries.forEach { option ->
              FilterChip(
                selected = kindStorage == option.storage,
                onClick = {
                  kindStorage = option.storage
                  categoryId = null
                },
                label = { Text(option.label) },
              )
            }
          }
        }
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nombre *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = internalCode,
          onValueChange = { internalCode = it.uppercase() },
          label = { Text("Código interno (se genera si queda vacío)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = barcode,
          onValueChange = { barcode = it },
          label = { Text("Código de barras") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Descripción") },
          minLines = 2,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = brand,
          onValueChange = { brand = it },
          label = { Text("Marca o proveedor") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Área *", style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          areas.filter { it.active }.forEach { area ->
            FilterChip(
              selected = areaId == area.id,
              onClick = { areaId = area.id },
              label = { Text(area.name) },
            )
          }
        }
        if (areas.none { it.active }) {
          Text(
            "No hay áreas activas. Configuralas en Ajustes > Catálogo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        Text("Categoría", style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = categoryId == null,
            onClick = { categoryId = null },
            label = { Text("Sin categoría") },
          )
          topCategories.forEach { category ->
            FilterChip(
              selected = categoryId == category.id,
              onClick = { categoryId = category.id },
              label = { Text(category.name) },
            )
          }
        }
        if (childCategories.isNotEmpty()) {
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            childCategories.forEach { category ->
              FilterChip(
                selected = categoryId == category.id,
                onClick = { categoryId = category.id },
                label = { Text("↳ ${category.name}") },
              )
            }
          }
        }
        OutlinedTextField(
          value = cost,
          onValueChange = { cost = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
          label = { Text(if (kind == CatalogKind.Product) "Costo *" else "Costo directo (opcional)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = price,
          onValueChange = { price = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
          label = { Text("Precio de venta *") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
        )
        if (kind == CatalogKind.Product) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = stockMin,
              onValueChange = { stockMin = it.filter { c -> c.isDigit() }.take(6) },
              label = { Text("Stock mínimo") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
            )
            if (item == null) {
              OutlinedTextField(
                value = initialStock,
                onValueChange = { initialStock = it.filter { c -> c.isDigit() }.take(6) },
                label = { Text("Stock inicial") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
              )
            }
          }
          OutlinedTextField(
            value = lot,
            onValueChange = { lot = it },
            label = { Text("Lote") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          DateField(
            label = "Vencimiento",
            epochDay = expiresOn,
            onChange = { expiresOn = it },
            enabled = !busy,
          )
          if (expiresOn != null) {
            TextButton(onClick = { expiresOn = null }, enabled = !busy) { Text("Borrar vencimiento") }
          }
        }
        if (kind == CatalogKind.Service) {
          OutlinedTextField(
            value = duration,
            onValueChange = { duration = it.filter { c -> c.isDigit() }.take(4) },
            label = { Text("Duración estimada en minutos") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Activo", modifier = Modifier.weight(1f))
          Switch(checked = active, onCheckedChange = { active = it }, enabled = !busy)
        }
        selectedArea?.let {
          Text(
            "Se guarda el área ${it.name}. Cambiarla no altera las ventas anteriores.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
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

private fun formatPlainMoney(cents: Long): String =
  java.math.BigDecimal(cents).divide(java.math.BigDecimal(100)).stripTrailingZeros().toPlainString()
