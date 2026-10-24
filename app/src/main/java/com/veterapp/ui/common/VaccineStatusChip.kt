package com.veterapp.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veterapp.domain.VaccineStatus

@Composable
fun VaccineStatusChip(status: VaccineStatus, modifier: Modifier = Modifier) {
  val container =
    when (status) {
      VaccineStatus.Overdue -> MaterialTheme.colorScheme.errorContainer
      VaccineStatus.DueSoon -> MaterialTheme.colorScheme.tertiaryContainer
      VaccineStatus.Upcoming -> MaterialTheme.colorScheme.primaryContainer
      VaccineStatus.Scheduled -> MaterialTheme.colorScheme.surfaceVariant
    }
  val content =
    when (status) {
      VaccineStatus.Overdue -> MaterialTheme.colorScheme.onErrorContainer
      VaccineStatus.DueSoon -> MaterialTheme.colorScheme.onTertiaryContainer
      VaccineStatus.Upcoming -> MaterialTheme.colorScheme.onPrimaryContainer
      VaccineStatus.Scheduled -> MaterialTheme.colorScheme.onSurfaceVariant
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
