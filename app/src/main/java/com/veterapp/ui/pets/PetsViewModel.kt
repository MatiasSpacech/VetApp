package com.veterapp.ui.pets

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veterapp.AppContainer
import com.veterapp.domain.Pet
import com.veterapp.domain.PetDraft
import com.veterapp.domain.PetOwnerHistory
import com.veterapp.domain.PetStatus
import com.veterapp.photo.CompressedPhoto
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PetsViewModel(private val container: AppContainer) : ViewModel() {
  val configured: Boolean = container.authRepository.isConfigured
  val online: StateFlow<Boolean> = container.networkMonitor.online

  val pets: StateFlow<List<Pet>> =
    container.petRepository.pets
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun observePet(petId: String): Flow<Pet?> = container.petRepository.observePet(petId)

  fun observePetsForClient(clientId: String): Flow<List<Pet>> =
    container.petRepository.observePetsForClient(clientId)

  fun observeOwnerHistory(petId: String): Flow<List<PetOwnerHistory>> =
    container.petRepository.observeOwnerHistory(petId)

  fun createPet(
    clientId: String,
    draft: PetDraft,
    photo: CompressedPhoto?,
    onResult: (Result<Pet>) -> Unit,
  ) {
    if (draft.name.isBlank() || draft.species.isBlank()) {
      onResult(Result.failure(IllegalArgumentException("Completá nombre y especie")))
      return
    }
    viewModelScope.launch {
      try {
        val petId = UUID.randomUUID().toString()
        val fotoUrl =
          if (photo != null) {
            if (!container.networkMonitor.online.value) {
              error("Sin conexión: no se puede subir la foto")
            }
            container.photoRepository.upload(container.settings.currentClinicId, petId, photo)
          } else {
            null
          }
        val pet = container.petRepository.createPet(clientId, petId, draft, fotoUrl)
        container.syncScheduler.requestSync()
        onResult(Result.success(pet))
      } catch (error: Throwable) {
        onResult(Result.failure(error))
      }
    }
  }

  fun updatePet(
    pet: Pet,
    draft: PetDraft,
    newPhoto: CompressedPhoto?,
    removePhoto: Boolean,
    onResult: (Result<Unit>) -> Unit,
  ) {
    if (draft.name.isBlank() || draft.species.isBlank()) {
      onResult(Result.failure(IllegalArgumentException("Completá nombre y especie")))
      return
    }
    viewModelScope.launch {
      try {
        val fotoUrl =
          if (newPhoto != null) {
            if (!container.networkMonitor.online.value) {
              error("Sin conexión: no se puede subir la foto")
            }
            container.photoRepository.upload(pet.clinicId, pet.id, newPhoto)
          } else {
            null
          }
        container.petRepository.updatePet(
          petId = pet.id,
          draft = draft,
          fotoUrl = fotoUrl,
          removePhoto = removePhoto,
        )
        container.syncScheduler.requestSync()
        onResult(Result.success(Unit))
      } catch (error: Throwable) {
        onResult(Result.failure(error))
      }
    }
  }

  fun changeStatus(pet: Pet, status: PetStatus, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.petRepository.changeStatus(pet.id, status) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun changeOwner(
    pet: Pet,
    newClientId: String,
    reason: String?,
    onResult: (Result<Unit>) -> Unit,
  ) {
    viewModelScope.launch {
      runCatching { container.petRepository.changeOwner(pet.id, newClientId, reason) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun archivePet(pet: Pet, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.petRepository.softDelete(pet.id) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun compressPhoto(uri: Uri, onResult: (Result<CompressedPhoto>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.photoCompressor.compress(uri) }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }
}
