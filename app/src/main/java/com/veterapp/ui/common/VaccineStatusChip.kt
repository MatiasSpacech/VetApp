package com.veterapp.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.veterapp.domain.VaccineStatus
import com.veterapp.theme.extendedColors

@Composable
fun VaccineStatusChip(status: VaccineStatus, modifier: Modifier = Modifier) {
  val (icon, container, content) =
    when (status) {
      VaccineStatus.Overdue ->
        Triple(AppIcons.Warning, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
      VaccineStatus.DueSoon ->
        Triple(
          AppIcons.NotificationsActive,
          MaterialTheme.extendedColors.warningContainer,
          MaterialTheme.extendedColors.onWarningContainer,
        )
      VaccineStatus.Upcoming ->
        Triple(AppIcons.Notifications, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
      VaccineStatus.Scheduled ->
        Triple(AppIcons.Schedule, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
    }
  StatusPill(
    label = status.label,
    icon = icon,
    modifier = modifier,
    container = container,
    contentColor = content,
  )
}