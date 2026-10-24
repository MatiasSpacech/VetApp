package com.veterapp.ui.lock

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veterapp.security.BiometricUnlock
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.BrandMark
import com.veterapp.security.SecurityRepository
import com.veterapp.security.findFragmentActivity
import kotlinx.coroutines.launch

@Composable
fun AppLockGate(
  securityRepository: SecurityRepository,
  content: @Composable () -> Unit,
) {
  val state by securityRepository.state.collectAsStateWithLifecycle()
  val saveableStateHolder = rememberSaveableStateHolder()
  if (state.locked) {
    LockScreen(securityRepository = securityRepository)
  } else {
    saveableStateHolder.SaveableStateProvider(CONTENT_KEY) { content() }
  }
}

private const val CONTENT_KEY = "app-content"

@Composable
private fun LockScreen(securityRepository: SecurityRepository) {
  val state by securityRepository.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val activity = remember(context) { context.findFragmentActivity() }
  val biometricAvailable = remember(context) { BiometricUnlock.isAvailable(context) }
  val pinLength = state.pinLength.takeIf { it in 4..8 } ?: 4

  var pin by rememberSaveable { mutableStateOf("") }
  var error by rememberSaveable { mutableStateOf<String?>(null) }
  var verifying by remember { mutableStateOf(false) }
  var autoPrompted by rememberSaveable { mutableStateOf(false) }

  fun submit() {
    if (verifying || pin.isBlank()) return
    verifying = true
    error = null
    scope.launch {
      try {
        when (val result = securityRepository.verifyPin(pin)) {
          is SecurityRepository.PinResult.Success -> Unit
          is SecurityRepository.PinResult.Incorrect ->
            error = "PIN incorrecto. Te quedan ${result.remainingAttempts} intento(s)"
          is SecurityRepository.PinResult.Blocked ->
            error = "Demasiados intentos. Esperá ${result.secondsRemaining} segundos"
          is SecurityRepository.PinResult.NotConfigured ->
            error = "No hay un PIN configurado"
        }
      } finally {
        pin = ""
        verifying = false
      }
    }
  }

  fun promptBiometric() {
    val host = activity ?: return
    error = null
    BiometricUnlock.authenticate(
      activity = host,
      title = "Desbloquear VeterApp",
      subtitle = "Usá tu huella o reconocimiento facial",
      onSuccess = { securityRepository.unlock() },
      onError = { code, message ->
        error =
          when (code) {
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_CANCELED -> null
            else -> message.toString()
          }
      },
    )
  }

  LaunchedEffect(pin) {
    if (!verifying && pinLength > 0 && pin.length == pinLength) submit()
  }

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    if (!autoPrompted && state.biometricEnabled && biometricAvailable) {
      autoPrompted = true
      promptBiometric()
    }
  }

  BackHandler(enabled = true) {}

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              MaterialTheme.colorScheme.background,
              MaterialTheme.colorScheme.surfaceContainerHigh,
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            ),
          ),
        ),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(min = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        BrandMark(size = 76.dp)
        Spacer(Modifier.height(16.dp))
        Text(
          "VeterApp",
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          "Desbloqueá para continuar",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Surface(
          shape = RoundedCornerShape(28.dp),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 6.dp,
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            PinDots(entered = pin.length, total = pinLength)
            Spacer(Modifier.height(14.dp))
            error?.let {
              Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
              )
              Spacer(Modifier.height(8.dp))
            }
            if (verifying) {
              CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
              Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(6.dp))
            PinKeypad(
              enabled = !verifying,
              onDigit = { digit ->
                if (pin.length < pinLength) {
                  pin += digit
                  error = null
                }
              },
              onBackspace = {
                pin = pin.dropLast(1)
                error = null
              },
              onUnlock = ::submit,
              unlockEnabled = pin.isNotBlank() && !verifying,
            )
            if (state.biometricEnabled && biometricAvailable) {
              Spacer(Modifier.height(10.dp))
              TextButton(onClick = ::promptBiometric, enabled = !verifying) {
                Icon(
                  painter = painterResource(AppIcons.Fingerprint),
                  contentDescription = null,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Usar biometría")
              }
            } else if (state.biometricEnabled && !biometricAvailable) {
              Spacer(Modifier.height(10.dp))
              Text(
                "La biometría no está disponible en este dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
        Spacer(Modifier.height(16.dp))
        Text(
          "PIN de $pinLength dígitos",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
@Composable
private fun PinDots(entered: Int, total: Int) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    repeat(total) { index ->
      Box(
        modifier =
          Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(
              if (index < entered) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
      )
    }
  }
}

@Composable
private fun PinKeypad(
  enabled: Boolean,
  onDigit: (String) -> Unit,
  onBackspace: () -> Unit,
  onUnlock: () -> Unit,
  unlockEnabled: Boolean,
) {
  val rows = listOf("123", "456", "789")
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    rows.forEach { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        row.forEach { digit ->
          PinKey(digit.toString(), enabled) { onDigit(digit.toString()) }
        }
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      PinKey("⌫", enabled, onClick = onBackspace)
      PinKey("0", enabled) { onDigit("0") }
      Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Button(
          onClick = onUnlock,
          enabled = enabled && unlockEnabled,
          modifier = Modifier.size(72.dp),
          shape = RoundedCornerShape(20.dp),
        ) {
          Icon(
            painter = painterResource(AppIcons.Check),
            contentDescription = "Desbloquear",
            modifier = Modifier.size(26.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun PinKey(
  label: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.size(72.dp),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor = MaterialTheme.colorScheme.onSurface,
    border =
      androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
      ),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
    }
  }
}