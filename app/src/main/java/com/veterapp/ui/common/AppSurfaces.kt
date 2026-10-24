package com.veterapp.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.veterapp.theme.AppDimens

@Composable
fun AppCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  contentPadding: androidx.compose.foundation.layout.PaddingValues =
    androidx.compose.foundation.layout.PaddingValues(16.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = RoundedCornerShape(AppDimens.cardRadius)
  val color = MaterialTheme.colorScheme.surfaceContainerLow
  val border = BorderStroke(AppDimens.cardBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
  if (onClick != null) {
    Surface(
      onClick = onClick,
      modifier = modifier.fillMaxWidth(),
      shape = shape,
      color = color,
      border = border,
      shadowElevation = 1.dp,
    ) {
      Column(Modifier.padding(contentPadding), content = content)
    }
  } else {
    Surface(
      modifier = modifier.fillMaxWidth(),
      shape = shape,
      color = color,
      border = border,
      shadowElevation = 1.dp,
    ) {
      Column(Modifier.padding(contentPadding), content = content)
    }
  }
}

@Composable
fun IconBadge(
  @DrawableRes icon: Int,
  tint: Color = MaterialTheme.colorScheme.primary,
  container: Color = tint.copy(alpha = 0.12f),
  size: Dp = 42.dp,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.size(size),
    shape = RoundedCornerShape(14.dp),
    color = container,
  ) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size * 0.52f),
      )
    }
  }
}

@Composable
fun StatTile(
  label: String,
  value: String,
  @DrawableRes icon: Int,
  modifier: Modifier = Modifier,
  accent: Color = MaterialTheme.colorScheme.primary,
  trendLabel: String? = null,
  trendPositive: Boolean = true,
) {
  AppCard(modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconBadge(icon = icon, tint = accent)
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Text(
          value,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          label,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
        )
      }
    }
    if (trendLabel != null) {
      Spacer(Modifier.height(10.dp))
      TrendBadge(
        label = trendLabel,
        positive = trendPositive,
      )
    }
  }
}

@Composable
fun QuickActionTile(
  label: String,
  @DrawableRes icon: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  accent: Color = MaterialTheme.colorScheme.primary,
) {
  Surface(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(AppDimens.cardBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      IconBadge(icon = icon, tint = accent, size = 44.dp)
      Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ListItemCard(
  title: String,
  subtitle: String? = null,
  @DrawableRes icon: Int? = null,
  modifier: Modifier = Modifier,
  trailing: (@Composable () -> Unit)? = null,
  onClick: (() -> Unit)? = null,
) {
  AppCard(
    modifier = modifier,
    onClick = onClick,
    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (icon != null) {
        IconBadge(icon = icon, size = 40.dp)
        Spacer(Modifier.width(12.dp))
      }
      Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (subtitle != null) {
          Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      if (trailing != null) {
        trailing()
      } else if (onClick != null) {
        Icon(
          painter = painterResource(AppIcons.ChevronRight),
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
