package com.veterapp.ui.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Client

@Composable
fun ClientPickerDialog(
  clients: List<Client>,
  selectedClientId: String?,
  title: String = "Elegir cliente",
  onSelect: (Client) -> Unit,
  onDismiss: () -> Unit,
) {
  var search by rememberSaveable { mutableStateOf("") }
  val filtered =
    if (search.isBlank()) {
      clients
    } else {
      clients.filter {
        it.name.contains(search, ignoreCase = true) ||
          it.phone.orEmpty().contains(search, ignoreCase = true) ||
          it.document.orEmpty().contains(search, ignoreCase = true)
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = search,
          onValueChange = { search = it },
          label = { Text("Buscar cliente") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (clients.isEmpty()) {
          Text(
            "Todavía no hay clientes. Creá uno desde la pestaña Clientes.",
            style = MaterialTheme.typography.bodyMedium,
          )
        } else if (filtered.isEmpty()) {
          Text(
            "Sin resultados para \"$search\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          LazyColumn(
            modifier = Modifier.heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(filtered, key = { it.id }) { client ->
              Card(
                onClick = { onSelect(client) },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  RadioButton(
                    selected = client.id == selectedClientId,
                    onClick = { onSelect(client) },
                  )
                  Column(Modifier.padding(start = 8.dp)) {
                    Text(client.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                      client.contactLine ?: "Sin datos de contacto",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar") }
    },
  )
}
