package com.veterapp.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veterapp.domain.AppointmentStatus

@Composable
fun AppointmentStatusChip(status: AppointmentStatus, modifier: Modifier = Modifier) {
  val container =
    when (status) {
      AppointmentStatus.Scheduled -> MaterialTheme.colorScheme.surfaceVariant
      AppointmentStatus.Confirmed -> MaterialTheme.colorScheme.primaryContainer
      AppointmentStatus.Attended -> MaterialTheme.colorScheme.secondaryContainer
      AppointmentStatus.Cancelled -> MaterialTheme.colorScheme.errorContainer
      AppointmentStatus.NoShow -> MaterialTheme.colorScheme.tertiaryContainer
    }
  val content =
    when (status) {
      AppointmentStatus.Scheduled -> MaterialTheme.colorScheme.onSurfaceVariant
      AppointmentStatus.Confirmed -> MaterialTheme.colorScheme.onPrimaryContainer
      AppointmentStatus.Attended -> MaterialTheme.colorScheme.onSecondaryContainer
      AppointmentStatus.Cancelled -> MaterialTheme.colorScheme.onErrorContainer
      AppointmentStatus.NoShow -> MaterialTheme.colorScheme.onTertiaryContainer
    }
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = container,
    contentColor = content,
  ) {
    Text(
      status.label,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      style = MaterialTheme.typography.labelMedium,
    )
  }
}
