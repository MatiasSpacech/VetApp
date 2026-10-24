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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veterapp.domain.Client
import com.veterapp.ui.pets.PetCard
import com.veterapp.ui.pets.PetFormDialog
import com.veterapp.ui.pets.PetsViewModel
import com.veterapp.util.userMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
  clientId: String,
  clientsViewModel: ClientsViewModel,
  petsViewModel: PetsViewModel,
  onBack: () -> Unit,
  onOpenPet: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val client by clientsViewModel.observeClient(clientId).collectAsStateWithLifecycle(initialValue = null)
  val allClients by clientsViewModel.clients.collectAsStateWithLifecycle()
  val pets by petsViewModel.observePetsForClient(clientId).collectAsStateWithLifecycle(initialValue = emptyList())
  val online by petsViewModel.online.collectAsStateWithLifecycle()
  val snackbar = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var showEdit by remember { mutableStateOf(false) }
  var showAddPet by remember { mutableStateOf(false) }
  var showArchive by remember { mutableStateOf(false) }

  fun message(text: String) {
    scope.launch { snackbar.showSnackbar(text) }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Ficha del cliente") },
        navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } },
      )
    },
    snackbarHost = { SnackbarHost(snackbar) },
  ) { padding ->
    val current = client
    if (current == null) {
      Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          "Cliente no encontrado o archivado.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      return@Scaffold
    }

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ClientHeader(current)
      ContactCard(current)
      if (!current.notes.isNullOrBlank()) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
          Column(Modifier.padding(16.dp)) {
            Text("Notas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(current.notes, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { showEdit = true }, modifier = Modifier.weight(1f)) {
          Text("Editar")
        }
        OutlinedButton(onClick = { showAddPet = true }, modifier = Modifier.weight(1f)) {
          Text("Agregar mascota")
        }
      }
      TextButton(onClick = { showArchive = true }) {
        Text("Archivar cliente", color = MaterialTheme.colorScheme.error)
      }

      Text(
        "Mascotas (${pets.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      if (pets.isEmpty()) {
        Text(
          "Este cliente todavía no tiene mascotas.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        pets.forEach { pet ->
          PetCard(pet = pet, onClick = { onOpenPet(pet.id) })
        }
      }
      Spacer(Modifier.height(24.dp))
    }
  }

  if (showEdit && client != null) {
    val editing = client!!
    ClientFormDialog(
      client = editing,
      onSave = { draft, force, onResult ->
        clientsViewModel.save(editing.id, draft, force, onResult)
      },
      onDismiss = { showEdit = false },
    )
  }

  if (showAddPet) {
    val owner = client
    if (owner != null) {
      PetFormDialog(
        pet = null,
        clients = allClients,
        configured = petsViewModel.configured,
        online = online,
        initialClientId = owner.id,
        onCompressPhoto = petsViewModel::compressPhoto,
        onSave = { _, draft, photo, _, onResult ->
          petsViewModel.createPet(owner.id, draft, photo) { result ->
            onResult(result.map { Unit })
          }
        },
        onDismiss = { showAddPet = false },
      )
    }
  }

  if (showArchive && client != null) {
    val archiving = client!!
    AlertDialog(
      onDismissRequest = { showArchive = false },
      title = { Text("Archivar cliente") },
      text = {
        Text(
          "El cliente dejará de aparecer en la lista. Para archivarlo no debe tener mascotas registradas. Las mascotas no se borran.",
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showArchive = false
            clientsViewModel.archive(archiving.id) { result ->
              result
                .onSuccess { onBack() }
                .onFailure { message(it.userMessage("No se pudo archivar el cliente")) }
            }
          },
        ) {
          Text("Archivar", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showArchive = false }) { Text("Cancelar") }
      },
    )
  }
}

@Composable
private fun ClientHeader(client: Client) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(64.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          client.initials,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          fontWeight = FontWeight.Bold,
        )
      }
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Text(
          client.name,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          client.contactLine ?: "Sin datos de contacto",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun ContactCard(client: Client) {
  val rows =
    listOfNotNull(
      client.phone?.takeIf { it.isNotBlank() }?.let { "Teléfono" to it },
      client.document?.takeIf { it.isNotBlank() }?.let { "Documento" to it },
      client.email?.takeIf { it.isNotBlank() }?.let { "Email" to it },
      client.address?.takeIf { it.isNotBlank() }?.let { "Dirección" to it },
    )
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text("Contacto", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      if (rows.isEmpty()) {
        Text(
          "Sin datos de contacto.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        rows.forEach { (label, value) ->
          Row {
            Text(
              "$label: ",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Text(value, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}
