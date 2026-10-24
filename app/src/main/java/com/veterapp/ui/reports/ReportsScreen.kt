package com.veterapp.ui.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.veterapp.VeterAppApplication
import com.veterapp.data.ReportFilters
import com.veterapp.data.ReportType
import com.veterapp.domain.Appointment
import com.veterapp.domain.CashSessionDetail
import com.veterapp.domain.CashSessionStatus
import com.veterapp.domain.CatalogKind
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.SaleDetail
import com.veterapp.domain.formatMoney
import com.veterapp.domain.formatSaleDateTime
import com.veterapp.ui.common.AppointmentStatusChip
import com.veterapp.ui.common.AppBackTopBar
import com.veterapp.ui.common.AppCard
import com.veterapp.ui.common.FilterPill
import com.veterapp.ui.common.DateField
import com.veterapp.ui.common.formatDate
import com.veterapp.ui.common.formatTimeRange
import com.veterapp.ui.sales.SaleDetailDialog
import com.veterapp.ui.sales.SalesViewModel
import com.veterapp.util.userMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
  onBack: () -> Unit,
  onOpenPet: (String) -> Unit,
  onOpenInventory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val app = context.applicationContext as VeterAppApplication
  val viewModel: ReportsViewModel = viewModel { ReportsViewModel(app.container) }
  val salesViewModel: SalesViewModel = viewModel { SalesViewModel(app.container) }
  val report by viewModel.report.collectAsStateWithLifecycle()
  val loading by viewModel.loading.collectAsStateWithLifecycle()
  val error by viewModel.error.collectAsStateWithLifecycle()
  val areas by salesViewModel.areas.collectAsStateWithLifecycle()
  val paymentMethods by salesViewModel.paymentMethods.collectAsStateWithLifecycle()
  val today = ClinicTime.today()
  var type by remember { mutableStateOf(ReportType.Sales) }
  var from by remember { mutableStateOf(today.withDayOfMonth(1).toEpochDay()) }
  var to by remember { mutableStateOf(today.toEpochDay()) }
  var areaId by remember { mutableStateOf<String?>(null) }
  var methodId by remember { mutableStateOf<String?>(null) }
  var kind by remember { mutableStateOf<CatalogKind?>(null) }
  var selectedSale by remember { mutableStateOf<String?>(null) }
  var selectedAppointmentId by remember { mutableStateOf<String?>(null) }
  var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
  var selectedSessionId by remember { mutableStateOf<String?>(null) }
  var pdfDetail by remember { mutableStateOf<CashSessionDetail?>(null) }
  var message by remember { mutableStateOf<String?>(null) }

  val filters = ReportFilters(areaId = areaId, methodId = methodId, kind = kind)
  val showAreaFilter =
    type in setOf(ReportType.Sales, ReportType.Areas, ReportType.Costs, ReportType.TopItems, ReportType.Inventory, ReportType.Evolution)
  val showMethodFilter =
    type in setOf(ReportType.Sales, ReportType.Payments, ReportType.Evolution)
  val showKindFilter =
    type in setOf(ReportType.Sales, ReportType.TopItems, ReportType.Inventory)

  fun load() {
    viewModel.loadReport(type, LocalDate.ofEpochDay(from), LocalDate.ofEpochDay(to), filters)
  }

  LaunchedEffect(type, from, to, areaId, methodId, kind) { load() }

  LaunchedEffect(selectedAppointmentId) {
    selectedAppointment =
      selectedAppointmentId?.let { app.container.appointmentRepository.findById(it) }
  }

  val pdfLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
      val detail = pdfDetail
      if (uri != null && detail != null) {
        viewModel.exportCashPdf(uri, detail) { result ->
          result
            .onSuccess { message = "PDF de cierre generado" }
            .onFailure { message = it.userMessage("No se pudo generar el PDF") }
        }
      }
    }

  val csvLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      val current = report
      if (uri != null && current != null) {
        viewModel.exportCsv(uri, current) { result ->
          result
            .onSuccess { message = "CSV exportado" }
            .onFailure { message = it.userMessage("No se pudo exportar el CSV") }
        }
      }
    }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
AppBackTopBar(title = "Reportes", onBack = onBack)
    },
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ReportType.entries.forEach { option ->
          FilterPill(
            label = option.title,
            selected = type == option,
            onClick = {
              type = option
              areaId = null
              methodId = null
              kind = null
              viewModel.clearReport()
            },
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateField(
          label = "Desde",
          epochDay = from,
          onChange = { from = it },
          maxEpochDay = ClinicTime.todayEpochDay(),
          modifier = Modifier.weight(1f),
        )
        DateField(
          label = "Hasta",
          epochDay = to,
          onChange = { to = it },
          maxEpochDay = ClinicTime.todayEpochDay(),
          modifier = Modifier.weight(1f),
        )
      }
      if (showAreaFilter && areas.isNotEmpty()) {
        Text("Área", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterPill(
            label = "Todas",
            selected = areaId == null,
            onClick = { areaId = null },
          )
          areas.forEach { area ->
            FilterPill(
              label = area.name,
              selected = areaId == area.id,
              onClick = { areaId = area.id },
            )
          }
        }
      }
      if (showMethodFilter && paymentMethods.isNotEmpty()) {
        Text("Medio de pago", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterPill(
            label = "Todos",
            selected = methodId == null,
            onClick = { methodId = null },
          )
          paymentMethods.forEach { method ->
            FilterPill(
              label = method.name,
              selected = methodId == method.id,
              onClick = { methodId = method.id },
            )
          }
        }
      }
      if (showKindFilter) {
        Text("Tipo", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterPill(
            label = "Todos",
            selected = kind == null,
            onClick = { kind = null },
          )
          CatalogKind.entries.forEach { option ->
            FilterPill(
              label = option.label,
              selected = kind == option,
              onClick = { kind = option },
            )
          }
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = ::load, enabled = !loading, modifier = Modifier.weight(1f)) {
          Text(if (loading) "Generando…" else "Generar reporte")
        }
        OutlinedButton(
          onClick = {
            val current = report ?: return@OutlinedButton
            val file = "reporte-${type.name.lowercase()}-" +
              "${LocalDate.ofEpochDay(from).format(DateTimeFormatter.BASIC_ISO_DATE)}.csv"
            csvLauncher.launch(file)
          },
          enabled = report != null,
          modifier = Modifier.weight(1f),
        ) {
          Text("Exportar CSV")
        }
      }
      error?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
      }
      val current = report
      if (current == null) {
        Text(
          "Elegí un reporte y el período, y tocá Generar reporte.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Text(current.type.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
          current.periodLabel,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppCard {
          Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            current.summary.forEach { (label, value) ->
              Row {
                Text("$label: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text(value, style = MaterialTheme.typography.bodySmall)
              }
            }
            if (current.type == ReportType.Costs || current.type == ReportType.Areas) {
              Text(
                "Resultado estimado: no es contabilidad formal. Se usan los costos y porcentajes congelados en cada línea.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        if (current.rows.isEmpty()) {
          Text(
            current.emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          Text(
            current.columns.joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          current.rows.forEach { row ->
            AppCard(
              onClick = {
                when {
                  row.saleId != null -> selectedSale = row.saleId
                  row.petId != null -> onOpenPet(row.petId)
                  row.itemId != null -> onOpenInventory()
                  row.appointmentId != null -> selectedAppointmentId = row.appointmentId
                  row.sessionId != null -> selectedSessionId = row.sessionId
                }
              },
            ) {
              Column(Modifier.padding(10.dp)) {
                Text(
                  row.cells.joinToString(" · "),
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
            }
          }
        }
      }
      Spacer(Modifier.height(24.dp))
    }
  }

  selectedSale?.let { saleId ->
    val detail by remember(saleId) { salesViewModel.observeSaleDetail(saleId) }
      .collectAsStateWithLifecycle(initialValue = null)
    val current: SaleDetail? = detail
    if (current != null) {
      SaleDetailDialog(
        detail = current,
        onCancel = { reason ->
          salesViewModel.cancelSale(saleId, reason) { result ->
            result.onSuccess { message = "Venta anulada" }.onFailure { message = it.message }
          }
          selectedSale = null
        },
        onDismiss = { selectedSale = null },
      )
    }
  }

  selectedAppointment?.let { appointment ->
    androidx.compose.material3.AlertDialog(
      onDismissRequest = {
        selectedAppointmentId = null
        selectedAppointment = null
      },
      title = { Text("Turno de ${appointment.petName}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          AppointmentStatusChip(appointment.status)
          Text(
            "${formatDate(appointment.startsAt)} · ${formatTimeRange(appointment.startsAt, appointment.endsAt)}",
            style = MaterialTheme.typography.titleSmall,
          )
          Text("Propietario: ${appointment.ownerName}", style = MaterialTheme.typography.bodyMedium)
          Text("Motivo: ${appointment.reason}", style = MaterialTheme.typography.bodyMedium)
          appointment.notes?.let { Text("Notas: $it", style = MaterialTheme.typography.bodySmall) }
          if (appointment.consultationId != null) {
            Text(
              "Consulta vinculada en la historia clínica.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          appointment.cancelReason?.let {
            Text(
              "Motivo de cancelación: $it",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
          Text(
            "Para gestionar el turno, abrí la agenda.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            selectedAppointmentId = null
            selectedAppointment = null
          },
        ) { Text("Cerrar") }
      },
    )
  }

  selectedSessionId?.let { sessionId ->
    CashReportDialog(
      sessionId = sessionId,
      viewModel = salesViewModel,
      onExportPdf = { detail ->
        pdfDetail = detail
        selectedSessionId = null
        pdfLauncher.launch("cierre-caja-${sessionId.take(8)}.pdf")
      },
      onDismiss = { selectedSessionId = null },
    )
  }

  message?.let { text ->
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { message = null },
      title = { Text("Reportes") },
      text = { Text(text) },
      confirmButton = { TextButton(onClick = { message = null }) { Text("Aceptar") } },
    )
  }
}

@Composable
private fun CashReportDialog(
  sessionId: String,
  viewModel: SalesViewModel,
  onExportPdf: (CashSessionDetail) -> Unit,
  onDismiss: () -> Unit,
) {
  val detail by remember(sessionId) { viewModel.observeCashDetail(sessionId) }
    .collectAsStateWithLifecycle(initialValue = null)
  val current = detail
  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Caja") },
    text = {
      if (current == null) {
        Text("Cargando caja…", style = MaterialTheme.typography.bodyMedium)
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            "Apertura: ${formatSaleDateTime(current.session.openedAt)}",
            style = MaterialTheme.typography.bodySmall,
          )
          current.session.closedAt?.let {
            Text("Cierre: ${formatSaleDateTime(it)}", style = MaterialTheme.typography.bodySmall)
          }
          Text("Inicial: ${formatMoney(current.session.openingCents)}", style = MaterialTheme.typography.bodySmall)
          Text("Esperado: ${formatMoney(current.expectedCashCents)}", style = MaterialTheme.typography.bodySmall)
          Text(
            "Contado: ${current.session.countedCashCents?.let(::formatMoney) ?: "—"}",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            "Diferencia: ${current.session.differenceCents?.let(::formatMoney) ?: "—"}",
            style = MaterialTheme.typography.bodySmall,
            color =
              if ((current.session.differenceCents ?: 0L) != 0L) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.onSurface,
          )
          HorizontalDivider()
          if (current.movements.isEmpty()) {
            Text("Sin movimientos.", style = MaterialTheme.typography.bodySmall)
          } else {
            current.movements.forEach { movement ->
              Text(
                "${movement.type.label} · ${formatMoney(movement.amountCents)}" +
                  (movement.reason?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
          if (current.session.status == CashSessionStatus.Closed) {
            OutlinedButton(onClick = { onExportPdf(current) }, modifier = Modifier.fillMaxWidth()) {
              Text("PDF de cierre")
            }
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
  )
}
