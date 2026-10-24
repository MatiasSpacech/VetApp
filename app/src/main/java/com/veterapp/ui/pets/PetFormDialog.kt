package com.veterapp.ui.pets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.veterapp.domain.Client
import com.veterapp.domain.Pet
import com.veterapp.domain.PetDraft
import com.veterapp.domain.PetStatus
import com.veterapp.photo.CameraFiles
import com.veterapp.photo.CompressedPhoto
import com.veterapp.ui.clients.ClientPickerDialog
import com.veterapp.util.userMessage

@Composable
fun PetFormDialog(
  pet: Pet?,
  clients: List<Client>,
  configured: Boolean,
  online: Boolean,
  initialClientId: String? = null,
  onCompressPhoto: (Uri, (Result<CompressedPhoto>) -> Unit) -> Unit,
  onSave:
    (
      clientId: String?,
      draft: PetDraft,
      photo: CompressedPhoto?,
      removePhoto: Boolean,
      onResult: (Result<Unit>) -> Unit,
    ) -> Unit,
  onDismiss: () -> Unit,
  onGoToClients: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val lockedClientId = pet?.clientId ?: initialClientId
  val canPickClient = pet == null && initialClientId == null
  val canUploadPhoto = configured && online

  var name by rememberSaveable(pet?.id, initialClientId) { mutableStateOf(pet?.name.orEmpty()) }
  var species by rememberSaveable(pet?.id, initialClientId) {
    mutableStateOf(pet?.species ?: "Perro")
  }
  var breed by rememberSaveable(pet?.id, initialClientId) { mutableStateOf(pet?.breed.orEmpty()) }
  var notes by rememberSaveable(pet?.id, initialClientId) { mutableStateOf(pet?.notes.orEmpty()) }
  var statusStorage by rememberSaveable(pet?.id, initialClientId) {
    mutableStateOf(pet?.status?.storage ?: PetStatus.Active.storage)
  }
  var selectedClientId by rememberSaveable(pet?.id, initialClientId) {
    mutableStateOf(lockedClientId)
  }
  var selectedUri by remember(pet?.id) { mutableStateOf<Uri?>(null) }
  var compressedPhoto by remember(pet?.id) { mutableStateOf<CompressedPhoto?>(null) }
  var cameraUri by remember(pet?.id) { mutableStateOf<Uri?>(null) }
  var removePhoto by remember(pet?.id) { mutableStateOf(false) }
  var isCompressing by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  var showClientPicker by remember { mutableStateOf(false) }

  val selectedClient = clients.firstOrNull { it.id == selectedClientId }

  fun compress(uri: Uri) {
    isCompressing = true
    error = null
    onCompressPhoto(uri) { result ->
      isCompressing = false
      result
        .onSuccess {
          compressedPhoto = it
          selectedUri = uri
          removePhoto = false
        }
        .onFailure { error = it.userMessage("No se pudo procesar la foto") }
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
        error = "No se tomó la foto"
      }
    }

  fun save() {
    if (name.isBlank() || species.isBlank()) {
      error = "Completá nombre y especie"
      return
    }
    if (pet == null && selectedClientId == null) {
      error = "Elegí un cliente para la mascota"
      return
    }
    isSaving = true
    error = null
    val draft =
      PetDraft(
        name = name,
        species = species,
        breed = breed,
        notes = notes,
        status = PetStatus.fromStorage(statusStorage),
      )
    onSave(selectedClientId, draft, compressedPhoto, removePhoto) { result ->
      isSaving = false
      result
        .onSuccess { onDismiss() }
        .onFailure { error = it.userMessage("No se pudo guardar") }
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

        Text("Estado", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PetStatus.entries.forEach { status ->
            FilterChip(
              selected = statusStorage == status.storage,
              onClick = { statusStorage = status.storage },
              label = { Text(status.label) },
            )
          }
        }

        Text("Cliente", style = MaterialTheme.typography.titleSmall)
        if (canPickClient) {
          if (clients.isEmpty()) {
            Text(
              "No hay clientes. Creá uno para poder registrar la mascota.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
            if (onGoToClients != null) {
              OutlinedButton(onClick = onGoToClients, enabled = !isSaving) {
                Text("Ir a clientes")
              }
            }
          } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                selectedClient?.name ?: "Sin cliente",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
              )
              OutlinedButton(
                onClick = { showClientPicker = true },
                enabled = !isSaving,
              ) {
                Text(if (selectedClient == null) "Elegir" else "Cambiar")
              }
            }
          }
        } else {
          Text(
            selectedClient?.name ?: pet?.ownerName ?: "Sin cliente",
            style = MaterialTheme.typography.bodyMedium,
          )
          if (pet != null && pet.clientId == null) {
            Text(
              "Cliente no vinculado. Podés vincularlo desde la ficha de la mascota.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

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
        if (!configured) {
          Text(
            "Configurá Supabase para subir fotos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else if (!online) {
          Text(
            "Necesitás conexión para subir fotos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        if (isCompressing) {
          Text("Procesando foto…", style = MaterialTheme.typography.bodySmall)
        }
        error?.let {
          Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (isSaving) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Guardando…", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = ::save,
        enabled = !isSaving && !isCompressing,
      ) {
        Text(if (isSaving) "Guardando…" else if (pet == null) "Guardar" else "Guardar cambios")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
    },
  )

  if (showClientPicker) {
    ClientPickerDialog(
      clients = clients,
      selectedClientId = selectedClientId,
      title = "Elegir cliente",
      onSelect = {
        selectedClientId = it.id
        showClientPicker = false
      },
      onDismiss = { showClientPicker = false },
    )
  }
}
