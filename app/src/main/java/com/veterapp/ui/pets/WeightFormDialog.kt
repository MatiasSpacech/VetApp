package com.veterapp.ui.pets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.PetWeight
import com.veterapp.domain.PetWeightDraft
import com.veterapp.ui.common.AppFormSheet
import com.veterapp.ui.common.DateField
import com.veterapp.util.userMessage
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun WeightFormDialog(
  weight: PetWeight? = null,
  onSave: (PetWeightDraft, (Result<Unit>) -> Unit) -> Unit,
  onDismiss: () -> Unit,
) {
  var measuredOn by rememberSaveable(weight?.id) {
    mutableStateOf(
      weight?.let { ClinicTime.dateOf(it.measuredAt).toEpochDay() } ?: ClinicTime.todayEpochDay(),
    )
  }
  var weightValue by rememberSaveable(weight?.id) {
    mutableStateOf(
      weight?.let {
        java.math.BigDecimal(it.weightGrams)
          .divide(java.math.BigDecimal(1000))
          .stripTrailingZeros()
          .toPlainString()
      }.orEmpty(),
    )
  }
  var notes by rememberSaveable(weight?.id) { mutableStateOf(weight?.notes.orEmpty()) }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  fun save() {
    val kg = weightValue.replace(",", ".").toBigDecimalOrNull()
    if (kg == null) {
      error = "Ingresá el peso en kilogramos"
      return
    }
    val grams = kg.multiply(BigDecimal(1000)).setScale(0, RoundingMode.HALF_UP).toInt()
    val draft = PetWeightDraft(measuredOn = measuredOn, weightGrams = grams, notes = notes)
    draft.validate()?.let {
      error = it
      return
    }
    busy = true
    error = null
    onSave(draft) { result ->
      busy = false
      result
        .onSuccess { onDismiss() }
        .onFailure { error = it.userMessage("No se pudo guardar el peso") }
    }
  }

  AppFormSheet(
    title = if (weight == null) "Registrar peso" else "Editar peso",
    subtitle = "El peso se guarda en gramos.",
    confirmLabel = "Guardar peso",
    onConfirm = ::save,
    onDismiss = onDismiss,
    busy = busy,
    dirty = false,
  ) {        OutlinedTextField(
          value = weightValue,
          onValueChange = { value -> weightValue = value.filter { it.isDigit() || it == ',' || it == '.' } },
          label = { Text("Peso en kg *") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
        )
        DateField(
          label = "Fecha de medición",
          epochDay = measuredOn,
          onChange = { measuredOn = it },
          enabled = !busy,
          maxEpochDay = ClinicTime.todayEpochDay(),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notas") },
          minLines = 2,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
        error?.let {
          Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (busy) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Guardando…", style = MaterialTheme.typography.bodySmall)
          }
        }

  }

}
