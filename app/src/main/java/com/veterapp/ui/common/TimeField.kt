package com.veterapp.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
  label: String,
  minuteOfDay: Int,
  onChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  var showPicker by remember { mutableStateOf(false) }
  val hour = (minuteOfDay / 60).coerceIn(0, 23)
  val minute = (minuteOfDay % 60).coerceIn(0, 59)

  OutlinedButton(
    onClick = { showPicker = true },
    enabled = enabled,
    modifier = modifier.fillMaxWidth(),
  ) {
    Text("$label: ${formatMinuteOfDay(minuteOfDay)}")
  }

  if (showPicker) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
      onDismissRequest = { showPicker = false },
      title = { Text(label) },
      text = { TimePicker(state = state) },
      confirmButton = {
        TextButton(
          onClick = {
            onChange(state.hour * 60 + state.minute)
            showPicker = false
          },
        ) {
          Text("Aceptar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
      },
    )
  }
}

fun formatMinuteOfDay(minuteOfDay: Int): String =
  "%02d:%02d".format((minuteOfDay / 60) % 24, minuteOfDay % 60)
