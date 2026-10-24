package com.veterapp.ui.inventory

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.width
import com.veterapp.theme.extendedColors
import com.veterapp.ui.common.AppBackTopBar
import com.veterapp.ui.common.AppCard
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.FilterPill
import com.veterapp.ui.common.IconBadge
import com.veterapp.ui.common.SearchField
import com.veterapp.ui.common.StatusPill
import com.veterapp.VeterAppApplication
import com.veterapp.domain.CatalogItem
import com.veterapp.domain.CatalogKind
import com.veterapp.domain.ExpiryStatus
import com.veterapp.domain.StockMovement
import com.veterapp.domain.formatMoney
import com.veterapp.ui.common.formatDateTime
import com.veterapp.ui.common.formatEpochDay
import com.veterapp.util.userMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
  onBack: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val app = context.applicationContext as VeterAppApplication
  val viewModel: InventoryViewModel = viewModel { InventoryViewModel(app.container) }
  val items by viewModel.items.collectAsStateWithLifecycle()
  val areas by viewModel.areas.collectAsStateWithLifecycle()
  val categories by viewModel.categories.collectAsStateWithLifecycle()
  var search by remember { mutableStateOf("") }
  var kindFilter by remember { mutableStateOf<String?>(null) }
  var lowStockOnly by remember { mutableStateOf(false) }
  var expiringOnly by remember { mutableStateOf(false) }
  var hideInactive by remember { mutableStateOf(true) }
  var showForm by remember { mutableStateOf(false) }
  var editing by remember { mutableStateOf<CatalogItem?>(null) }
  var detail by remember { mutableStateOf<CatalogItem?>(null) }
  var movementFor by remember { mutableStateOf<CatalogItem?>(null) }
  var historyFor by remember { mutableStateOf<CatalogItem?>(null) }
  var error by remember { mutableStateOf<String?>(null) }

  val filtered =
    items.filter { item ->
      (search.isBlank() ||
        item.name.contains(search, ignoreCase = true) ||
        item.internalCode.contains(search, ignoreCase = true) ||
        item.barcode?.contains(search, ignoreCase = true) == true) &&
        (kindFilter == null || item.kind.storage == kindFilter) &&
        (!lowStockOnly || item.isLowStock) &&
        (!expiringOnly ||
          item.expiryStatus() == ExpiryStatus.Expired ||
          item.expiryStatus() == ExpiryStatus.ExpiringSoon) &&
        (!hideInactive || item.active)
    }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
AppBackTopBar(title = "Inventario", onBack = onBack)
    },
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        item {
          SearchField(
            value = search,
            onValueChange = { search = it },
            placeholder = "Buscar por nombre, código o barras",
          )
        }
        item {
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterPill(
              label = "Todos",
              selected = kindFilter == null,
              onClick = { kindFilter = null },
            )
            CatalogKind.entries.forEach { kind ->
              FilterPill(
                label = kind.label,
                selected = kindFilter == kind.storage,
                onClick = { kindFilter = kind.storage },
              )
            }
            FilterPill(
              label = "Stock bajo",
              selected = lowStockOnly,
              onClick = { lowStockOnly = !lowStockOnly },
              icon = AppIcons.Warning,
            )
            FilterPill(
              label = "Vencimientos",
              selected = expiringOnly,
              onClick = { expiringOnly = !expiringOnly },
              icon = AppIcons.Schedule,
            )
            FilterPill(
              label = "Ver inactivos",
              selected = !hideInactive,
              onClick = { hideInactive = !hideInactive },
            )
          }
        }
        item {
          Text(
            "${filtered.size} ítem(s). Stock bajo: ${items.count { it.isLowStock }} · " +
              "Por vencer: ${items.count { it.expiryStatus() == ExpiryStatus.ExpiringSoon }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                editing = null
                showForm = true
              },
              modifier = Modifier.weight(1f),
              enabled = areas.isNotEmpty(),
            ) {
              Text("Nuevo ítem")
            }
            OutlinedButton(
              onClick = onOpenSettings,
              modifier = Modifier.weight(1f),
            ) {
              Text("Ajustes de catálogo")
            }
          }
        }
        if (areas.isEmpty()) {
          item {
            Text(
              "Todavía no hay áreas. Abrí Ajustes > Catálogo para crearlas.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
        if (filtered.isEmpty()) {
          item {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
              Text(
                "No hay ítems que coincidan con los filtros.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          items(filtered, key = { it.id }) { item ->
            ItemCard(item = item, onClick = { detail = item })
          }
        }
        item { Spacer(Modifier.height(80.dp)) }
      }
    }
  }

  if (showForm) {
    CatalogItemFormDialog(
      item = editing,
      areas = areas,
      categories = categories,
      onSave = { draft, onResult ->
        if (editing == null) {
          viewModel.createItem(draft) { result -> onResult(result.map { Unit }) }
        } else {
          viewModel.updateItem(editing!!.id, draft) { result -> onResult(result) }
        }
      },
      onDismiss = {
        showForm = false
        editing = null
      },
    )
  }

  detail?.let { item ->
    AlertDialog(
      onDismissRequest = { detail = null },
      title = { Text(item.name) },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text("Código: ${item.internalCode}", style = MaterialTheme.typography.bodySmall)
          item.barcode?.let { Text("Código de barras: $it", style = MaterialTheme.typography.bodySmall) }
          Text("Tipo: ${item.kind.label}", style = MaterialTheme.typography.bodySmall)
          Text("Área: ${item.areaName}", style = MaterialTheme.typography.bodySmall)
          item.categoryName?.let { Text("Categoría: $it", style = MaterialTheme.typography.bodySmall) }
          item.brand?.let { Text("Marca: $it", style = MaterialTheme.typography.bodySmall) }
          Text("Precio: ${formatMoney(item.salePriceCents)}", style = MaterialTheme.typography.bodyMedium)
          item.unitCostCents?.let {
            Text("Costo: ${formatMoney(it)}", style = MaterialTheme.typography.bodySmall)
          }
          if (item.trackStock) {
            Text(
              "Stock: ${item.stockCached} · Mínimo: ${item.stockMin}",
              style = MaterialTheme.typography.bodyMedium,
              color = if (item.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
          }
          item.lot?.let { Text("Lote: $it", style = MaterialTheme.typography.bodySmall) }
          item.expiresOn?.let {
            Text(
              "Vencimiento: $it (${item.expiryStatus()?.label ?: ""})",
              style = MaterialTheme.typography.bodySmall,
            )
          }
          item.durationMinutes?.let {
            Text("Duración: $it minutos", style = MaterialTheme.typography.bodySmall)
          }
          Text(
            if (item.active) "Activo" else "Inactivo",
            style = MaterialTheme.typography.bodySmall,
            color = if (item.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
          )
          HorizontalDivider()
          OutlinedButton(
            onClick = {
              editing = item
              detail = null
              showForm = true
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Editar")
          }
          if (item.trackStock) {
            OutlinedButton(
              onClick = {
                movementFor = item
                detail = null
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Registrar movimiento")
            }
          }
          OutlinedButton(
            onClick = {
              historyFor = item
              detail = null
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Ver historial")
          }
          TextButton(
            onClick = {
              viewModel.setActive(item.id, !item.active) { result ->
                result.onFailure { error = it.userMessage("No se pudo cambiar el estado") }
              }
              detail = null
            },
          ) {
            Text(if (item.active) "Desactivar" else "Activar")
          }
        }
      },
      confirmButton = { TextButton(onClick = { detail = null }) { Text("Cerrar") } },
    )
  }

  movementFor?.let { item ->
    StockMovementDialog(
      item = item,
      onSave = { draft, onResult ->
        viewModel.addMovement(item.id, draft) { result -> onResult(result.map { Unit }) }
      },
      onDismiss = { movementFor = null },
    )
  }

  historyFor?.let { item ->
    StockHistoryDialog(
      item = item,
      movements = viewModel.observeMovements(item.id).collectAsStateWithLifecycle(initialValue = emptyList()).value,
      onDismiss = { historyFor = null },
    )
  }

  error?.let { message ->
    AlertDialog(
      onDismissRequest = { error = null },
      title = { Text("Aviso") },
      text = { Text(message) },
      confirmButton = { TextButton(onClick = { error = null }) { Text("Cerrar") } },
    )
  }
}

@Composable
private fun ItemCard(item: CatalogItem, onClick: () -> Unit) {
  AppCard(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconBadge(
        icon = if (item.kind == CatalogKind.Product) AppIcons.ShoppingBag else AppIcons.MedicalServices,
        tint = if (item.kind == CatalogKind.Product) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
      )
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            item.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
          )
          if (!item.active) {
            StatusPill(
              label = "Inactivo",
              icon = AppIcons.Close,
              container = MaterialTheme.colorScheme.surfaceContainerHigh,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Text(
          "${item.internalCode} · ${item.areaName}" + (item.categoryName?.let { " · $it" } ?: ""),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(formatMoney(item.salePriceCents), style = MaterialTheme.typography.titleSmall)
          if (item.trackStock) {
            StatusPill(
              label = "Stock ${item.stockCached}/${item.stockMin}",
              icon = AppIcons.Inventory,
              container =
                if (item.isLowStock) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
              contentColor =
                if (item.isLowStock) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          item.expiryStatus()?.takeIf { it != ExpiryStatus.Ok }?.let {
            StatusPill(
              label = it.label,
              icon = AppIcons.Warning,
              container = MaterialTheme.extendedColors.warningContainer,
              contentColor = MaterialTheme.extendedColors.onWarningContainer,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StockHistoryDialog(
  item: CatalogItem,
  movements: List<StockMovement>,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Historial de ${item.name}") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (movements.isEmpty()) {
          Text("Sin movimientos.", style = MaterialTheme.typography.bodyMedium)
        } else {
          movements.forEach { movement ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
              Column(Modifier.padding(8.dp)) {
                Text(
                  "${movement.type.label}: ${if (movement.quantityDelta > 0) "+" else ""}${movement.quantityDelta}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(formatDateTime(movement.createdAt), style = MaterialTheme.typography.labelSmall)
                movement.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
              }
            }
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
  )
}
