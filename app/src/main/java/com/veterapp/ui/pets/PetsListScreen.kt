package com.veterapp.ui.pets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veterapp.domain.Pet
import com.veterapp.ui.clients.ClientsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetsListScreen(
  petsViewModel: PetsViewModel,
  clientsViewModel: ClientsViewModel,
  onBack: () -> Unit,
  onOpenPet: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val pets by petsViewModel.pets.collectAsStateWithLifecycle()
  val clients by clientsViewModel.clients.collectAsStateWithLifecycle()
  var showForm by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Mascotas") },
        navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } },
      )
    },
  ) { padding ->
    PetsTab(
      pets = pets,
      hasClients = clients.isNotEmpty(),
      onOpenPet = onOpenPet,
      onNewPet = { showForm = true },
      modifier = Modifier.fillMaxSize().padding(padding),
    )
  }

  if (showForm) {
    PetFormDialog(
      pet = null,
      clients = clients,
      configured = petsViewModel.configured,
      online = petsViewModel.online.collectAsStateWithLifecycle().value,
      onCompressPhoto = petsViewModel::compressPhoto,
      onSave = { clientId, draft, photo, _, onResult ->
        if (clientId == null) {
          onResult(Result.failure(IllegalArgumentException("Elegí un cliente")))
        } else {
          petsViewModel.createPet(clientId, draft, photo) { result -> onResult(result.map { Unit }) }
        }
      },
      onDismiss = { showForm = false },
    )
  }
}
