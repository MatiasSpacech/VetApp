package com.veterapp.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veterapp.AppContainer
import com.veterapp.domain.Area
import com.veterapp.domain.Category
import com.veterapp.domain.CategoryScope
import com.veterapp.domain.PaymentMethod
import com.veterapp.domain.WeeklyHours
import com.veterapp.domain.ClinicTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
  val securityState = container.securityRepository.state
  val lastBackupAt = container.settings.lastBackupAtFlow
  val auditEvents = container.auditRepository.recent
  val areas: Flow<List<Area>> = container.catalogRepository.areas
  val categories: Flow<List<Category>> = container.catalogRepository.categories
  val paymentMethods: Flow<List<PaymentMethod>> = container.commerceRepository.paymentMethods
  val areaCosts = container.catalogRepository.areaCostSettings

  fun businessHours(): WeeklyHours = container.settings.businessHours()

  fun defaultDurationMinutes(): Int = container.settings.defaultDurationMinutes

  fun reminderMonthDays(): Int = container.settings.reminderMonthDays

  fun reminderWeekDays(): Int = container.settings.reminderWeekDays

  fun reminderOverdue(): Boolean = container.settings.reminderOverdue

  fun saveReminders(
    monthDays: Int,
    weekDays: Int,
    overdue: Boolean,
    onResult: (Result<Unit>) -> Unit,
  ) {
    viewModelScope.launch {
      runCatching {
          require(monthDays in 1..365) { "La anticipación mensual debe estar entre 1 y 365 días" }
          require(weekDays in 0..monthDays) {
            "La anticipación semanal debe estar entre 0 y la mensual"
          }
          container.settings.reminderMonthDays = monthDays
          container.settings.reminderWeekDays = weekDays
          container.settings.reminderOverdue = overdue
          container.reminderScheduler.requestRefresh()
          record("settings.reminders.update")
        }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  val allowNegativeStock: Boolean
    get() = container.settings.allowNegativeStock

  fun setAllowNegativeStock(value: Boolean) {
    container.settings.allowNegativeStock = value
    viewModelScope.launch { record("settings.stock.update") }
  }

  fun createArea(name: String, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.createArea(name) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(onSuccess = { onResult(Result.success(Unit)) }, onFailure = { onResult(Result.failure(it)) })
    }
  }

  fun updateArea(areaId: String, name: String, active: Boolean, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.updateArea(areaId, name, active) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(onSuccess = { onResult(Result.success(Unit)) }, onFailure = { onResult(Result.failure(it)) })
    }
  }

  fun createCategory(
    name: String,
    parentId: String?,
    scope: CategoryScope,
    onResult: (Result<Unit>) -> Unit,
  ) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.createCategory(name, parentId, scope) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(onSuccess = { onResult(Result.success(Unit)) }, onFailure = { onResult(Result.failure(it)) })
    }
  }

  fun updateCategory(categoryId: String, name: String, active: Boolean, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.catalogRepository.updateCategory(categoryId, name, active) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(onSuccess = { onResult(Result.success(Unit)) }, onFailure = { onResult(Result.failure(it)) })
    }
  }

  fun createPaymentMethod(name: String, affectsCash: Boolean, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.commerceRepository.createPaymentMethod(name, affectsCash) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(onSuccess = { onResult(Result.success(Unit)) }, onFailure = { onResult(Result.failure(it)) })
    }
  }

  fun updatePaymentMethod(methodId: String, name: String, active: Boolean, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching { container.commerceRepository.updatePaymentMethod(methodId, name, active) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(onSuccess = { onResult(Result.success(Unit)) }, onFailure = { onResult(Result.failure(it)) })
    }
  }

  fun setAreaCost(areaId: String, percentBp: Int, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching {
          container.catalogRepository.setAreaCost(areaId, percentBp, ClinicTime.today())
        }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(onSuccess = { onResult(Result.success(Unit)) }, onFailure = { onResult(Result.failure(it)) })
    }
  }

  fun saveAgenda(hours: WeeklyHours, defaultDurationMinutes: Int, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      runCatching {
          require(defaultDurationMinutes in 5..480) { "La duración debe estar entre 5 y 480 minutos" }
          hours.days.forEach { day ->
            if (!day.closed) {
              require(day.openMinute < day.closeMinute) {
                "La apertura debe ser anterior al cierre"
              }
            }
          }
          container.settings.defaultDurationMinutes = defaultDurationMinutes
          container.settings.setBusinessHours(hours)
          record("settings.agenda.update")
        }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun setPin(pin: String, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      val result = container.securityRepository.setPin(pin)
      if (result.isSuccess) record("security.pin.create")
      onResult(result)
    }
  }

  fun changePin(currentPin: String, newPin: String, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      val result = container.securityRepository.changePin(currentPin, newPin)
      if (result.isSuccess) record("security.pin.change")
      onResult(result)
    }
  }

  fun removePin(currentPin: String, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      val result = container.securityRepository.removePin(currentPin)
      if (result.isSuccess) record("security.pin.remove")
      onResult(result)
    }
  }

  fun setBiometricEnabled(enabled: Boolean) {
    container.securityRepository.setBiometricEnabled(enabled)
    viewModelScope.launch {
      record(if (enabled) "security.biometric.enable" else "security.biometric.disable")
    }
  }

  fun lockNow() {
    container.securityRepository.lockNow()
  }

  fun exportBackup(passphrase: String, uri: Uri, onResult: (Result<Int>) -> Unit) {
    viewModelScope.launch {
      val chars = passphrase.toCharArray()
      try {
        onResult(container.backupRepository.export(chars, uri))
      } finally {
        chars.fill('\u0000')
      }
    }
  }

  fun restoreBackup(passphrase: String, uri: Uri, onResult: (Result<Int>) -> Unit) {
    viewModelScope.launch {
      val chars = passphrase.toCharArray()
      try {
        onResult(container.backupRepository.restore(chars, uri))
      } finally {
        chars.fill('\u0000')
      }
    }
  }

  private suspend fun record(action: String) {
    runCatching {
      container.auditRepository.record(
        entity = "security",
        entityId = null,
        action = action,
      )
    }
  }
}
