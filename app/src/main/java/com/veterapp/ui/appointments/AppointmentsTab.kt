package com.veterapp.ui.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.veterapp.ui.common.AppCard
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.FilterPill
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veterapp.domain.Appointment
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.Pet
import com.veterapp.domain.buildWhatsAppUrl
import com.veterapp.ui.clinical.ConsultationFormDialog
import com.veterapp.ui.clinical.ConsultationPrefill
import com.veterapp.ui.common.AppointmentStatusChip
import com.veterapp.ui.common.formatEpochDay
import com.veterapp.ui.common.formatTimeRange
import com.veterapp.util.userMessage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.launch

private enum class CalendarMode(val label: String) {
  Month("Mes"),
  Week("Semana"),
  Day("Día"),
}

@Composable
fun AppointmentsTab(
  appointmentsViewModel: AppointmentsViewModel,
  pets: List<Pet>,
  onOpenPet: (String) -> Unit,
  onNewAppointment: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val snackbar = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var modeName by rememberSaveable { mutableStateOf(CalendarMode.Month.name) }
  val mode = runCatching { CalendarMode.valueOf(modeName) }.getOrDefault(CalendarMode.Month)
  var anchorEpochDay by rememberSaveable { mutableStateOf(ClinicTime.todayEpochDay()) }
  var selectedEpochDay by rememberSaveable { mutableStateOf(ClinicTime.todayEpochDay()) }
  var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
  var editingAppointment by remember { mutableStateOf<Appointment?>(null) }
  var attendAppointment by remember { mutableStateOf<Appointment?>(null) }

  val anchor = LocalDate.ofEpochDay(anchorEpochDay)
  val fromDay =
    when (mode) {
      CalendarMode.Month -> anchor.withDayOfMonth(1)
      CalendarMode.Week -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
      CalendarMode.Day -> anchor
    }
  val toDay =
    when (mode) {
      CalendarMode.Month -> fromDay.plusMonths(1)
      CalendarMode.Week -> fromDay.plusWeeks(1)
      CalendarMode.Day -> fromDay.plusDays(1)
    }
  val from = ClinicTime.startOfDay(fromDay.toEpochDay())
  val to = ClinicTime.startOfDay(toDay.toEpochDay())
  val appointmentsFlow = remember(from, to) { appointmentsViewModel.observeBetween(from, to) }
  val appointments by appointmentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
  val byDay = appointments.groupBy { ClinicTime.dateOf(it.startsAt) }
  val selectedDay = LocalDate.ofEpochDay(selectedEpochDay)

  fun message(text: String) {
    scope.launch { snackbar.showSnackbar(text) }
  }

  fun handleResult(result: Result<Unit>, successText: String) {
    result
      .onSuccess {
        selectedAppointment = null
        message(successText)
      }
      .onFailure { message(it.userMessage("No se pudo actualizar el turno")) }
  }

  fun openWhatsApp(appointment: Appointment) {
    val url = buildWhatsAppUrl(appointment)
    if (url == null) {
      message("El propietario no tiene un teléfono válido")
      return
    }
    runCatching {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure { message("No se pudo abrir WhatsApp") }
  }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item {
        CalendarHeader(
          mode = mode,
          title = periodTitle(mode, anchor, fromDay, toDay),
          onPrevious = {
            val next =
              when (mode) {
                CalendarMode.Month -> anchor.plusMonths(-1).withDayOfMonth(1)
                CalendarMode.Week -> anchor.plusWeeks(-1)
                CalendarMode.Day -> anchor.plusDays(-1)
              }
            anchorEpochDay = next.toEpochDay()
            selectedEpochDay = next.toEpochDay()
          },
          onNext = {
            val next =
              when (mode) {
                CalendarMode.Month -> anchor.plusMonths(1).withDayOfMonth(1)
                CalendarMode.Week -> anchor.plusWeeks(1)
                CalendarMode.Day -> anchor.plusDays(1)
              }
            anchorEpochDay = next.toEpochDay()
            selectedEpochDay = next.toEpochDay()
          },
          onToday = {
            anchorEpochDay = ClinicTime.todayEpochDay()
            selectedEpochDay = ClinicTime.todayEpochDay()
          },
          onNewAppointment = onNewAppointment,
        )
      }
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CalendarMode.entries.forEach { option ->
            FilterPill(
              label = option.label,
              selected = mode == option,
              onClick = {
                modeName = option.name
                anchorEpochDay = selectedEpochDay
              },
            )
          }
        }
      }

      when (mode) {
        CalendarMode.Month -> {
          item {
            MonthGrid(
              month = anchor,
              selected = selectedDay,
              today = ClinicTime.today(),
              counts = byDay.mapValues { it.value.size },
              onSelect = { selectedEpochDay = it.toEpochDay() },
            )
          }
          item {
            DaySectionHeader(
              date = selectedDay,
              count = byDay[selectedDay].orEmpty().size,
            )
          }
          val dayAppointments = byDay[selectedDay].orEmpty()
          if (dayAppointments.isEmpty()) {
            item { EmptyDay() }
          } else {
            items(dayAppointments, key = { it.id }) { appointment ->
              AppointmentCard(appointment = appointment, onClick = { selectedAppointment = it })
            }
          }
        }
        CalendarMode.Week -> {
          (0L until 7L).forEach { offset ->
            val day = fromDay.plusDays(offset)
            val dayAppointments = byDay[day].orEmpty()
            item(key = "header-$day") {
              DaySectionHeader(date = day, count = dayAppointments.size)
            }
            if (dayAppointments.isEmpty()) {
              item(key = "empty-$day") { EmptyDay() }
            } else {
              items(dayAppointments, key = { it.id }) { appointment ->
                AppointmentCard(appointment = appointment, onClick = { selectedAppointment = it })
              }
            }
          }
        }
        CalendarMode.Day -> {
          item {
            DaySectionHeader(date = fromDay, count = appointments.size)
          }
          if (appointments.isEmpty()) {
            item { EmptyDay() }
          } else {
            items(appointments, key = { it.id }) { appointment ->
              AppointmentCard(appointment = appointment, onClick = { selectedAppointment = it })
            }
          }
        }
      }
      item { Spacer(Modifier.height(24.dp)) }
    }

    SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
  }

  selectedAppointment?.let { appointment ->
    AppointmentDetailDialog(
      appointment = appointment,
      onDismiss = { selectedAppointment = null },
      onConfirm = {
        appointmentsViewModel.confirm(appointment.id) {
          handleResult(it, "Turno confirmado")
        }
      },
      onCancel = { reason ->
        appointmentsViewModel.cancel(appointment.id, reason) {
          handleResult(it, "Turno cancelado")
        }
      },
      onNoShow = {
        appointmentsViewModel.markNoShow(appointment.id) {
          handleResult(it, "Turno marcado como ausente")
        }
      },
      onAttend = {
        selectedAppointment = null
        attendAppointment = appointment
      },
      onEdit = {
        selectedAppointment = null
        editingAppointment = appointment
      },
      onOpenPet = {
        selectedAppointment = null
        onOpenPet(appointment.petId)
      },
      onWhatsApp = { openWhatsApp(appointment) },
    )
  }

  editingAppointment?.let { appointment ->
    AppointmentFormDialog(
      appointment = appointment,
      pets = pets,
      defaultDurationMinutes = appointmentsViewModel.defaultDurationMinutes(),
      onSave = { draft, force, onResult ->
        appointmentsViewModel.update(appointment.id, draft, force) { result ->
          if (result is com.veterapp.domain.AppointmentSaveResult.Success) {
            message("Turno actualizado")
          }
          onResult(result)
        }
      },
      onDismiss = { editingAppointment = null },
    )
  }

  attendAppointment?.let { appointment ->
    ConsultationFormDialog(
      prefill =
        ConsultationPrefill(
          consultedOn = ClinicTime.dateOf(appointment.startsAt).toEpochDay(),
          reason = appointment.reason,
        ),
      onSave = { draft, onResult ->
        appointmentsViewModel.attend(appointment, draft) { result ->
          result.onSuccess {
            selectedAppointment = null
            attendAppointment = null
            message("Turno atendido y consulta registrada")
          }
          onResult(result)
        }
      },
      onDismiss = { attendAppointment = null },
    )
  }
}

@Composable
private fun CalendarHeader(
  mode: CalendarMode,
  title: String,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onToday: () -> Unit,
  onNewAppointment: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = onToday) {
        Icon(
          painter = painterResource(AppIcons.CalendarToday),
          contentDescription = null,
          modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text("Hoy")
      }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onPrevious) {
        Icon(
          painter = painterResource(AppIcons.ChevronRight),
          contentDescription = "Período anterior",
          modifier = Modifier.size(22.dp).rotate(180f),
        )
      }
      Text(
        when (mode) {
          CalendarMode.Month -> "Vista mensual"
          CalendarMode.Week -> "Vista semanal"
          CalendarMode.Day -> "Vista diaria"
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      IconButton(onClick = onNext) {
        Icon(
          painter = painterResource(AppIcons.ChevronRight),
          contentDescription = "Período siguiente",
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(Modifier.weight(1f))
      TextButton(onClick = onNewAppointment) {
        Icon(
          painter = painterResource(AppIcons.Add),
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text("Nuevo")
      }
    }
  }
}
@Composable
private fun DaySectionHeader(date: LocalDate, count: Int) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f)) {
      Text(
        formatDayTitle(date),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        if (count == 0) "Sin turnos" else "$count turno(s)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun MonthGrid(
  month: LocalDate,
  selected: LocalDate,
  today: LocalDate,
  counts: Map<LocalDate, Int>,
  onSelect: (LocalDate) -> Unit,
) {
  val first = month.withDayOfMonth(1)
  val leading = first.dayOfWeek.value - 1
  val totalCells = ((leading + first.lengthOfMonth() + 6) / 7) * 7
  val days =
    (0 until totalCells).map { index ->
      val day = index - leading + 1
      if (day in 1..first.lengthOfMonth()) first.plusDays((day - 1).toLong()) else null
    }

  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(Modifier.fillMaxWidth()) {
      listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom").forEach { label ->
        Text(
          label,
          modifier = Modifier.weight(1f),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
      }
    }
    days.chunked(7).forEach { week ->
      Row(Modifier.fillMaxWidth()) {
        week.forEach { day ->
          Box(modifier = Modifier.weight(1f).padding(2.dp)) {
            if (day != null) {
              val count = counts[day] ?: 0
              Surface(
                onClick = { onSelect(day) },
                shape = RoundedCornerShape(14.dp),
                color =
                  when {
                    day == selected -> MaterialTheme.colorScheme.primaryContainer
                    day == today -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                  },
                border =
                  if (day == today && day != selected) {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                  } else {
                    null
                  },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                    color =
                      if (day == selected) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurface,
                  )
                  if (count > 0) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                      Modifier
                        .size(5.dp)
                        .background(
                          if (day == selected) MaterialTheme.colorScheme.onPrimaryContainer
                          else MaterialTheme.colorScheme.primary,
                          androidx.compose.foundation.shape.CircleShape,
                        ),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
@Composable
private fun EmptyDay() {
  Text(
    "No hay turnos para este día.",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(vertical = 8.dp),
  )
}

@Composable
private fun AppointmentCard(appointment: Appointment, onClick: (Appointment) -> Unit) {
  AppCard(onClick = { onClick(appointment) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1f)) {
        Text(
          formatTimeRange(appointment.startsAt, appointment.endsAt),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          "${appointment.petName} · ${appointment.ownerName}",
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          appointment.reason,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      AppointmentStatusChip(appointment.status)
    }
  }
}

private fun periodTitle(
  mode: CalendarMode,
  anchor: LocalDate,
  fromDay: LocalDate,
  toDay: LocalDate,
): String {
  val locale = Locale.forLanguageTag("es-AR")
  return when (mode) {
    CalendarMode.Month ->
      DateTimeFormatter.ofPattern("MMMM yyyy", locale)
        .format(anchor)
        .replaceFirstChar { it.titlecase(locale) }
    CalendarMode.Week ->
      "${formatEpochDay(fromDay.toEpochDay())} – ${formatEpochDay(toDay.minusDays(1).toEpochDay())}"
    CalendarMode.Day ->
      DateTimeFormatter.ofPattern("EEEE dd/MM", locale)
        .format(anchor)
        .replaceFirstChar { it.titlecase(locale) }
  }
}

private fun formatDayTitle(date: LocalDate): String {
  val locale = Locale.forLanguageTag("es-AR")
  return DateTimeFormatter.ofPattern("EEEE dd/MM", locale)
    .format(date)
    .replaceFirstChar { it.titlecase(locale) }
}
