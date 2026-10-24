package com.veterapp.ui.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.veterapp.VeterAppApplication
import com.veterapp.domain.Pet
import com.veterapp.domain.PetDraft
import com.veterapp.photo.CameraFiles
import com.veterapp.photo.CompressedPhoto
import com.veterapp.util.userMessage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val app = context.applicationContext as VeterAppApplication
  val viewModel: MainViewModel = viewModel { MainViewModel(app.container) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  when (val auth = state.auth) {
    AuthUiState.Loading -> LoadingScreen()
    is AuthUiState.SignedOut -> LoginScreen(auth.message, viewModel)
    is AuthUiState.Error -> ErrorScreen(auth.message)
    is AuthUiState.Local,
    is AuthUiState.SignedIn -> PetsContent(state, viewModel, onOpenSettings, modifier)
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
      "Revisá la configuración de Supabase y el SQL de la etapa 0.",
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
private fun PetsContent(
  state: MainUiState,
  viewModel: MainViewModel,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showForm by remember { mutableStateOf(false) }
  var formPet by remember { mutableStateOf<Pet?>(null) }
  var search by rememberSaveable { mutableStateOf("") }
  val signedIn = state.auth is AuthUiState.SignedIn
  val visiblePets =
    if (search.isBlank()) {
      state.pets
    } else {
      state.pets.filter {
        it.name.contains(search, ignoreCase = true) ||
          it.ownerName.contains(search, ignoreCase = true)
      }
    }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("VeterApp") },
        actions = {
          if (state.configured) {
            TextButton(onClick = viewModel::syncNow, enabled = !state.syncing) {
              Text(if (state.syncing) "Sincronizando…" else "Sync")
            }
          }
          TextButton(onClick = onOpenSettings) { Text("Ajustes") }
          if (signedIn) {
            TextButton(onClick = viewModel::signOut) { Text("Salir") }
          }
        },
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = {
          formPet = null
          showForm = true
        },
        icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
        text = { Text("Agregar") },
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
    ) {
      Spacer(Modifier.height(8.dp))
      SyncSummary(state)
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = search,
        onValueChange = { search = it },
        label = { Text("Buscar por mascota o dueño") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(12.dp))

      when {
        state.pets.isEmpty() -> EmptyPets()
        visiblePets.isEmpty() -> NoResults(search)
        else -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(visiblePets, key = { it.id }) { pet ->
              PetCard(
                pet = pet,
                onClick = {
                  formPet = pet
                  showForm = true
                },
              )
            }
            item { Spacer(Modifier.height(88.dp)) }
          }
        }
      }
    }
  }

  if (showForm) {
    PetFormDialog(
      pet = formPet,
      state = state,
      viewModel = viewModel,
      onDismiss = { showForm = false },
    )
  }

}

@Composable
private fun SyncSummary(state: MainUiState) {
  val status =
    when {
      !state.configured -> "Modo local"
      !state.online -> "Sin conexión"
      state.syncing -> "Sincronizando…"
      state.pendingCount > 0 -> "${state.pendingCount} cambio(s) pendiente(s)"
      else -> "Sincronizado"
    }
  val color =
    when {
      !state.configured -> MaterialTheme.colorScheme.onSurfaceVariant
      !state.online -> MaterialTheme.colorScheme.error
      state.pendingCount > 0 -> MaterialTheme.colorScheme.tertiary
      else -> MaterialTheme.colorScheme.primary
    }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(status, color = color, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(4.dp))
      Text(
        "Último sync: ${formatTimestamp(state.lastSyncAt)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      val backupPending =
        state.lastBackupAt <= 0L ||
          System.currentTimeMillis() - state.lastBackupAt > 7L * 24L * 60L * 60L * 1000L
      if (backupPending) {
        Spacer(Modifier.height(4.dp))
        Text(
          "Backup semanal pendiente: abrí Ajustes",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
      if (state.syncError != null) {
        Spacer(Modifier.height(6.dp))
        Text(state.syncError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
      }
    }
  }
}

@Composable
private fun EmptyPets() {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Todavía no hay mascotas", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
    Text(
      "Agregá la primera con el botón de abajo.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun NoResults(search: String) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Sin resultados para \"$search\"", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
    Text(
      "Probá con otro nombre o dueño.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun PetCard(pet: Pet, onClick: () -> Unit) {
  Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PetPhoto(pet)
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Text(pet.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(pet.subtitle, style = MaterialTheme.typography.bodySmall)
        Text(
          "Dueño: ${pet.ownerName}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (pet.pendingSync) {
          Spacer(Modifier.height(4.dp))
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
private fun PetPhoto(pet: Pet) {
  val modifier =
    Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer)
  if (pet.fotoUrl != null) {
    AsyncImage(
      model = pet.fotoUrl,
      contentDescription = "Foto de ${pet.name}",
      modifier = modifier,
      contentScale = ContentScale.Crop,
    )
  } else {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
      Text(
        pet.name.take(1).uppercase(),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
    }
  }
}

@Composable
private fun PetFormDialog(
  pet: Pet?,
  state: MainUiState,
  viewModel: MainViewModel,
  onDismiss: () -> Unit,
) {
  var name by rememberSaveable(pet?.id) { mutableStateOf(pet?.name.orEmpty()) }
  var species by rememberSaveable(pet?.id) { mutableStateOf(pet?.species ?: "Perro") }
  var breed by rememberSaveable(pet?.id) { mutableStateOf(pet?.breed.orEmpty()) }
  var ownerName by rememberSaveable(pet?.id) { mutableStateOf(pet?.ownerName.orEmpty()) }
  var notes by rememberSaveable(pet?.id) { mutableStateOf(pet?.notes.orEmpty()) }
  var selectedUri by remember(pet?.id) { mutableStateOf<Uri?>(null) }
  var compressedPhoto by remember(pet?.id) { mutableStateOf<CompressedPhoto?>(null) }
  var cameraUri by remember(pet?.id) { mutableStateOf<Uri?>(null) }
  var removePhoto by remember(pet?.id) { mutableStateOf(false) }
  var confirmDelete by remember { mutableStateOf(false) }
  var isCompressing by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val context = LocalContext.current
  val canUploadPhoto = state.configured && state.online

  fun compress(uri: Uri) {
    isCompressing = true
    errorMessage = null
    viewModel.compressPhoto(uri) { result ->
      isCompressing = false
      result
        .onSuccess {
          compressedPhoto = it
          selectedUri = uri
          removePhoto = false
        }
        .onFailure { errorMessage = it.userMessage("No se pudo procesar la foto") }
    }
  }

  val galleryLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) compress(uri)
    }
  val cameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
      val uri = cameraUri
      if (success && uri != null) {
        compress(uri)
      } else if (!success) {
        errorMessage = "No se tomó la foto"
      }
    }

  AlertDialog(
    onDismissRequest = { if (!isSaving) onDismiss() },
    title = { Text(if (pet == null) "Nueva mascota" else "Editar mascota") },
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
          value = species,
          onValueChange = { species = it },
          label = { Text("Especie *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = breed,
          onValueChange = { breed = it },
          label = { Text("Raza") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = ownerName,
          onValueChange = { ownerName = it },
          label = { Text("Dueño *") },
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

        Text("Foto", style = MaterialTheme.typography.titleSmall)
        val preview: Any? =
          when {
            selectedUri != null -> selectedUri
            !removePhoto && pet?.fotoUrl != null -> pet.fotoUrl
            else -> null
          }
        if (preview != null) {
          AsyncImage(
            model = preview,
            contentDescription = "Foto seleccionada",
            modifier =
              Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
          )
          compressedPhoto?.let {
            Text(
              "${it.width}x${it.height} · ${it.bytes.size / 1024} KB · ${it.extension.uppercase()}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        if (pet?.fotoUrl != null && selectedUri == null && !removePhoto) {
          TextButton(onClick = { removePhoto = true }) { Text("Quitar foto") }
        }
        if (removePhoto) {
          Text(
            "La foto se quitará al guardar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
              )
            },
            enabled = canUploadPhoto && !isCompressing && !isSaving,
          ) {
            Text("Galería")
          }
          OutlinedButton(
            onClick = {
              val uri = CameraFiles.create(context)
              cameraUri = uri
              cameraLauncher.launch(uri)
            },
            enabled = canUploadPhoto && !isCompressing && !isSaving,
          ) {
            Text("Cámara")
          }
        }
        if (!state.configured) {
          Text(
            "Configurá Supabase para subir fotos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else if (!state.online) {
          Text(
            "Necesitás conexión para subir fotos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        if (isCompressing) {
          Text("Procesando foto…", style = MaterialTheme.typography.bodySmall)
        }
        errorMessage?.let {
          Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (pet != null) {
          TextButton(
            onClick = { confirmDelete = true },
            enabled = !isSaving,
          ) {
            Text("Eliminar mascota", color = MaterialTheme.colorScheme.error)
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          isSaving = true
          errorMessage = null
          val draft =
            PetDraft(
              name = name,
              species = species,
              breed = breed,
              ownerName = ownerName,
              notes = notes,
            )
          val onResult: (Result<Unit>) -> Unit = { result ->
            isSaving = false
            result
              .onSuccess { onDismiss() }
              .onFailure { errorMessage = it.userMessage("No se pudo guardar") }
          }
          if (pet == null) {
            viewModel.addPet(draft = draft, photo = compressedPhoto, onResult = onResult)
          } else {
            viewModel.updatePet(
              pet = pet,
              draft = draft,
              newPhoto = compressedPhoto,
              removePhoto = removePhoto,
              onResult = onResult,
            )
          }
        },
        enabled = !isSaving && !isCompressing,
      ) {
        Text(if (isSaving) "Guardando…" else if (pet == null) "Guardar" else "Guardar cambios")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
    },
  )

  if (confirmDelete && pet != null) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text("Eliminar mascota") },
      text = {
        Text(
          "Se va a marcar a ${pet.name} como inactiva. Los registros clínicos y las fotos no se borran.",
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            confirmDelete = false
            isSaving = true
            viewModel.deletePet(pet) { result ->
              isSaving = false
              result
                .onSuccess { onDismiss() }
                .onFailure { errorMessage = it.userMessage("No se pudo eliminar") }
            }
          },
        ) {
          Text("Eliminar", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
      },
    )
  }
}

private fun formatTimestamp(timestamp: Long): String {
  if (timestamp <= 0L) return "nunca"
  return Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}

