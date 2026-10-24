package com.veterapp.ui.clients

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Client
import com.veterapp.domain.ClientDraft
import com.veterapp.domain.ClientSaveResult

@Composable
fun ClientFormDialog(
  client: Client?,
  onSave: (draft: ClientDraft, force: Boolean, onResult: (ClientSaveResult) -> Unit) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by rememberSaveable(client?.id) { mutableStateOf(client?.name.orEmpty()) }
  var phone by rememberSaveable(client?.id) { mutableStateOf(client?.phone.orEmpty()) }
  var document by rememberSaveable(client?.id) { mutableStateOf(client?.document.orEmpty()) }
  var email by rememberSaveable(client?.id) { mutableStateOf(client?.email.orEmpty()) }
  var address by rememberSaveable(client?.id) { mutableStateOf(client?.address.orEmpty()) }
  var notes by rememberSaveable(client?.id) { mutableStateOf(client?.notes.orEmpty()) }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  var duplicates by remember { mutableStateOf<List<Client>>(emptyList()) }

  fun draft(): ClientDraft =
    ClientDraft(
      name = name,
      phone = phone,
      document = document,
      email = email,
      address = address,
      notes = notes,
    )

  fun save(force: Boolean) {
    if (name.isBlank()) {
      error = "Completá el nombre del cliente"
      return
    }
    busy = true
    error = null
    onSave(draft(), force) { result ->
      busy = false
      when (result) {
        is ClientSaveResult.Success -> onDismiss()
        is ClientSaveResult.Duplicate -> {
          duplicates = result.matches
          error = null
        }
        is ClientSaveResult.Failure -> {
          duplicates = emptyList()
          error = result.message
        }
      }
    }
  }

  AlertDialog(
    onDismissRequest = { if (!busy) onDismiss() },
    title = { Text(if (client == null) "Nuevo cliente" else "Editar cliente") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nombre *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Teléfono") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = document,
          onValueChange = { document = it },
          label = { Text("Documento") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("Dirección") },
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
          "El teléfono y el documento se usan para detectar clientes duplicados.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (duplicates.isNotEmpty()) {
          Text(
            "Ya existe un cliente con ese teléfono o documento:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
          duplicates.forEach { match ->
            Text(
              "· ${match.name}${match.contactLine?.let { " ($it)" }.orEmpty()}",
              style = MaterialTheme.typography.bodySmall,
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
              onClick = {
                duplicates = emptyList()
                save(force = true)
              },
              enabled = !busy,
            ) {
              Text("Guardar de todos modos")
            }
            TextButton(
              onClick = { duplicates = emptyList() },
              enabled = !busy,
            ) {
              Text("Revisar")
            }
          }
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
    },
    confirmButton = {
      TextButton(onClick = { save(force = false) }, enabled = !busy) {
        Text(if (busy) "Guardando…" else "Guardar")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") }
    },
  )
}
