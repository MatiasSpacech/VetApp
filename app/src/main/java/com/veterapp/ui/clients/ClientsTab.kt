package com.veterapp.ui.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Client
import com.veterapp.domain.Pet

@Composable
fun ClientsTab(
  clients: List<Client>,
  pets: List<Pet>,
  onOpenClient: (String) -> Unit,
  onNewClient: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var search by rememberSaveable { mutableStateOf("") }
  val petCounts = pets.groupingBy { it.clientId }.eachCount()
  val visible =
    if (search.isBlank()) {
      clients
    } else {
      clients.filter {
        it.name.contains(search, ignoreCase = true) ||
          it.phone.orEmpty().contains(search, ignoreCase = true) ||
          it.document.orEmpty().contains(search, ignoreCase = true) ||
          it.email.orEmpty().contains(search, ignoreCase = true)
      }
    }

  Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
      value = search,
      onValueChange = { search = it },
      label = { Text("Buscar por nombre, teléfono o documento") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))

    when {
      clients.isEmpty() -> EmptyClients(onNewClient)
      visible.isEmpty() -> NoClientResults(search)
      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(visible, key = { it.id }) { client ->
            ClientCard(
              client = client,
              petCount = petCounts[client.id] ?: 0,
              onClick = { onOpenClient(client.id) },
            )
          }
          item { Spacer(Modifier.height(88.dp)) }
        }
      }
    }
  }
}

@Composable
private fun ClientCard(client: Client, petCount: Int, onClick: () -> Unit) {
  Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(52.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          client.initials,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Text(client.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
          client.contactLine ?: "Sin datos de contacto",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          when (petCount) {
            0 -> "Sin mascotas"
            1 -> "1 mascota"
            else -> "$petCount mascotas"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
        )
        if (client.pendingSync) {
          Text(
            "Pendiente de sincronizar",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyClients(onNewClient: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Todavía no hay clientes", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
    Text(
      "Creá el primer cliente para vincular sus mascotas.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onNewClient) { Text("Crear cliente") }
  }
}

@Composable
private fun NoClientResults(search: String) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Sin resultados para \"$search\"", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
    Text(
      "Probá con otro nombre, teléfono o documento.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
