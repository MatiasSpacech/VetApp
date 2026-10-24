package com.veterapp.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

data class FilterOption(
  val id: String,
  val label: String,
)

@Composable
fun SearchField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  onSearch: (() -> Unit)? = null,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
    singleLine = true,
    shape = RoundedCornerShape(18.dp),
    leadingIcon = {
      Icon(
        painter = painterResource(AppIcons.Search),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
    },
    trailingIcon = {
      if (value.isNotEmpty()) {
        IconButton(onClick = { onValueChange("") }) {
          Icon(
            painter = painterResource(AppIcons.Close),
            contentDescription = "Limpiar búsqueda",
            modifier = Modifier.size(18.dp),
          )
        }
      }
    },
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
    colors =
      OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
    modifier = modifier.fillMaxWidth(),
  )
}

@Composable
fun FilterPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: Int? = null,
) {
  Surface(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color =
      if (selected) MaterialTheme.colorScheme.primary
      else MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor =
      if (selected) MaterialTheme.colorScheme.onPrimary
      else MaterialTheme.colorScheme.onSurfaceVariant,
    border =
      if (selected) null
      else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (icon != null) {
        Icon(
          painter = painterResource(icon),
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
      Text(label, style = MaterialTheme.typography.labelLarge)
    }
  }
}

@Composable
fun FilterPillRow(
  options: List<FilterOption>,
  selectedId: String?,
  onSelect: (String?) -> Unit,
  modifier: Modifier = Modifier,
  allLabel: String? = "Todos",
) {
  Row(
    modifier = modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (allLabel != null) {
      FilterPill(
        label = allLabel,
        selected = selectedId == null,
        onClick = { onSelect(null) },
      )
    }
    options.forEach { option ->
      FilterPill(
        label = option.label,
        selected = selectedId == option.id,
        onClick = { onSelect(option.id) },
      )
    }
    Spacer(Modifier.width(2.dp))
  }
}
