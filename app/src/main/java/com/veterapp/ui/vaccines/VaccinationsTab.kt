package com.veterapp.ui.vaccines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.Vaccination
import com.veterapp.domain.latestPerPetAndVaccine
import com.veterapp.ui.common.VaccineStatusChip
import com.veterapp.ui.common.formatEpochDay
import java.time.LocalDate

@Composable
fun VaccinationsTab(
  vaccinations: List<Vaccination>,
  onAddVaccination: () -> Unit,
  onApplyDose: (Vaccination) -> Unit,
  onReschedule: (Vaccination) -> Unit,
  modifier: Modifier = Modifier,
) {
  val today = ClinicTime.today()
  val latestIds = vaccinations.latestPerPetAndVaccine().map { it.id }.toSet()

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(Modifier.weight(1f)) {
          Text(
            "Vacunaciones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            "Los avisos se calculan 30 y 7 días antes y al vencer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAddVaccination) { Text("Registrar") }
      }
    }
    if (vaccinations.isEmpty()) {
      item {
        Text(
          "Todavía no hay vacunas registradas.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 24.dp),
        )
      }
    } else {
      items(vaccinations, key = { it.id }) { vaccination ->
        VaccinationCard(
          vaccination = vaccination,
          today = today,
          isLatest = vaccination.id in latestIds,
          onApplyDose = { onApplyDose(vaccination) },
          onReschedule = { onReschedule(vaccination) },
        )
      }
    }
    item { Spacer(Modifier.height(24.dp)) }
  }
}

@Composable
private fun VaccinationCard(
  vaccination: Vaccination,
  today: LocalDate,
  isLatest: Boolean,
  onApplyDose: () -> Unit,
  onReschedule: () -> Unit,
) {
  val status = if (isLatest) vaccination.status(today) else null
  val days = if (isLatest) vaccination.daysUntilDue(today) else null

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(
            vaccination.vaccineName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            "Aplicada el ${formatEpochDay(vaccination.appliedOn)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        status?.let { VaccineStatusChip(it) }
      }

      if (!isLatest) {
        Text(
          "Aplicación anterior",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        val nextDue = vaccination.nextDueOn
        if (nextDue == null) {
          Text("Sin próximo aviso programado.", style = MaterialTheme.typography.bodyMedium)
        } else {
          Text(
            "Próxima dosis: ${formatEpochDay(nextDue)}",
            style = MaterialTheme.typography.bodyMedium,
          )
          days?.let { remaining ->
            val label =
              when {
                remaining < 0 -> "Vencida hace ${-remaining} días"
                remaining == 0L -> "Vence hoy"
                remaining == 1L -> "Vence mañana"
                else -> "Faltan $remaining días"
              }
            Text(
              label,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onReschedule) { Text("Reprogramar") }
            OutlinedButton(onClick = onApplyDose) { Text("Aplicar dosis") }
          }
        }
      }

      vaccination.lot?.let {
        Text("Lote: $it", style = MaterialTheme.typography.bodySmall)
      }
      vaccination.notes?.let {
        Text(it, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}
