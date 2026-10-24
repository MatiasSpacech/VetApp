package com.veterapp.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.veterapp.domain.Client
import com.veterapp.domain.Pet
import com.veterapp.domain.PetStatus
import com.veterapp.domain.VaccinationAlert
import com.veterapp.domain.VaccineStatus
import com.veterapp.ui.common.VaccineStatusChip
import com.veterapp.ui.common.formatDateTime
import com.veterapp.ui.common.formatEpochDay
import com.veterapp.ui.main.MainUiState

@Composable
fun HomeTab(
  state: MainUiState,
  clients: List<Client>,
  pets: List<Pet>,
  alerts: List<VaccinationAlert>,
  onOpenPet: (String) -> Unit,
  onRefreshReminders: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var notificationsEnabled by remember { mutableStateOf(notificationsAllowed(context)) }
  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      notificationsEnabled = notificationsAllowed(context)
      if (granted) onRefreshReminders()
    }

  LifecycleResumeEffect(Unit) {
    notificationsEnabled = notificationsAllowed(context)
    onPauseOrDispose { }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      "Resumen",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      StatCard(
        label = "Clientes",
        value = clients.size,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f),
      )
      StatCard(
        label = "Mascotas activas",
        value = pets.count { it.status == PetStatus.Active },
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f),
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      StatCard(
        label = "Inactivas",
        value = pets.count { it.status == PetStatus.Inactive },
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.weight(1f),
      )
      StatCard(
        label = "Fallecidas",
        value = pets.count { it.status == PetStatus.Deceased },
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.weight(1f),
      )
    }

    VaccineAlertsCard(alerts = alerts, onOpenPet = onOpenPet)

    if (!notificationsEnabled) {
      NotificationPermissionCard(
        onEnable = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          } else {
            openNotificationSettings(context)
          }
        },
      )
    }

    Spacer(Modifier.height(4.dp))
    SyncSummary(state)
    Spacer(Modifier.height(24.dp))
  }
}

@Composable
private fun StatCard(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(
        value.toString(),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = color,
      )
      Text(
        label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun VaccineAlertsCard(alerts: List<VaccinationAlert>, onOpenPet: (String) -> Unit) {
  val overdue = alerts.count { it.status == VaccineStatus.Overdue }
  val dueSoon = alerts.count { it.status == VaccineStatus.DueSoon }
  val upcoming = alerts.count { it.status == VaccineStatus.Upcoming }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          "Vacunas",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f),
        )
        if (alerts.isNotEmpty()) {
          Text(
            "${alerts.size} aviso(s)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (alerts.isEmpty()) {
        Text(
          "No hay vacunas próximas ni vencidas.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Text(
          "Vencidas: $overdue · Por vencer: $dueSoon · Próximas: $upcoming",
          style = MaterialTheme.typography.bodySmall,
          color =
            if (overdue > 0) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        alerts.take(5).forEach { alert ->
          AlertRow(alert = alert, onOpenPet = onOpenPet)
        }
        if (alerts.size > 5) {
          Text(
            "Y ${alerts.size - 5} aviso(s) más.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun AlertRow(alert: VaccinationAlert, onOpenPet: (String) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text(
        "${alert.petName} · ${alert.vaccination.vaccineName}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        dueLabel(alert),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    VaccineStatusChip(alert.status)
    TextButton(onClick = { onOpenPet(alert.vaccination.petId) }) { Text("Ver") }
  }
}

private fun dueLabel(alert: VaccinationAlert): String =
  when {
    alert.daysUntilDue < 0 -> "Venció el ${formatEpochDay(alert.vaccination.nextDueOn!!)}"
    alert.daysUntilDue == 0L -> "Vence hoy"
    alert.daysUntilDue == 1L ->
      "Vence mañana (${formatEpochDay(alert.vaccination.nextDueOn!!)})"
    else ->
      "Vence el ${formatEpochDay(alert.vaccination.nextDueOn!!)} " +
        "(en ${alert.daysUntilDue} días)"
  }

@Composable
private fun NotificationPermissionCard(onEnable: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        "Avisos de vacunas",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
      Text(
        "Activá las notificaciones para recibir los avisos a 30 y 7 días y los vencimientos.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
      Button(onClick = onEnable) { Text("Activar avisos") }
      Text(
        "Si el sistema no muestra el pedido, habilitalas desde Ajustes > Notificaciones.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
    }
  }
}

@Composable
private fun SyncSummary(state: MainUiState) {
  val status =
    when {
      !state.configured -> "Modo local"
      !state.online -> "Sin conexión"
      state.syncing -> "Sincronizando…"
      state.pendingCount > 0 -> "${state.pendingCount} cambio(s) pendiente(s)"
      else -> "Sincronizado"
    }
  val color =
    when {
      !state.configured -> MaterialTheme.colorScheme.onSurfaceVariant
      !state.online -> MaterialTheme.colorScheme.error
      state.pendingCount > 0 -> MaterialTheme.colorScheme.tertiary
      else -> MaterialTheme.colorScheme.primary
    }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(status, color = color, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(4.dp))
      Text(
        "Último sync: ${formatDateTime(state.lastSyncAt)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      val backupPending =
        state.lastBackupAt <= 0L ||
          System.currentTimeMillis() - state.lastBackupAt > 7L * 24L * 60L * 60L * 1000L
      if (backupPending) {
        Spacer(Modifier.height(4.dp))
        Text(
          "Backup semanal pendiente: abrí Ajustes",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
      if (state.syncError != null) {
        Spacer(Modifier.height(6.dp))
        Text(
          state.syncError,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

private fun notificationsAllowed(context: Context): Boolean {
  if (
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
  ) {
    return false
  }
  return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun openNotificationSettings(context: Context) {
  val intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
      putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  runCatching { context.startActivity(intent) }
}
