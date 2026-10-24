package com.veterapp.ui.clinical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Consultation
import com.veterapp.domain.ConsultationAddendum
import com.veterapp.ui.common.formatConsultationDate
import com.veterapp.ui.common.formatDate

@Composable
fun ClinicalHistoryTab(
  consultations: List<Consultation>,
  addenda: List<ConsultationAddendum>,
  onAddConsultation: () -> Unit,
  onAddAddendum: (Consultation) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
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
            "Historia clínica",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            "Las consultas cerradas no se editan: las correcciones van como addendum.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAddConsultation) { Text("Nueva") }
      }
    }
    if (consultations.isEmpty()) {
      item {
        Text(
          "Todavía no hay consultas registradas.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 24.dp),
        )
      }
    } else {
      items(consultations, key = { it.id }) { consultation ->
        ConsultationCard(
          consultation = consultation,
          addenda = addenda.filter { it.consultationId == consultation.id },
          onAddAddendum = { onAddAddendum(consultation) },
        )
      }
    }
    item { Spacer(Modifier.height(24.dp)) }
  }
}

@Composable
private fun ConsultationCard(
  consultation: Consultation,
  addenda: List<ConsultationAddendum>,
  onAddAddendum: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        formatConsultationDate(consultation.consultedAt),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        consultation.reason,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      ClinicalField("Examen", consultation.exam)
      ClinicalField("Diagnóstico", consultation.diagnosis)
      ClinicalField("Tratamiento", consultation.treatment)
      ClinicalField("Medicación", consultation.medication)
      ClinicalField("Indicaciones", consultation.instructions)

      if (addenda.isNotEmpty()) {
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Text(
          "Addendum",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        addenda.forEach { item ->
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              "${item.reason} · ${formatDate(item.createdAt)}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(item.text, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }

      TextButton(onClick = onAddAddendum, modifier = Modifier.align(Alignment.End)) {
        Text("Agregar addendum")
      }
    }
  }
}

@Composable
private fun ClinicalField(label: String, value: String?) {
  if (value.isNullOrBlank()) return
  Text(
    "$label: $value",
    style = MaterialTheme.typography.bodyMedium,
  )
}
