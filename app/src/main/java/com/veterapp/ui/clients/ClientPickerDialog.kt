package com.veterapp.ui.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Client
import com.veterapp.ui.common.AppCard
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.SearchField

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
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          painter = painterResource(AppIcons.Clients),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(title)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SearchField(
          value = search,
          onValueChange = { search = it },
          placeholder = "Buscar cliente",
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(filtered, key = { it.id }) { client ->
              AppCard(
                onClick = { onSelect(client) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier =
                      Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      client.initials,
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                  Spacer(Modifier.width(12.dp))
                  Column(Modifier.weight(1f)) {
                    Text(client.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                      client.contactLine ?: "Sin datos de contacto",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  if (client.id == selectedClientId) {
                    Icon(
                      painter = painterResource(AppIcons.CheckCircle),
                      contentDescription = "Seleccionado",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(22.dp),
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