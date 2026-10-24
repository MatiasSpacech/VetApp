package com.veterapp.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veterapp.AppContainer
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
  val securityState = container.securityRepository.state
  val lastBackupAt = container.settings.lastBackupAtFlow
  val auditEvents = container.auditRepository.recent

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
