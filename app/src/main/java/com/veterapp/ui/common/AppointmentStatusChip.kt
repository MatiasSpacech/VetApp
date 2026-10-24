package com.veterapp.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.veterapp.domain.AppointmentStatus
import com.veterapp.theme.extendedColors

@Composable
fun AppointmentStatusChip(status: AppointmentStatus, modifier: Modifier = Modifier) {
  val (icon, container, content) =
    when (status) {
      AppointmentStatus.Scheduled ->
        Triple(AppIcons.Schedule, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
      AppointmentStatus.Confirmed ->
        Triple(AppIcons.CheckCircle, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
      AppointmentStatus.Attended ->
        Triple(AppIcons.MedicalServices, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
      AppointmentStatus.Cancelled ->
        Triple(AppIcons.Close, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
      AppointmentStatus.NoShow ->
        Triple(AppIcons.Warning, MaterialTheme.extendedColors.warningContainer, MaterialTheme.extendedColors.onWarningContainer)
    }
  StatusPill(
    label = status.label,
    icon = icon,
    modifier = modifier,
    container = container,
    contentColor = content,
  )
}