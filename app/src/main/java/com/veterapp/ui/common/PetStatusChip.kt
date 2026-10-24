package com.veterapp.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.veterapp.domain.PetStatus

@Composable
fun PetStatusChip(status: PetStatus, modifier: Modifier = Modifier) {
  val (icon, container, content) =
    when (status) {
      PetStatus.Active ->
        Triple(AppIcons.CheckCircle, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
      PetStatus.Inactive ->
        Triple(AppIcons.Schedule, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
      PetStatus.Deceased ->
        Triple(AppIcons.Info, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }
  StatusPill(
    label = status.label,
    icon = icon,
    modifier = modifier,
    container = container,
    contentColor = content,
  )
}