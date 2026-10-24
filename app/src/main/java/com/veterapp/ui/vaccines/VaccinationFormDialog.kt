package com.veterapp.ui.vaccines

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
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.VaccinationDraft
import com.veterapp.domain.VaccinationSchedule
import com.veterapp.ui.common.AppFormSheet
import com.veterapp.ui.common.DateField
import com.veterapp.util.userMessage

data class VaccinationPrefill(
  val vaccineName: String,
)

@Composable
fun VaccinationFormDialog(
  prefill: VaccinationPrefill?,
  onSave: (VaccinationDraft, (Result<Unit>) -> Unit) -> Unit,
  onDismiss: () -> Unit,
) {
  var vaccineName by rememberSaveable { mutableStateOf(prefill?.vaccineName.orEmpty()) }
  var appliedOn by rememberSaveable { mutableStateOf(ClinicTime.todayEpochDay()) }
  var scheduleStorage by rememberSaveable {
    mutableStateOf(VaccinationSchedule.SixMonths.storage)
  }
  var manualNextDueOn by rememberSaveable { mutableStateOf<Long?>(null) }
  var lot by rememberSaveable { mutableStateOf("") }
  var notes by rememberSaveable { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  val schedule = VaccinationSchedule.fromStorage(scheduleStorage)

  fun save() {
    val draft =
      VaccinationDraft(
        vaccineName = vaccineName,
        appliedOn = appliedOn,
        schedule = schedule,
        manualNextDueOn = manualNextDueOn,
        lot = lot,
        notes = notes,
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
        .onFailure { error = it.userMessage("No se pudo guardar la vacunación") }
    }
  }

  AppFormSheet(
    title = if (prefill == null) "Registrar vacuna" else "Aplicar dosis",
    subtitle = "La fecha de la próxima dosis es una ayuda, no reemplaza el criterio clínico.",
    confirmLabel = "Guardar vacunación",
    onConfirm = ::save,
    onDismiss = onDismiss,
    busy = busy,
    dirty = false,
  ) {        OutlinedTextField(
          value = vaccineName,
          onValueChange = { vaccineName = it },
          label = { Text("Vacuna *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        DateField(
          label = "Fecha de aplicación",
          epochDay = appliedOn,
          onChange = { appliedOn = it },
          enabled = !busy,
          maxEpochDay = ClinicTime.todayEpochDay(),
        )
        Text("Próxima dosis", style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          VaccinationSchedule.entries.forEach { option ->
            FilterChip(
              selected = schedule == option,
              onClick = { scheduleStorage = option.storage },
              enabled = !busy,
              label = { Text(option.label) },
            )
          }
        }
        if (schedule == VaccinationSchedule.Manual) {
          DateField(
            label = "Fecha del próximo aviso",
            epochDay = manualNextDueOn,
            onChange = { manualNextDueOn = it },
            enabled = !busy,
            minEpochDay = appliedOn + 1,
          )
        }
        OutlinedTextField(
          value = lot,
          onValueChange = { lot = it },
          label = { Text("Lote") },
          singleLine = true,
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
        Text(
          "La fecha de la próxima dosis es una ayuda: el criterio clínico siempre es del veterinario.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
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
