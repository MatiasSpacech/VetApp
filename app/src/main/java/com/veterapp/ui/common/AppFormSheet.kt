package com.veterapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veterapp.theme.AppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFormSheet(
  title: String,
  confirmLabel: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  confirmEnabled: Boolean = true,
  busy: Boolean = false,
  dirty: Boolean = false,
  content: @Composable ColumnScope.() -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var showDiscard by remember { mutableStateOf(false) }

  fun requestDismiss() {
    if (dirty) showDiscard = true else onDismiss()
  }

  ModalBottomSheet(
    onDismissRequest = ::requestDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = AppDimens.sheetRadius, topEnd = AppDimens.sheetRadius),
    containerColor = MaterialTheme.colorScheme.surface,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    modifier = modifier,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.94f)
          .imePadding()
          .navigationBarsPadding(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(Modifier.weight(1f)) {
          Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
          )
          if (subtitle != null) {
            Text(
              subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        IconButton(onClick = ::requestDismiss, enabled = !busy) {
          Icon(
            painter = painterResource(AppIcons.Close),
            contentDescription = "Cerrar",
            modifier = Modifier.size(22.dp),
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Column(
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = ::requestDismiss,
          enabled = !busy,
          modifier = Modifier.weight(1f),
        ) {
          Text("Cancelar")
        }
        Button(
          onClick = onConfirm,
          enabled = confirmEnabled && !busy,
          modifier = Modifier.weight(1f),
        ) {
          if (busy) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.size(8.dp))
          }
          Text(if (busy) "Guardando…" else confirmLabel)
        }
      }
      Spacer(Modifier.height(4.dp))
    }
  }

  if (showDiscard) {
    AlertDialog(
      onDismissRequest = { showDiscard = false },
      title = { Text("Descartar cambios") },
      text = { Text("Hay datos sin guardar. ¿Querés salir igual?") },
      confirmButton = {
        TextButton(
          onClick = {
            showDiscard = false
            onDismiss()
          },
        ) {
          Text("Salir sin guardar", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDiscard = false }) { Text("Seguir editando") }
      },
    )
  }
}
