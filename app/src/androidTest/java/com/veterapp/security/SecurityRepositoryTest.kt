package com.veterapp.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veterapp.data.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityRepositoryTest {
  private lateinit var settings: AppSettings
  private var now = 1_000L

  @Before
  fun setUp() {
    settings = AppSettings(ApplicationProvider.getApplicationContext<Context>())
    clearPin()
    now = 1_000L
  }

  @After
  fun tearDown() {
    clearPin()
  }

  @Test
  fun setPinLocksAndVerifies() {
    runBlocking {
      val security = repository()

      assertFalse(security.state.value.hasPin)
      security.setPin("1234").getOrThrow()

      assertTrue(security.state.value.hasPin)
      assertEquals(4, security.state.value.pinLength)
      assertFalse(security.state.value.locked)

      security.lockNow()
      assertTrue(security.state.value.locked)

      assertEquals(SecurityRepository.PinResult.Incorrect(4), security.verifyPin("0000"))
      assertTrue(security.state.value.locked)

      assertEquals(SecurityRepository.PinResult.Success, security.verifyPin("1234"))
      assertFalse(security.state.value.locked)
    }
  }

  @Test
  fun blocksAfterFiveFailedAttempts() {
    runBlocking {
      val security = repository()
      security.setPin("1234").getOrThrow()
      security.lockNow()

      assertEquals(SecurityRepository.PinResult.Incorrect(4), security.verifyPin("0000"))
      assertEquals(SecurityRepository.PinResult.Incorrect(3), security.verifyPin("0000"))
      assertEquals(SecurityRepository.PinResult.Incorrect(2), security.verifyPin("0000"))
      assertEquals(SecurityRepository.PinResult.Incorrect(1), security.verifyPin("0000"))

      val blocked = security.verifyPin("0000")
      assertTrue(blocked is SecurityRepository.PinResult.Blocked)
      assertEquals(30L, (blocked as SecurityRepository.PinResult.Blocked).secondsRemaining)
      assertTrue(security.state.value.locked)

      now += 31_000L
      assertEquals(SecurityRepository.PinResult.Success, security.verifyPin("1234"))
    }
  }

  @Test
  fun locksAfterGracePeriodInBackground() {
    val security = repository()
    runBlocking { security.setPin("1234").getOrThrow() }

    now = 10_000L
    security.onBackground()
    now = 40_000L
    security.onForeground()
    assertFalse(security.state.value.locked)

    now = 50_000L
    security.onBackground()
    now = 110_001L
    security.onForeground()
    assertTrue(security.state.value.locked)
  }

  @Test
  fun changesAndRemovesPin() {
    runBlocking {
      val security = repository()
      security.setPin("1234").getOrThrow()

      assertTrue(security.changePin("0000", "5678").isFailure)
      assertTrue(security.changePin("1234", "5678").isSuccess)
      assertEquals(SecurityRepository.PinResult.Success, security.verifyPin("5678"))

      assertTrue(security.removePin("0000").isFailure)
      assertTrue(security.removePin("5678").isSuccess)
      assertFalse(security.state.value.hasPin)
      assertFalse(security.state.value.locked)
    }
  }

  @Test
  fun biometricsRequirePin() {
    val security = repository()

    security.setBiometricEnabled(true)
    assertFalse(settings.biometricEnabled)

    runBlocking { security.setPin("1234").getOrThrow() }
    security.setBiometricEnabled(true)
    assertTrue(settings.biometricEnabled)

    runBlocking { security.removePin("1234").getOrThrow() }
    assertFalse(settings.biometricEnabled)
  }

  private fun repository(): SecurityRepository = SecurityRepository(settings) { now }

  private fun clearPin() {
    settings.pinHash = null
    settings.pinLength = 0
    settings.biometricEnabled = false
  }
}
