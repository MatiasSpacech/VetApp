package com.veterapp.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veterapp.AppContainer
import com.veterapp.domain.Area
import com.veterapp.domain.CatalogItem
import com.veterapp.domain.CatalogItemDraft
import com.veterapp.domain.Category
import com.veterapp.domain.CategoryScope
import com.veterapp.domain.StockMovement
import com.veterapp.domain.StockMovementDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(private val container: AppContainer) : ViewModel() {
  val items: StateFlow<List<CatalogItem>> =
    container.catalogRepository.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val areas: StateFlow<List<Area>> =
    container.catalogRepository.areas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val categories: StateFlow<List<Category>> =
    container.catalogRepository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun observeMovements(itemId: String): Flow<List<StockMovement>> =
    container.catalogRepository.observeMovements(itemId)

  fun ensureDefaults() {
    val clinicId = container.settings.currentClinicId
    viewModelScope.launch { container.catalogRepository.ensureDefaults(clinicId) }
  }

  fun createItem(draft: CatalogItemDraft, onResult: (Result<CatalogItem>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.createItem(draft) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun updateItem(itemId: String, draft: CatalogItemDraft, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.updateItem(itemId, draft) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun setActive(itemId: String, active: Boolean, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.setItemActive(itemId, active) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun addMovement(itemId: String, draft: StockMovementDraft, onResult: (Result<StockMovement>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.addMovement(itemId, draft) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun createArea(name: String, onResult: (Result<Area>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.createArea(name) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun updateArea(areaId: String, name: String, active: Boolean, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.updateArea(areaId, name, active) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun createCategory(
    name: String,
    parentId: String?,
    scope: CategoryScope,
    onResult: (Result<Category>) -> Unit,
  ) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.createCategory(name, parentId, scope) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun updateCategory(categoryId: String, name: String, active: Boolean, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.updateCategory(categoryId, name, active) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }
}
