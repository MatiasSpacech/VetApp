package com.veterapp.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veterapp.domain.PetStatus

@Composable
fun PetStatusChip(status: PetStatus, modifier: Modifier = Modifier) {
  val container =
    when (status) {
      PetStatus.Active -> MaterialTheme.colorScheme.primaryContainer
      PetStatus.Inactive -> MaterialTheme.colorScheme.tertiaryContainer
      PetStatus.Deceased -> MaterialTheme.colorScheme.errorContainer
    }
  val content =
    when (status) {
      PetStatus.Active -> MaterialTheme.colorScheme.onPrimaryContainer
      PetStatus.Inactive -> MaterialTheme.colorScheme.onTertiaryContainer
      PetStatus.Deceased -> MaterialTheme.colorScheme.onErrorContainer
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
