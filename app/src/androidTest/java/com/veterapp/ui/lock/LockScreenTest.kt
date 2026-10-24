package com.veterapp.ui.lock

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veterapp.data.AppSettings
import com.veterapp.security.SecurityRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LockScreenTest {
  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var settings: AppSettings
  private lateinit var security: SecurityRepository

  @Before
  fun setUp() {
    settings = AppSettings(ApplicationProvider.getApplicationContext<Context>())
    clearPin()
    security = SecurityRepository(settings)
    runBlocking {
      security.setPin("1234").getOrThrow()
      security.lockNow()
    }
  }

  @After
  fun tearDown() {
    clearPin()
  }

  @Test
  fun unlocksWithPin() {
    composeRule.setContent {
      AppLockGate(securityRepository = security) {
        Text("Contenido privado")
      }
    }

    composeRule.onNodeWithText("App bloqueada").assertIsDisplayed()
    assertNoContent()

    listOf("1", "2", "3", "4").forEach { digit ->
      composeRule.onNodeWithText(digit).performClick()
    }

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText("Contenido privado").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText("Contenido privado").assertIsDisplayed()
  }

  @Test
  fun wrongPinKeepsContentHidden() {
    composeRule.setContent {
      AppLockGate(securityRepository = security) {
        Text("Contenido privado")
      }
    }

    listOf("0", "0", "0", "0").forEach { digit ->
      composeRule.onNodeWithText(digit).performClick()
    }

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText("PIN incorrecto", substring = true).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText("PIN incorrecto", substring = true).assertIsDisplayed()
    assertNoContent()
  }

  private fun assertNoContent() {
    assertFalse(
      composeRule.onAllNodesWithText("Contenido privado").fetchSemanticsNodes().isNotEmpty(),
    )
  }

  private fun clearPin() {
    settings.pinHash = null
    settings.pinLength = 0
    settings.biometricEnabled = false
  }
}
