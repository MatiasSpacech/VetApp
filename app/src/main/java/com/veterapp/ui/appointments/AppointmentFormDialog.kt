package com.veterapp.ui.appointments

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Appointment
import com.veterapp.domain.AppointmentDraft
import com.veterapp.domain.AppointmentSaveResult
import com.veterapp.domain.AppointmentWarning
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.Pet
import com.veterapp.ui.common.AppFormSheet
import com.veterapp.ui.common.DateField
import com.veterapp.ui.common.TimeField
import com.veterapp.ui.common.formatDate
import com.veterapp.ui.common.formatTime
import com.veterapp.ui.common.formatTimeRange
import java.time.LocalTime

@Composable
fun AppointmentFormDialog(
  appointment: Appointment?,
  pets: List<Pet>,
  defaultDurationMinutes: Int,
  onSave: (AppointmentDraft, Boolean, (AppointmentSaveResult) -> Unit) -> Unit,
  onDismiss: () -> Unit,
) {
  val editing = appointment != null
  val initialDate =
    appointment?.let { ClinicTime.dateOf(it.startsAt).toEpochDay() } ?: ClinicTime.todayEpochDay()
  val initialStart =
    appointment?.let { ClinicTime.minuteOfDay(it.startsAt) } ?: defaultStartMinute()
  val initialDuration =
    appointment?.durationMinutes?.toInt()?.coerceAtLeast(5) ?: defaultDurationMinutes

  var selectedPetId by rememberSaveable(appointment?.id) {
    mutableStateOf(appointment?.petId)
  }
  var dateEpochDay by rememberSaveable(appointment?.id) { mutableStateOf(initialDate) }
  var startMinute by rememberSaveable(appointment?.id) { mutableStateOf(initialStart) }
  var durationMinutes by rememberSaveable(appointment?.id) { mutableStateOf(initialDuration) }
  var reason by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.reason.orEmpty()) }
  var notes by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.notes.orEmpty()) }
  var showPetPicker by remember { mutableStateOf(false) }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  var warning by remember { mutableStateOf<AppointmentWarning?>(null) }
  var pendingDraft by remember { mutableStateOf<AppointmentDraft?>(null) }

  val activePets = pets.filter { it.status == com.veterapp.domain.PetStatus.Active || it.id == appointment?.petId }
  val selectedPet = pets.firstOrNull { it.id == selectedPetId }
  val startAt = ClinicTime.instantAt(dateEpochDay, startMinute)
  val endAt = startAt + durationMinutes * 60_000L
  val durations = remember(durationMinutes) {
    (listOf(15, 30, 45, 60, 90, 120) + durationMinutes).distinct().sorted()
  }

  fun buildDraft(): AppointmentDraft =
    AppointmentDraft(
      petId = selectedPetId.orEmpty(),
      startsAt = startAt,
      endsAt = endAt,
      reason = reason,
      notes = notes,
    )

  fun submit(force: Boolean, draft: AppointmentDraft? = null) {
    val target = draft ?: buildDraft()
    target.validate()?.let {
      error = it
      return
    }
    busy = true
    error = null
    onSave(target, force) { result ->
      busy = false
      when (result) {
        is AppointmentSaveResult.Success -> onDismiss()
        is AppointmentSaveResult.Warning -> {
          pendingDraft = target
          warning = result.warning
        }
        is AppointmentSaveResult.Failure -> error = result.message
      }
    }
  }

  AppFormSheet(
    title = if (editing) "Editar turno" else "Nuevo turno",
    subtitle = "Elegí mascota, fecha, duración y motivo.",
    confirmLabel = "Guardar turno",
    onConfirm = { submit(force = false) },
    onDismiss = onDismiss,
    busy = busy,
    dirty = false,
  ) {        if (editing) {
          Text(
            selectedPet?.let { "${it.name} · ${it.ownerName}" } ?: "Mascota del turno",
            style = MaterialTheme.typography.titleSmall,
          )
        } else {
          OutlinedButton(
            onClick = { showPetPicker = true },
            enabled = !busy && activePets.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              selectedPet?.let { "${it.name} · ${it.ownerName}" }
                ?: if (activePets.isEmpty()) "No hay mascotas activas" else "Elegir mascota *",
            )
          }
        }
        DateField(
          label = "Fecha",
          epochDay = dateEpochDay,
          onChange = { dateEpochDay = it },
          enabled = !busy,
        )
        TimeField(
          label = "Inicio",
          minuteOfDay = startMinute,
          onChange = { startMinute = it },
          enabled = !busy,
        )
        Text("Duración", style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          durations.forEach { option ->
            FilterChip(
              selected = durationMinutes == option,
              onClick = { durationMinutes = option },
              enabled = !busy,
              label = { Text("$option min") },
            )
          }
        }
        Text(
          "Termina a las ${formatTime(endAt)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Motivo *") },
          minLines = 2,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notas") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
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


  if (showPetPicker) {
    PetPickerDialog(
      pets = activePets,
      selectedPetId = selectedPetId,
      onSelect = {
        selectedPetId = it.id
        showPetPicker = false
      },
      onDismiss = { showPetPicker = false },
    )
  }

  warning?.let { current ->
    AlertDialog(
      onDismissRequest = {
        warning = null
        pendingDraft = null
      },
      title = { Text("Revisar turno") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          if (current.conflicts.isNotEmpty()) {
            Text(
              "Hay ${current.conflicts.size} turno(s) en el mismo horario:",
              style = MaterialTheme.typography.bodyMedium,
            )
            current.conflicts.forEach { conflict ->
              Text(
                "• ${conflict.petName} · ${formatDate(conflict.startsAt)} " +
                  formatTimeRange(conflict.startsAt, conflict.endsAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          if (current.outsideBusinessHours) {
            Text(
              "El turno está fuera del horario de atención configurado.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val draft = pendingDraft
            warning = null
            pendingDraft = null
            if (draft != null) submit(force = true, draft = draft)
          },
          enabled = !busy,
        ) {
          Text("Guardar igual")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            warning = null
            pendingDraft = null
          },
          enabled = !busy,
        ) {
          Text("Volver")
        }
      },
    )
  }
}

private fun defaultStartMinute(): Int {
  val now = LocalTime.now(ClinicTime.zone)
  val minutes = now.hour * 60 + now.minute
  val rounded = ((minutes + 14) / 15) * 15
  return when {
    rounded < 9 * 60 -> 9 * 60
    rounded > 18 * 60 -> 9 * 60
    else -> rounded
  }
}
