package com.veterapp.data

import android.content.Context
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(context: Context) {
  private val preferences = context.getSharedPreferences("veterapp_settings", Context.MODE_PRIVATE)
  private val _lastBackupAt = MutableStateFlow(preferences.getLong(KEY_LAST_BACKUP_AT, 0L))

  val lastBackupAtFlow: StateFlow<Long> = _lastBackupAt.asStateFlow()

  val deviceId: String
    get() =
      preferences.getString(KEY_DEVICE_ID, null)
        ?: UUID.randomUUID().toString().also { preferences.edit().putString(KEY_DEVICE_ID, it).apply() }

  var clinicId: String?
    get() = preferences.getString(KEY_CLINIC_ID, null)
    set(value) {
      preferences.edit().putString(KEY_CLINIC_ID, value).apply()
    }

  var userId: String?
    get() = preferences.getString(KEY_USER_ID, null)
    set(value) {
      preferences.edit().putString(KEY_USER_ID, value).apply()
    }

  val localClinicId: String
    get() =
      preferences.getString(KEY_LOCAL_CLINIC_ID, null)
        ?: UUID.randomUUID().toString().also {
          preferences.edit().putString(KEY_LOCAL_CLINIC_ID, it).apply()
        }

  var lastSyncAt: Long
    get() = preferences.getLong(KEY_LAST_SYNC_AT, 0L)
    set(value) {
      preferences.edit().putLong(KEY_LAST_SYNC_AT, value).apply()
    }

  var lastBackupAt: Long
    get() = _lastBackupAt.value
    set(value) {
      preferences.edit().putLong(KEY_LAST_BACKUP_AT, value).apply()
      _lastBackupAt.value = value
    }

  var pinHash: String?
    get() = preferences.getString(KEY_PIN_HASH, null)
    set(value) {
      preferences.edit().putString(KEY_PIN_HASH, value).apply()
    }

  var pinLength: Int
    get() = preferences.getInt(KEY_PIN_LENGTH, 0)
    set(value) {
      preferences.edit().putInt(KEY_PIN_LENGTH, value).apply()
    }

  var biometricEnabled: Boolean
    get() = preferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    set(value) {
      preferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()
    }

  val hasPin: Boolean
    get() = pinHash != null

  private companion object {
    const val KEY_DEVICE_ID = "device_id"
    const val KEY_CLINIC_ID = "clinic_id"
    const val KEY_USER_ID = "user_id"
    const val KEY_LOCAL_CLINIC_ID = "local_clinic_id"
    const val KEY_LAST_SYNC_AT = "last_sync_at"
    const val KEY_LAST_BACKUP_AT = "last_backup_at"
    const val KEY_PIN_HASH = "pin_hash"
    const val KEY_PIN_LENGTH = "pin_length"
    const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
  }
}
