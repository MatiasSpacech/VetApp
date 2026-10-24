package com.veterapp.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veterapp.AppContainer
import com.veterapp.data.local.AreaCostSettingEntity
import com.veterapp.domain.Area
import com.veterapp.domain.CashMovement
import com.veterapp.domain.CashMovementType
import com.veterapp.domain.CashSession
import com.veterapp.domain.CashSessionDetail
import com.veterapp.domain.CatalogItem
import com.veterapp.domain.Client
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.PaymentMethod
import com.veterapp.domain.Sale
import com.veterapp.domain.SaleDetail
import com.veterapp.domain.SaleDraft
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SalesViewModel(private val container: AppContainer) : ViewModel() {
  val openCash: StateFlow<CashSession?> =
    container.commerceRepository.openCashSession
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  val cashSessions: StateFlow<List<CashSession>> =
    container.commerceRepository.cashSessions
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val paymentMethods: StateFlow<List<PaymentMethod>> =
    container.commerceRepository.activePaymentMethods
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val allPaymentMethods: StateFlow<List<PaymentMethod>> =
    container.commerceRepository.paymentMethods
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val items: StateFlow<List<CatalogItem>> =
    container.catalogRepository.items
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val areas: StateFlow<List<Area>> =
    container.catalogRepository.areas
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val clients: StateFlow<List<Client>> =
    container.clientRepository.clients
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val areaCostSettings: StateFlow<List<AreaCostSettingEntity>> =
    container.catalogRepository.areaCostSettings
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val todaySales: StateFlow<List<Sale>> =
    flow {
        val today = ClinicTime.todayEpochDay()
        emitAll(
          container.commerceRepository.observeSalesBetween(
            ClinicTime.startOfDay(today),
            ClinicTime.startOfDay(today + 1),
          ),
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun observeSales(from: LocalDate, to: LocalDate): Flow<List<Sale>> =
    container.commerceRepository.observeSalesBetween(
      ClinicTime.startOfDay(from.toEpochDay()),
      ClinicTime.startOfDay(to.toEpochDay() + 1),
    )

  fun observeSaleDetail(saleId: String): Flow<SaleDetail?> =
    container.commerceRepository.observeSaleDetail(saleId)

  fun observeCashDetail(sessionId: String): Flow<CashSessionDetail?> =
    container.commerceRepository.observeCashDetail(sessionId)

  fun ensureDefaults() {
    val clinicId = container.settings.currentClinicId
    viewModelScope.launch {
      container.catalogRepository.ensureDefaults(clinicId)
      container.commerceRepository.ensureDefaults(clinicId)
    }
  }

  fun createSale(draft: SaleDraft, onResult: (Result<SaleDetail>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.commerceRepository.createSale(draft) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun cancelSale(saleId: String, reason: String, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.commerceRepository.cancelSale(saleId, reason) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun openCash(openingCents: Long, notes: String?, onResult: (Result<CashSession>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.commerceRepository.openCashSession(openingCents, notes) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun closeCash(sessionId: String, countedCents: Long, onResult: (Result<CashSession>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.commerceRepository.closeCashSession(sessionId, countedCents) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun addCashMovement(
    type: CashMovementType,
    amountCents: Long,
    reason: String?,
    onResult: (Result<CashMovement>) -> Unit,
  ) {
    viewModelScope.launch {
      runCatching { container.commerceRepository.addCashMovement(type, amountCents, reason) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun setAreaCost(areaId: String, percentBp: Int, validFrom: LocalDate, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.setAreaCost(areaId, percentBp, validFrom) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun createPaymentMethod(name: String, affectsCash: Boolean, onResult: (Result<PaymentMethod>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.commerceRepository.createPaymentMethod(name, affectsCash) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun updatePaymentMethod(methodId: String, name: String, active: Boolean, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.commerceRepository.updatePaymentMethod(methodId, name, active) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }
}
