package com.veterapp.ui.sales

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.veterapp.VeterAppApplication
import com.veterapp.domain.CashMovement
import com.veterapp.domain.CashMovementType
import com.veterapp.domain.CashSession
import com.veterapp.domain.CashSessionDetail
import com.veterapp.domain.CashSessionStatus
import com.veterapp.domain.formatMoney
import com.veterapp.domain.formatSaleDateTime
import com.veterapp.domain.parseMoneyToCents
import com.veterapp.ui.common.AppIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.veterapp.ui.reports.ReportsViewModel
import com.veterapp.util.userMessage
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun CashScreen(
  viewModel: SalesViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val app = context.applicationContext as VeterAppApplication
  val reportsViewModel: ReportsViewModel = viewModel { ReportsViewModel(app.container) }
  val open by viewModel.openCash.collectAsStateWithLifecycle()
  val sessions by viewModel.cashSessions.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  var showOpenForm by remember { mutableStateOf(false) }
  var openingAmount by remember { mutableStateOf("") }
  var openingNotes by remember { mutableStateOf("") }
  var showMovement by remember { mutableStateOf<CashMovementType?>(null) }
  var movementAmount by remember { mutableStateOf("") }
  var movementReason by remember { mutableStateOf("") }
  var showClose by remember { mutableStateOf(false) }
  var countedAmount by remember { mutableStateOf("") }
  var selectedSession by remember { mutableStateOf<CashSession?>(null) }
  var error by remember { mutableStateOf<String?>(null) }
  var message by remember { mutableStateOf<String?>(null) }
  var pdfDetail by remember { mutableStateOf<CashSessionDetail?>(null) }

  val pdfLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
      val detail = pdfDetail
      if (uri != null && detail != null) {
        reportsViewModel.exportCashPdf(uri, detail) { result ->
          result
            .onSuccess { message = "PDF de cierre generado" }
            .onFailure { error = it.userMessage("No se pudo generar el PDF") }
        }
      }
    }

  val detail by remember(open?.id) {
    open?.let { viewModel.observeCashDetail(it.id) } ?: kotlinx.coroutines.flow.flowOf(null)
  }.collectAsStateWithLifecycle(initialValue = null)

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack) {
        Icon(
          painter = painterResource(AppIcons.ArrowBack),
          contentDescription = "Volver",
          modifier = Modifier.size(22.dp),
        )
      }
      Text("Caja", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }

    if (open == null) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("La caja está cerrada", style = MaterialTheme.typography.titleMedium)
          Text(
            "Abrí la caja con el monto inicial para empezar a vender.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedButton(onClick = { showOpenForm = true }) { Text("Abrir caja") }
        }
      }
    } else {
      val currentDetail = detail
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Caja abierta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text("Apertura: ${formatSaleDateTime(open!!.openedAt)}", style = MaterialTheme.typography.bodySmall)
          Text("Monto inicial: ${formatMoney(open!!.openingCents)}", style = MaterialTheme.typography.bodySmall)
          Text(
            "Efectivo esperado: ${formatMoney(currentDetail?.expectedCashCents ?: open!!.openingCents)}",
            style = MaterialTheme.typography.titleSmall,
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = {
            movementAmount = ""
            movementReason = ""
            showMovement = CashMovementType.Withdrawal
          },
          modifier = Modifier.weight(1f),
        ) { Text("Retiro") }
        OutlinedButton(
          onClick = {
            movementAmount = ""
            movementReason = ""
            showMovement = CashMovementType.Expense
          },
          modifier = Modifier.weight(1f),
        ) { Text("Gasto") }
        OutlinedButton(
          onClick = {
            movementAmount = ""
            movementReason = ""
            showMovement = CashMovementType.Adjustment
          },
          modifier = Modifier.weight(1f),
        ) { Text("Ajuste") }
      }
      OutlinedButton(
        onClick = {
          countedAmount = ""
          showClose = true
        },
        modifier = Modifier.fillMaxWidth(),
      ) { Text("Cerrar caja y arquear") }
      Text("Movimientos de la caja", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      val movements = currentDetail?.movements.orEmpty()
      if (movements.isEmpty()) {
        Text(
          "Todavía no hay movimientos en esta caja.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        movements.forEach { movement -> CashMovementRow(movement) }
      }
    }

    if (open == null && sessions.isNotEmpty()) {
      HorizontalDivider()
      Text("Cajas anteriores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      sessions.filter { it.status == CashSessionStatus.Closed }.take(10).forEach { session ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
          Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatSaleDateTime(session.openedAt), style = MaterialTheme.typography.bodyMedium)
            Text(
              "Esperado ${formatMoney(session.expectedCashCents ?: 0)} · Contado ${formatMoney(session.countedCashCents ?: 0)} · Diferencia ${formatMoney(session.differenceCents ?: 0)}",
              style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              TextButton(onClick = { selectedSession = session }) { Text("Ver detalle") }
            }
          }
        }
      }
    }
    Spacer(Modifier.height(24.dp))
  }

  if (showOpenForm) {
    AlertDialog(
      onDismissRequest = { showOpenForm = false },
      title = { Text("Abrir caja") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = openingAmount,
            onValueChange = { openingAmount = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
            label = { Text("Monto inicial *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = openingNotes,
            onValueChange = { openingNotes = it },
            label = { Text("Notas") },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val amount = parseMoneyToCents(openingAmount) ?: 0
            viewModel.openCash(amount, openingNotes) { result ->
              result
                .onSuccess {
                  showOpenForm = false
                  message = "Caja abierta"
                }
                .onFailure { error = it.userMessage("No se pudo abrir la caja") }
            }
          },
        ) { Text("Abrir") }
      },
      dismissButton = { TextButton(onClick = { showOpenForm = false }) { Text("Cancelar") } },
    )
  }

  showMovement?.let { type ->
    AlertDialog(
      onDismissRequest = { showMovement = null },
      title = { Text(type.label) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = movementAmount,
            onValueChange = { movementAmount = it.filter { c -> c.isDigit() || c == ',' || c == '.' || c == '-' } },
            label = { Text("Importe *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = movementReason,
            onValueChange = { movementReason = it },
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
            val amount = parseMoneyToCents(movementAmount.removePrefix("-")) ?: 0
            if (amount == 0L) return@TextButton
            val signed = if (type == CashMovementType.Adjustment && movementAmount.startsWith("-")) -amount else amount
            viewModel.addCashMovement(type, signed, movementReason) { result ->
              result
                .onSuccess {
                  showMovement = null
                  message = "Movimiento registrado"
                }
                .onFailure { error = it.userMessage("No se pudo registrar el movimiento") }
            }
          },
        ) { Text("Registrar") }
      },
      dismissButton = { TextButton(onClick = { showMovement = null }) { Text("Cancelar") } },
    )
  }

  if (showClose && open != null) {
    AlertDialog(
      onDismissRequest = { showClose = false },
      title = { Text("Cerrar caja") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Efectivo esperado: ${formatMoney(detail?.expectedCashCents ?: open!!.openingCents)}",
            style = MaterialTheme.typography.titleSmall,
          )
          OutlinedTextField(
            value = countedAmount,
            onValueChange = { countedAmount = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
            label = { Text("Efectivo contado *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val counted = parseMoneyToCents(countedAmount) ?: 0
            viewModel.closeCash(open!!.id, counted) { result ->
              result
                .onSuccess {
                  showClose = false
                  message = "Caja cerrada"
                }
                .onFailure { error = it.userMessage("No se pudo cerrar la caja") }
            }
          },
        ) { Text("Cerrar caja") }
      },
      dismissButton = { TextButton(onClick = { showClose = false }) { Text("Cancelar") } },
    )
  }

  selectedSession?.let { session ->
    val sessionDetail by viewModel.observeCashDetail(session.id)
      .collectAsStateWithLifecycle(initialValue = null)
    AlertDialog(
      onDismissRequest = { selectedSession = null },
      title = { Text("Caja del ${formatSaleDateTime(session.openedAt)}") },
      text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          val current = sessionDetail
          Text("Inicial: ${formatMoney(session.openingCents)}", style = MaterialTheme.typography.bodySmall)
          Text("Esperado: ${formatMoney(current?.expectedCashCents ?: session.expectedCashCents ?: 0)}", style = MaterialTheme.typography.bodySmall)
          Text("Contado: ${formatMoney(session.countedCashCents ?: 0)}", style = MaterialTheme.typography.bodySmall)
          Text("Diferencia: ${formatMoney(session.differenceCents ?: 0)}", style = MaterialTheme.typography.bodySmall)
          HorizontalDivider()
          current?.movements?.forEach { movement -> CashMovementRow(movement) }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val current = sessionDetail
            if (current != null) {
              pdfDetail = current
              pdfLauncher.launch("cierre-caja-${session.id.take(8)}.pdf")
            }
          },
        ) { Text("PDF de cierre") }
      },
      dismissButton = { TextButton(onClick = { selectedSession = null }) { Text("Cerrar") } },
    )
  }

  message?.let { text ->
    AlertDialog(
      onDismissRequest = { message = null },
      title = { Text("Caja") },
      text = { Text(text) },
      confirmButton = { TextButton(onClick = { message = null }) { Text("Aceptar") } },
    )
  }
  error?.let { text ->
    AlertDialog(
      onDismissRequest = { error = null },
      title = { Text("Aviso") },
      text = { Text(text) },
      confirmButton = { TextButton(onClick = { error = null }) { Text("Cerrar") } },
    )
  }
}

@Composable
private fun CashMovementRow(movement: CashMovement) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text(
        "${movement.type.label} · ${formatMoney(movement.amountCents)}",
        style = MaterialTheme.typography.bodyMedium,
        color = if (movement.amountCents < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
      )
      Text(formatSaleDateTime(movement.createdAt), style = MaterialTheme.typography.labelSmall)
      movement.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
  }
}
