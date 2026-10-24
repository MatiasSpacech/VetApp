package com.veterapp.security

import android.os.SystemClock
import com.veterapp.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SecurityRepository(
  private val settings: AppSettings,
  private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
) {
  data class SecurityState(
    val hasPin: Boolean,
    val pinLength: Int,
    val biometricEnabled: Boolean,
    val locked: Boolean,
  )

  sealed interface PinResult {
    data object Success : PinResult

    data class Incorrect(val remainingAttempts: Int) : PinResult

    data class Blocked(val secondsRemaining: Long) : PinResult

    data object NotConfigured : PinResult
  }

  private val _state =
    MutableStateFlow(
      SecurityState(
        hasPin = settings.hasPin,
        pinLength = settings.pinLength,
        biometricEnabled = settings.biometricEnabled,
        locked = settings.hasPin,
      ),
    )
  val state: StateFlow<SecurityState> = _state.asStateFlow()

  private var backgroundedAt = 0L
  private var lastUnlockAt = 0L
  private var failedAttempts = 0
  private var blockedUntil = 0L

  fun onBackground() {
    backgroundedAt = elapsedRealtime()
  }

  fun onForeground() {
    val leftAt = backgroundedAt
    backgroundedAt = 0L
    if (leftAt <= 0L || !settings.hasPin) return
    val now = elapsedRealtime()
    if (now - leftAt >= BACKGROUND_GRACE_MS && now - lastUnlockAt >= BACKGROUND_GRACE_MS) {
      lockNow()
    }
  }

  fun lockNow() {
    if (settings.hasPin) updateState(locked = true)
  }

  fun unlock() {
    lastUnlockAt = elapsedRealtime()
    failedAttempts = 0
    blockedUntil = 0L
    updateState(locked = false)
  }

  suspend fun setPin(pin: String): Result<Unit> =
    withContext(Dispatchers.Default) {
      PinPolicy.error(pin)?.let { return@withContext Result.failure(IllegalArgumentException(it)) }
      storePin(pin)
    }

  suspend fun changePin(currentPin: String, newPin: String): Result<Unit> =
    withContext(Dispatchers.Default) {
      PinPolicy.error(newPin)?.let { return@withContext Result.failure(IllegalArgumentException(it)) }
      if (!matchesCurrent(currentPin)) {
        return@withContext Result.failure(IllegalArgumentException("El PIN actual no es correcto"))
      }
      storePin(newPin)
    }

  suspend fun removePin(currentPin: String): Result<Unit> =
    withContext(Dispatchers.Default) {
      if (!matchesCurrent(currentPin)) {
        return@withContext Result.failure(IllegalArgumentException("El PIN actual no es correcto"))
      }
      settings.pinHash = null
      settings.pinLength = 0
      settings.biometricEnabled = false
      unlock()
      Result.success(Unit)
    }

  suspend fun verifyPin(pin: String): PinResult =
    withContext(Dispatchers.Default) {
      val now = elapsedRealtime()
      val blockedSeconds = ((blockedUntil - now + 999L) / 1000L)
      if (blockedSeconds > 0L) return@withContext PinResult.Blocked(blockedSeconds)

      val stored = settings.pinHash ?: return@withContext PinResult.NotConfigured
      val entered = pin.toCharArray()
      val matches =
        try {
          PinHasher.verify(entered, stored)
        } finally {
          entered.fill('\u0000')
        }

      if (matches) {
        unlock()
        PinResult.Success
      } else {
        failedAttempts += 1
        if (failedAttempts >= MAX_ATTEMPTS) {
          failedAttempts = 0
          blockedUntil = elapsedRealtime() + BLOCK_MS
          PinResult.Blocked(BLOCK_MS / 1000L)
        } else {
          PinResult.Incorrect(MAX_ATTEMPTS - failedAttempts)
        }
      }
    }

  fun setBiometricEnabled(enabled: Boolean) {
    if (enabled && !settings.hasPin) return
    settings.biometricEnabled = enabled
    updateState()
  }

  private fun storePin(pin: String): Result<Unit> {
    val chars = pin.toCharArray()
    return try {
      settings.pinHash = PinHasher.hash(chars)
      settings.pinLength = pin.length
      unlock()
      Result.success(Unit)
    } catch (error: Throwable) {
      Result.failure(error)
    } finally {
      chars.fill('\u0000')
    }
  }

  private fun matchesCurrent(pin: String): Boolean {
    val stored = settings.pinHash ?: return false
    val entered = pin.toCharArray()
    return try {
      PinHasher.verify(entered, stored)
    } finally {
      entered.fill('\u0000')
    }
  }

  private fun updateState(locked: Boolean = _state.value.locked) {
    _state.value =
      SecurityState(
        hasPin = settings.hasPin,
        pinLength = settings.pinLength,
        biometricEnabled = settings.biometricEnabled,
        locked = settings.hasPin && locked,
      )
  }

  private companion object {
    const val BACKGROUND_GRACE_MS = 60_000L
    const val MAX_ATTEMPTS = 5
    const val BLOCK_MS = 30_000L
  }
}
