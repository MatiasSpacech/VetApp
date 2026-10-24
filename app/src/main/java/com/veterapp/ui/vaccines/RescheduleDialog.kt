package com.veterapp.ui.vaccines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import com.veterapp.domain.Vaccination
import com.veterapp.ui.common.AppFormSheet
import com.veterapp.ui.common.DateField
import com.veterapp.ui.common.formatEpochDay
import com.veterapp.util.userMessage

@Composable
fun RescheduleDialog(
  vaccination: Vaccination,
  onSave: (newDueOn: Long, reason: String?, onResult: (Result<Unit>) -> Unit) -> Unit,
  onDismiss: () -> Unit,
) {
  var dueOn by rememberSaveable(vaccination.id) {
    mutableStateOf(vaccination.nextDueOn ?: (vaccination.appliedOn + 1))
  }
  var reason by rememberSaveable(vaccination.id) { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  fun save() {
    if (dueOn <= vaccination.appliedOn) {
      error = "El próximo aviso debe ser posterior a la aplicación"
      return
    }
    busy = true
    error = null
    onSave(dueOn, reason) { result ->
      busy = false
      result
        .onSuccess { onDismiss() }
        .onFailure { error = it.userMessage("No se pudo reprogramar") }
    }
  }

  AppFormSheet(
    title = "Reprogramar próxima dosis",
    subtitle = "El cambio queda auditado.",
    confirmLabel = "Reprogramar",
    onConfirm = ::save,
    onDismiss = onDismiss,
    busy = busy,
    dirty = false,
  ) {        Text(
          "${vaccination.vaccineName} · aplicada el ${formatEpochDay(vaccination.appliedOn)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DateField(
          label = "Nueva fecha",
          epochDay = dueOn,
          onChange = { dueOn = it },
          enabled = !busy,
          minEpochDay = vaccination.appliedOn + 1,
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Motivo (opcional)") },
          minLines = 2,
          maxLines = 3,
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

}
