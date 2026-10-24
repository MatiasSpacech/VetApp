package com.veterapp.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veterapp.AppContainer
import com.veterapp.domain.WeeklyHours
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
  val securityState = container.securityRepository.state
  val lastBackupAt = container.settings.lastBackupAtFlow
  val auditEvents = container.auditRepository.recent

  fun businessHours(): WeeklyHours = container.settings.businessHours()

  fun defaultDurationMinutes(): Int = container.settings.defaultDurationMinutes

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
