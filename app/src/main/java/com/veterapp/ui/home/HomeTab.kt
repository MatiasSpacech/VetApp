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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.veterapp.domain.Appointment
import com.veterapp.domain.Client
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.DashboardData
import com.veterapp.domain.Pet
import com.veterapp.domain.PetStatus
import com.veterapp.domain.VaccinationAlert
import com.veterapp.domain.VaccineStatus
import com.veterapp.domain.formatMoney
import com.veterapp.domain.formatVariation
import com.veterapp.theme.AppSpacing
import com.veterapp.theme.extendedColors
import com.veterapp.ui.common.AppCard
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.AppointmentStatusChip
import com.veterapp.ui.common.GradientHero
import com.veterapp.ui.common.IconBadge
import com.veterapp.ui.common.MiniBarChart
import com.veterapp.ui.common.QuickActionTile
import com.veterapp.ui.common.SectionHeader
import com.veterapp.ui.common.StatTile
import com.veterapp.ui.common.TrendBadge
import com.veterapp.ui.common.VaccineStatusChip
import com.veterapp.ui.common.formatDateTime
import com.veterapp.ui.common.formatEpochDay
import com.veterapp.ui.common.formatTime
import com.veterapp.ui.main.MainUiState
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class QuickAction(val label: String) {
  Client("Registrar cliente"),
  Pet("Registrar mascota"),
  Appointment("Crear turno"),
  Consultation("Registrar consulta"),
  Vaccination("Cargar vacunación"),
  Item("Registrar producto o servicio"),
  Sale("Realizar venta"),
}

private data class QuickActionVisual(
  val action: QuickAction,
  val icon: Int,
  val accent: Color,
)

@Composable
fun HomeTab(
  state: MainUiState,
  clients: List<Client>,
  pets: List<Pet>,
  alerts: List<VaccinationAlert>,
  todayAppointments: List<Appointment>,
  dashboard: DashboardData?,
  onOpenPet: (String) -> Unit,
  onOpenAppointments: () -> Unit,
  onOpenReports: () -> Unit,
  onQuickAction: (QuickAction) -> Unit,
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

  val activePets = pets.count { it.status == PetStatus.Active }
  val quickActions =
    listOf(
      QuickActionVisual(QuickAction.Client, AppIcons.Person, MaterialTheme.colorScheme.primary),
      QuickActionVisual(QuickAction.Pet, AppIcons.Pets, MaterialTheme.colorScheme.secondary),
      QuickActionVisual(QuickAction.Appointment, AppIcons.EventNote, MaterialTheme.colorScheme.tertiary),
      QuickActionVisual(QuickAction.Consultation, AppIcons.MedicalServices, MaterialTheme.colorScheme.primary),
      QuickActionVisual(QuickAction.Vaccination, AppIcons.Vaccines, MaterialTheme.colorScheme.secondary),
      QuickActionVisual(QuickAction.Item, AppIcons.Inventory, MaterialTheme.colorScheme.tertiary),
      QuickActionVisual(QuickAction.Sale, AppIcons.Sales, MaterialTheme.colorScheme.primary),
    )

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding =
      androidx.compose.foundation.layout.PaddingValues(
        start = AppSpacing.lg,
        end = AppSpacing.lg,
        top = AppSpacing.xs,
        bottom = 96.dp,
      ),
    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
  ) {
    item {
      HomeHero(
        totalClients = clients.size,
        activePets = activePets,
        todayAppointments = todayAppointments.size,
      )
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        StatTile(
          label = "Clientes",
          value = clients.size.toString(),
          icon = AppIcons.Clients,
          modifier = Modifier.weight(1f),
        )
        StatTile(
          label = "Mascotas activas",
          value = activePets.toString(),
          icon = AppIcons.Pets,
          accent = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        StatTile(
          label = "Turnos hoy",
          value = todayAppointments.size.toString(),
          icon = AppIcons.Agenda,
          accent = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.weight(1f),
        )
        StatTile(
          label = "Facturación hoy",
          value = formatCompactMoney(dashboard?.today?.netCents ?: 0L),
          icon = AppIcons.Payments,
          accent = MaterialTheme.extendedColors.success,
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      SectionHeader(title = "Accesos rápidos")
    }
    items(quickActions.chunked(4)) { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        row.forEach { visual ->
          QuickActionTile(
            label = visual.action.label,
            icon = visual.icon,
            accent = visual.accent,
            onClick = { onQuickAction(visual.action) },
            modifier = Modifier.weight(1f),
          )
        }
        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
      }
    }

    dashboard?.let { data ->
      item { EconomicCard(data = data, onOpenReports = onOpenReports) }
    }

    item {
      AppointmentsCard(
        appointments = todayAppointments,
        onOpenAppointments = onOpenAppointments,
      )
    }

    item {
      VaccinesCard(alerts = alerts, onOpenPet = onOpenPet)
    }

    if (!notificationsEnabled) {
      item {
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
    }

    item { SyncSummary(state) }
  }
}

@Composable
private fun HomeHero(
  totalClients: Int,
  activePets: Int,
  todayAppointments: Int,
) {
  val hour = LocalTime.now(ClinicTime.zone).hour
  val greeting =
    when {
      hour < 12 -> "Buen día"
      hour < 19 -> "Buenas tardes"
      else -> "Buenas noches"
    }
  val date =
    ClinicTime.today()
      .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "AR")))
      .replaceFirstChar { it.uppercase() }

  GradientHero {
    Text(
      date,
      style = MaterialTheme.typography.labelLarge,
      color = Color.White.copy(alpha = 0.82f),
    )
    Spacer(Modifier.height(4.dp))
    Text(
      greeting,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = Color.White,
    )
    Text(
      "Este es el pulso de tu veterinaria.",
      style = MaterialTheme.typography.bodyMedium,
      color = Color.White.copy(alpha = 0.85f),
    )
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      HeroStat("Clientes", totalClients.toString())
      HeroStat("Mascotas", activePets.toString())
      HeroStat("Turnos", todayAppointments.toString())
    }
  }
}

@Composable
private fun HeroStat(label: String, value: String) {
  Surface(
    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    color = Color.White.copy(alpha = 0.15f),
  ) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
      Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
      Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
  }
}

@Composable
private fun EconomicCard(data: DashboardData, onOpenReports: () -> Unit) {
  AppCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconBadge(icon = AppIcons.Insights, tint = MaterialTheme.extendedColors.success)
      Spacer(Modifier.width(10.dp))
      Column(Modifier.weight(1f)) {
        Text("Resumen económico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
          "Estimado del día",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      TextButton(onClick = onOpenReports) {
        Text("Ver")
        Icon(
          painter = painterResource(AppIcons.ChevronRight),
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
    }
    Spacer(Modifier.height(10.dp))
    Text(
      formatMoney(data.today.netCents),
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        "vs. ayer ${formatVariation(data.today.netCents, data.yesterday.netCents)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.width(8.dp))
      TrendBadge(
        label = formatVariation(data.month.netCents, data.previousMonth.netCents),
        positive = data.month.netCents >= data.previousMonth.netCents,
      )
    }
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
      MiniMetric("Mes", formatCompactMoney(data.month.netCents), Modifier.weight(1f))
      MiniMetric("Año", formatCompactMoney(data.year.netCents), Modifier.weight(1f))
      MiniMetric(
        "Ticket",
        data.today.ticketAverageCents?.let(::formatCompactMoney) ?: "—",
        Modifier.weight(1f),
      )
    }
    if (data.topAreas.isNotEmpty()) {
      Spacer(Modifier.height(12.dp))
      MiniBarChart(
        values = data.topAreas.map { it.netCents.toFloat() },
        height = 64.dp,
        color = MaterialTheme.colorScheme.secondary,
      )
      Text(
        "Facturación por área de hoy",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "Resultado estimado, no contabilidad formal.",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun MiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
  Column(modifier) {
    Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun AppointmentsCard(
  appointments: List<Appointment>,
  onOpenAppointments: () -> Unit,
) {
  AppCard {
    SectionHeader(
      title = "Turnos de hoy",
      actionLabel = "Ver agenda",
      onAction = onOpenAppointments,
    )
    if (appointments.isEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconBadge(icon = AppIcons.EventNote, tint = MaterialTheme.colorScheme.primary, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Text(
          "No hay turnos agendados para hoy.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      appointments.take(4).forEach { appointment ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              formatTime(appointment.startsAt),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              fontWeight = FontWeight.SemiBold,
            )
          }
          Spacer(Modifier.width(10.dp))
          Column(Modifier.weight(1f)) {
            Text(
              "${appointment.petName} · ${appointment.ownerName}",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              appointment.reason,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          AppointmentStatusChip(appointment.status)
        }
      }
      if (appointments.size > 4) {
        Text(
          "Y ${appointments.size - 4} turno(s) más.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun VaccinesCard(alerts: List<VaccinationAlert>, onOpenPet: (String) -> Unit) {
  val overdue = alerts.count { it.status == VaccineStatus.Overdue }
  AppCard {
    SectionHeader(title = "Vacunas")
    if (alerts.isEmpty()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(
          icon = AppIcons.CheckCircle,
          tint = MaterialTheme.extendedColors.success,
          size = 36.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
          "Todo al día. No hay vacunas próximas ni vencidas.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      Text(
        "${alerts.size} aviso(s) · ${overdue} vencida(s)",
        style = MaterialTheme.typography.bodySmall,
        color = if (overdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
      )
      alerts.take(3).forEach { alert ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconBadge(
            icon = AppIcons.Vaccines,
            tint =
              if (alert.status == VaccineStatus.Overdue) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.secondary,
            size = 34.dp,
          )
          Spacer(Modifier.width(10.dp))
          Column(Modifier.weight(1f)) {
            Text(
              "${alert.petName} · ${alert.vaccination.vaccineName}",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              dueLabel(alert),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          VaccineStatusChip(alert.status)
          TextButton(onClick = { onOpenPet(alert.vaccination.petId) }) {
            Text("Ver")
          }
        }
      }
      if (alerts.size > 3) {
        Text(
          "Y ${alerts.size - 3} aviso(s) más.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

private fun dueLabel(alert: VaccinationAlert): String =
  when {
    alert.daysUntilDue < 0 -> "Venció el ${formatEpochDay(alert.vaccination.nextDueOn!!)}"
    alert.daysUntilDue == 0L -> "Vence hoy"
    alert.daysUntilDue == 1L -> "Vence mañana"
    else -> "Vence en ${alert.daysUntilDue} días"
  }

@Composable
private fun NotificationPermissionCard(onEnable: () -> Unit) {
  AppCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconBadge(icon = AppIcons.NotificationsActive, tint = MaterialTheme.extendedColors.warning)
      Spacer(Modifier.width(10.dp))
      Column(Modifier.weight(1f)) {
        Text(
          "Activá los avisos de vacunas",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          "Para recibir recordatorios aunque la app esté cerrada.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Spacer(Modifier.height(10.dp))
    Button(onClick = onEnable) {
      Icon(
        painter = painterResource(AppIcons.Notifications),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(8.dp))
      Text("Activar avisos")
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
      else -> "Todo sincronizado"
    }
  val icon =
    when {
      !state.configured -> AppIcons.CloudOff
      !state.online -> AppIcons.CloudOff
      state.syncing -> AppIcons.Sync
      state.pendingCount > 0 -> AppIcons.Sync
      else -> AppIcons.CloudDone
    }
  val tint =
    when {
      !state.configured -> MaterialTheme.colorScheme.onSurfaceVariant
      !state.online -> MaterialTheme.colorScheme.error
      state.pendingCount > 0 -> MaterialTheme.extendedColors.warning
      else -> MaterialTheme.extendedColors.success
    }
  AppCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconBadge(icon = icon, tint = tint, size = 38.dp)
      Spacer(Modifier.width(10.dp))
      Column(Modifier.weight(1f)) {
        Text(status, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
          "Último sync: ${formatDateTime(state.lastSyncAt)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    val backupPending =
      state.lastBackupAt <= 0L ||
        System.currentTimeMillis() - state.lastBackupAt > 7L * 24L * 60L * 60L * 1000L
    if (backupPending) {
      Spacer(Modifier.height(8.dp))
      Text(
        "Backup semanal pendiente. Abrí Ajustes > Backup.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
      )
    }
    state.syncError?.let {
      Spacer(Modifier.height(6.dp))
      Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
  }
}

private fun formatCompactMoney(cents: Long): String {
  val amount = cents / 100.0
  return when {
    kotlin.math.abs(amount) >= 1_000_000 ->
      String.format(Locale("es", "AR"), "$%.1fM", amount / 1_000_000)
    kotlin.math.abs(amount) >= 10_000 ->
      String.format(Locale("es", "AR"), "$%.0f", amount)
    else -> formatMoney(cents)
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
