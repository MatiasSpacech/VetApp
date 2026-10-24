package com.veterapp.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veterapp.ui.clients.ClientFormDialog
import com.veterapp.ui.clients.ClientsTab
import com.veterapp.ui.clients.ClientsViewModel
import com.veterapp.ui.home.HomeTab
import com.veterapp.ui.pets.PetFormDialog
import com.veterapp.ui.pets.PetsTab
import com.veterapp.ui.pets.PetsViewModel

private enum class MainTab(val label: String) {
  Home("Inicio"),
  Clients("Clientes"),
  Pets("Mascotas"),
}

@Composable
fun MainScreen(
  mainViewModel: MainViewModel,
  clientsViewModel: ClientsViewModel,
  petsViewModel: PetsViewModel,
  onOpenSettings: () -> Unit,
  onOpenClient: (String) -> Unit,
  onOpenPet: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by mainViewModel.uiState.collectAsStateWithLifecycle()

  when (val auth = state.auth) {
    AuthUiState.Loading -> LoadingScreen()
    is AuthUiState.SignedOut -> LoginScreen(auth.message, mainViewModel)
    is AuthUiState.Error -> ErrorScreen(auth.message)
    is AuthUiState.Local,
    is AuthUiState.SignedIn ->
      AppContent(
        state = state,
        mainViewModel = mainViewModel,
        clientsViewModel = clientsViewModel,
        petsViewModel = petsViewModel,
        onOpenSettings = onOpenSettings,
        onOpenClient = onOpenClient,
        onOpenPet = onOpenPet,
        modifier = modifier,
      )
  }
}

@Composable
private fun LoadingScreen() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}

@Composable
private fun ErrorScreen(message: String) {
  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("No se pudo iniciar", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
    Text(message, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(12.dp))
    Text(
      "Revisá la configuración de Supabase y el SQL de las etapas 0 y 2.",
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun LoginScreen(message: String?, viewModel: MainViewModel) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
  ) {
    Text("VeterApp", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text(
      "Gestión veterinaria local-first",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    Text("Iniciar sesión", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
      value = email,
      onValueChange = { email = it },
      label = { Text("Email") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
      value = password,
      onValueChange = { password = it },
      label = { Text("Contraseña") },
      singleLine = true,
      visualTransformation = PasswordVisualTransformation(),
      modifier = Modifier.fillMaxWidth(),
    )
    if (message != null) {
      Spacer(Modifier.height(12.dp))
      Text(message, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(20.dp))
    Button(onClick = { viewModel.signIn(email, password) }, modifier = Modifier.fillMaxWidth()) {
      Text("Entrar")
    }
    Spacer(Modifier.height(12.dp))
    Text(
      "El primer inicio de sesión necesita conexión. Después la app funciona offline.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContent(
  state: MainUiState,
  mainViewModel: MainViewModel,
  clientsViewModel: ClientsViewModel,
  petsViewModel: PetsViewModel,
  onOpenSettings: () -> Unit,
  onOpenClient: (String) -> Unit,
  onOpenPet: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val clients by clientsViewModel.clients.collectAsStateWithLifecycle()
  val pets by petsViewModel.pets.collectAsStateWithLifecycle()
  var selectedTabName by rememberSaveable { mutableStateOf(MainTab.Home.name) }
  val selectedTab =
    runCatching { MainTab.valueOf(selectedTabName) }.getOrDefault(MainTab.Home)
  var showClientForm by remember { mutableStateOf(false) }
  var showPetForm by remember { mutableStateOf(false) }
  val signedIn = state.auth is AuthUiState.SignedIn

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("VeterApp") },
        actions = {
          if (state.configured) {
            TextButton(onClick = mainViewModel::syncNow, enabled = !state.syncing) {
              Text(if (state.syncing) "Sincronizando…" else "Sync")
            }
          }
          TextButton(onClick = onOpenSettings) { Text("Ajustes") }
          if (signedIn) {
            TextButton(onClick = mainViewModel::signOut) { Text("Salir") }
          }
        },
      )
    },
    floatingActionButton = {
      when (selectedTab) {
        MainTab.Clients ->
          ExtendedFloatingActionButton(
            onClick = { showClientForm = true },
            icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Nuevo cliente") },
          )
        MainTab.Pets ->
          ExtendedFloatingActionButton(
            onClick = { showPetForm = true },
            icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Nueva mascota") },
          )
        MainTab.Home -> Unit
      }
    },
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {
      PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
        MainTab.entries.forEach { tab ->
          Tab(
            selected = selectedTab == tab,
            onClick = { selectedTabName = tab.name },
            text = { Text(tab.label) },
          )
        }
      }
      when (selectedTab) {
        MainTab.Home ->
          HomeTab(
            state = state,
            clients = clients,
            pets = pets,
            modifier = Modifier.weight(1f),
          )
        MainTab.Clients ->
          ClientsTab(
            clients = clients,
            pets = pets,
            onOpenClient = onOpenClient,
            onNewClient = { showClientForm = true },
            modifier = Modifier.weight(1f),
          )
        MainTab.Pets ->
          PetsTab(
            pets = pets,
            hasClients = clients.isNotEmpty(),
            onOpenPet = onOpenPet,
            onNewPet = { showPetForm = true },
            modifier = Modifier.weight(1f),
          )
      }
    }
  }

  if (showClientForm) {
    ClientFormDialog(
      client = null,
      onSave = { draft, force, onResult ->
        clientsViewModel.save(clientId = null, draft = draft, force = force, onResult = onResult)
      },
      onDismiss = { showClientForm = false },
    )
  }

  if (showPetForm) {
    PetFormDialog(
      pet = null,
      clients = clients,
      configured = petsViewModel.configured,
      online = state.online,
      onCompressPhoto = petsViewModel::compressPhoto,
      onSave = { clientId, draft, photo, _, onResult ->
        if (clientId == null) {
          onResult(Result.failure(IllegalArgumentException("Elegí un cliente")))
        } else {
          petsViewModel.createPet(clientId, draft, photo) { result ->
            onResult(result.map { Unit })
          }
        }
      },
      onDismiss = { showPetForm = false },
      onGoToClients = {
        showPetForm = false
        selectedTabName = MainTab.Clients.name
        showClientForm = true
      },
    )
  }
}
