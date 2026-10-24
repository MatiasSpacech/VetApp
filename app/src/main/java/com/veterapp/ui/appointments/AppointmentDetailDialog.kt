package com.veterapp.ui.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Appointment
import com.veterapp.domain.AppointmentStatus
import com.veterapp.ui.common.AppointmentStatusChip
import com.veterapp.ui.common.formatDate
import com.veterapp.ui.common.formatTimeRange

@Composable
fun AppointmentDetailDialog(
  appointment: Appointment,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
  onCancel: (String?) -> Unit,
  onNoShow: () -> Unit,
  onAttend: () -> Unit,
  onEdit: () -> Unit,
  onOpenPet: () -> Unit,
  onWhatsApp: () -> Unit,
) {
  var showCancel by remember { mutableStateOf(false) }
  var cancelReason by rememberSaveable(appointment.id) { mutableStateOf("") }
  val isOpen = appointment.status.isOpen

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(appointment.petName)
        AppointmentStatusChip(appointment.status)
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          "${formatDate(appointment.startsAt)} · " +
            formatTimeRange(appointment.startsAt, appointment.endsAt),
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          "Duración: ${appointment.durationMinutes} minutos",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Propietario: ${appointment.ownerName}", style = MaterialTheme.typography.bodyMedium)
        Text(
          appointment.ownerPhone?.takeIf { it.isNotBlank() } ?: "Sin teléfono registrado",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        Text("Motivo: ${appointment.reason}", style = MaterialTheme.typography.bodyMedium)
        appointment.notes?.takeIf { it.isNotBlank() }?.let {
          Text("Notas: $it", style = MaterialTheme.typography.bodyMedium)
        }
        if (appointment.consultationId != null) {
          Text(
            "Consulta registrada en la historia clínica.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        if (appointment.cancelReason != null) {
          Text(
            "Motivo de cancelación: ${appointment.cancelReason}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        HorizontalDivider()
        if (appointment.status == AppointmentStatus.Scheduled) {
          OutlinedButton(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text("Confirmar turno")
          }
        }
        if (isOpen) {
          Button(onClick = onAttend, modifier = Modifier.fillMaxWidth()) {
            Text("Atender y registrar consulta")
          }
          OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text("Editar turno")
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showCancel = true }) {
              Text("Cancelar", color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = onNoShow) { Text("Marcar ausente") }
          }
        }
        OutlinedButton(
          onClick = onWhatsApp,
          enabled = appointment.ownerPhone?.isNotBlank() == true,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Enviar recordatorio por WhatsApp")
        }
        TextButton(onClick = onOpenPet) { Text("Ver ficha de la mascota") }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Cerrar") }
    },
  )

  if (showCancel) {
    AlertDialog(
      onDismissRequest = { showCancel = false },
      title = { Text("Cancelar turno") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "El turno quedará cancelado y no contará para superposiciones.",
            style = MaterialTheme.typography.bodyMedium,
          )
          OutlinedTextField(
            value = cancelReason,
            onValueChange = { cancelReason = it },
            label = { Text("Motivo (opcional)") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            showCancel = false
            onCancel(cancelReason)
          },
        ) {
          Text("Cancelar turno", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCancel = false }) { Text("Volver") }
      },
    )
  }
}
