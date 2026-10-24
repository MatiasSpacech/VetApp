package com.veterapp.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veterapp.AppContainer
import com.veterapp.domain.Client
import com.veterapp.domain.ClientDraft
import com.veterapp.domain.ClientSaveResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClientsViewModel(private val container: AppContainer) : ViewModel() {
  val clients: StateFlow<List<Client>> =
    container.clientRepository.clients
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun observeClient(clientId: String): Flow<Client?> =
    container.clientRepository.observeById(clientId)

  fun save(
    clientId: String?,
    draft: ClientDraft,
    force: Boolean = false,
    onResult: (ClientSaveResult) -> Unit,
  ) {
    viewModelScope.launch {
      val result =
        if (clientId == null) {
          container.clientRepository.create(draft, force)
        } else {
          container.clientRepository.update(clientId, draft, force)
        }
      if (result is ClientSaveResult.Success) {
        container.syncScheduler.requestSync()
      }
      onResult(result)
    }
  }

  fun archive(clientId: String, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      val result = container.clientRepository.archive(clientId)
      if (result.isSuccess) container.syncScheduler.requestSync()
      onResult(result)
    }
  }
}
