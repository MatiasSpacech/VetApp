package com.veterapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veterapp.AppContainer
import com.veterapp.util.userMessage
import io.github.jan.supabase.auth.status.SessionStatus
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
      container.petRepository.pendingCount,
      container.networkMonitor.online,
      container.syncRepository.lastSyncAt,
    ) { auth, pending, online, lastSync ->
      MainUiState(
        configured = container.authRepository.isConfigured,
        auth = auth,
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
}

data class MainUiState(
  val configured: Boolean = false,
  val auth: AuthUiState = AuthUiState.Loading,
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
