package com.veterapp.ui.clinical

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
import com.veterapp.domain.ConsultationDraft
import com.veterapp.ui.common.DateField
import com.veterapp.util.userMessage

@Composable
fun ConsultationFormDialog(
  onSave: (ConsultationDraft, (Result<Unit>) -> Unit) -> Unit,
  onDismiss: () -> Unit,
) {
  var consultedOn by rememberSaveable { mutableStateOf(ClinicTime.todayEpochDay()) }
  var reason by rememberSaveable { mutableStateOf("") }
  var exam by rememberSaveable { mutableStateOf("") }
  var diagnosis by rememberSaveable { mutableStateOf("") }
  var treatment by rememberSaveable { mutableStateOf("") }
  var medication by rememberSaveable { mutableStateOf("") }
  var instructions by rememberSaveable { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  fun save() {
    val draft =
      ConsultationDraft(
        consultedOn = consultedOn,
        reason = reason,
        exam = exam,
        diagnosis = diagnosis,
        treatment = treatment,
        medication = medication,
        instructions = instructions,
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
        .onFailure { error = it.userMessage("No se pudo guardar la consulta") }
    }
  }

  AlertDialog(
    onDismissRequest = { if (!busy) onDismiss() },
    title = { Text("Nueva consulta") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        DateField(
          label = "Fecha",
          epochDay = consultedOn,
          onChange = { consultedOn = it },
          enabled = !busy,
          maxEpochDay = ClinicTime.todayEpochDay(),
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
          value = exam,
          onValueChange = { exam = it },
          label = { Text("Examen físico") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = diagnosis,
          onValueChange = { diagnosis = it },
          label = { Text("Diagnóstico") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = treatment,
          onValueChange = { treatment = it },
          label = { Text("Tratamiento") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = medication,
          onValueChange = { medication = it },
          label = { Text("Medicación") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = instructions,
          onValueChange = { instructions = it },
          label = { Text("Indicaciones") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
        Text(
          "La consulta queda cerrada al guardar. Las correcciones se agregan como addendum.",
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
    },
    confirmButton = {
      TextButton(onClick = ::save, enabled = !busy) {
        Text(if (busy) "Guardando…" else "Guardar")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") }
    },
  )
}
