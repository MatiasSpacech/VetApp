package com.veterapp.ui.clinical

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
import com.veterapp.ui.common.AppFormSheet
import com.veterapp.domain.ConsultationAddendumDraft
import com.veterapp.ui.common.formatConsultationDate
import com.veterapp.util.userMessage

@Composable
fun AddendumDialog(
  consultationDate: Long,
  onSave: (ConsultationAddendumDraft, (Result<Unit>) -> Unit) -> Unit,
  onDismiss: () -> Unit,
) {
  var reason by rememberSaveable { mutableStateOf("") }
  var text by rememberSaveable { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  fun save() {
    val draft = ConsultationAddendumDraft(reason = reason, text = text)
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
        .onFailure { error = it.userMessage("No se pudo guardar el addendum") }
    }
  }

  AppFormSheet(
    title = "Agregar addendum",
    subtitle = "La consulta original no se modifica.",
    confirmLabel = "Guardar addendum",
    onConfirm = ::save,
    onDismiss = onDismiss,
    busy = busy,
    dirty = false,
  ) {        Text(
          "Corrección de la consulta del ${formatConsultationDate(consultationDate)}. La consulta original no se modifica.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Motivo *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("Contenido *") },
          minLines = 3,
          maxLines = 6,
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
