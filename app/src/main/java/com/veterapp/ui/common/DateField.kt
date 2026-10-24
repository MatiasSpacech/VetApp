package com.veterapp.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.veterapp.domain.ClinicTime

private const val DAY_MILLIS = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
  label: String,
  epochDay: Long?,
  onChange: (Long) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  minEpochDay: Long? = null,
  maxEpochDay: Long? = null,
) {
  var showPicker by remember { mutableStateOf(false) }
  val selectableDates =
    remember(minEpochDay, maxEpochDay) {
      object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
          val day = utcTimeMillis / DAY_MILLIS
          if (minEpochDay != null && day < minEpochDay) return false
          if (maxEpochDay != null && day > maxEpochDay) return false
          return true
        }
      }
    }

  OutlinedButton(
    onClick = { showPicker = true },
    enabled = enabled,
    modifier = modifier.fillMaxWidth(),
  ) {
    Text("$label: ${epochDay?.let(::formatEpochDay) ?: "Elegir"}")
  }

  if (showPicker) {
    val state =
      rememberDatePickerState(
        initialSelectedDateMillis = (epochDay ?: ClinicTime.todayEpochDay()) * DAY_MILLIS,
        selectableDates = selectableDates,
      )
    DatePickerDialog(
      onDismissRequest = { showPicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            state.selectedDateMillis?.let { onChange(it / DAY_MILLIS) }
            showPicker = false
          },
        ) {
          Text("Aceptar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
      },
    ) {
      DatePicker(state = state)
    }
  }
}
