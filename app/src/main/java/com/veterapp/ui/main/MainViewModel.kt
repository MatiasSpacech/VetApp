package com.veterapp.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veterapp.AppContainer
import com.veterapp.domain.Pet
import com.veterapp.domain.PetDraft
import com.veterapp.photo.CompressedPhoto
import com.veterapp.util.userMessage
import io.github.jan.supabase.auth.status.SessionStatus
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val container: AppContainer) : ViewModel() {
  private val authState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
  private val syncing = MutableStateFlow(false)

  private val baseState =
    combine(
      authState,
      container.petRepository.pets,
      container.petRepository.pendingCount,
      container.networkMonitor.online,
      container.syncRepository.lastSyncAt,
    ) { auth, pets, pending, online, lastSync ->
      MainUiState(
        configured = container.authRepository.isConfigured,
        auth = auth,
        pets = pets,
        pendingCount = pending,
        online = online,
        lastSyncAt = lastSync,
        syncing = false,
        syncError = null,
      )
    }

  val uiState: StateFlow<MainUiState> =
    combine(
      baseState,
      syncing,
      container.syncRepository.lastError,
      container.settings.lastBackupAtFlow,
    ) { base, isSyncing, error, backupAt ->
        base.copy(
          syncing = isSyncing,
          syncError = error,
          lastBackupAt = backupAt,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

  init {
    if (!container.authRepository.isConfigured) {
      authState.value = AuthUiState.Local(container.settings.localClinicId)
    } else {
      observeSession()
    }
  }

  private fun observeSession() {
    viewModelScope.launch {
      container.authRepository.sessionStatus.collect { status ->
        when (status) {
          SessionStatus.Initializing -> authState.value = AuthUiState.Loading
          is SessionStatus.Authenticated -> onAuthenticated()
          is SessionStatus.NotAuthenticated ->
            authState.value =
              AuthUiState.SignedOut(
                if (status.isSignOut) "Sesión cerrada" else null,
              )
          is SessionStatus.RefreshFailure ->
            authState.value =
              AuthUiState.SignedOut("La sesión venció. Volvé a iniciar sesión.")
        }
      }
    }
  }

  private suspend fun onAuthenticated() {
    if (authState.value is AuthUiState.SignedIn) return
    try {
      val clinicId = container.authRepository.ensureClinicId()
      authState.value = AuthUiState.SignedIn(clinicId)
      syncNow()
    } catch (error: Throwable) {
      authState.value =
        AuthUiState.Error(
          error.userMessage("No se pudo preparar la clínica en Supabase"),
        )
    }
  }

  fun signIn(email: String, password: String) {
    if (email.isBlank() || password.isBlank()) {
      authState.value = AuthUiState.SignedOut("Completá email y contraseña")
      return
    }
    authState.value = AuthUiState.Loading
    viewModelScope.launch {
      try {
        container.authRepository.signIn(email, password)
      } catch (error: Throwable) {
        authState.value =
          AuthUiState.SignedOut(error.userMessage("No se pudo iniciar sesión"))
      }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      container.authRepository.signOut()
      authState.value = AuthUiState.SignedOut("Sesión cerrada")
    }
  }

  fun syncNow() {
    if (syncing.value) return
    viewModelScope.launch {
      syncing.value = true
      try {
        container.syncRepository.syncNow()
      } finally {
        syncing.value = false
      }
    }
  }

  fun addPet(
    draft: PetDraft,
    photo: CompressedPhoto?,
    onResult: (Result<Unit>) -> Unit,
  ) {
    if (draft.name.isBlank() || draft.species.isBlank() || draft.ownerName.isBlank()) {
      onResult(Result.failure(IllegalArgumentException("Completá nombre, especie y dueño")))
      return
    }

    viewModelScope.launch {
      try {
        val clinicId =
          when (val auth = authState.value) {
            is AuthUiState.SignedIn -> auth.clinicId
            is AuthUiState.Local -> auth.clinicId
            else -> error("Iniciá sesión para guardar mascotas")
          }

        val petId = UUID.randomUUID().toString()
        val fotoUrl =
          if (photo != null) {
            if (!container.networkMonitor.online.value) {
              error("Sin conexión: no se puede subir la foto")
            }
            container.photoRepository.upload(clinicId, petId, photo)
          } else {
            null
          }

        container.petRepository.createPet(clinicId, petId, draft, fotoUrl)
        container.syncScheduler.requestSync()
        if (authState.value is AuthUiState.SignedIn) syncNow()
        onResult(Result.success(Unit))
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
    if (draft.name.isBlank() || draft.species.isBlank() || draft.ownerName.isBlank()) {
      onResult(Result.failure(IllegalArgumentException("Completá nombre, especie y dueño")))
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
        if (authState.value is AuthUiState.SignedIn) syncNow()
        onResult(Result.success(Unit))
      } catch (error: Throwable) {
        onResult(Result.failure(error))
      }
    }
  }

  fun deletePet(pet: Pet, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      try {
        container.petRepository.softDelete(pet.id)
        container.syncScheduler.requestSync()
        if (authState.value is AuthUiState.SignedIn) syncNow()
        onResult(Result.success(Unit))
      } catch (error: Throwable) {
        onResult(Result.failure(error))
      }
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

data class MainUiState(
  val configured: Boolean = false,
  val auth: AuthUiState = AuthUiState.Loading,
  val pets: List<Pet> = emptyList(),
  val pendingCount: Int = 0,
  val online: Boolean = false,
  val syncing: Boolean = false,
  val lastSyncAt: Long = 0L,
  val lastBackupAt: Long = 0L,
  val syncError: String? = null,
)

sealed interface AuthUiState {
  data object Loading : AuthUiState

  data class SignedOut(val message: String? = null) : AuthUiState

  data class Local(val clinicId: String) : AuthUiState

  data class SignedIn(val clinicId: String) : AuthUiState

  data class Error(val message: String) : AuthUiState
}
