package com.veterapp.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.veterapp.theme.extendedColors

@Composable
fun StatusPill(
  label: String,
  @DrawableRes icon: Int,
  modifier: Modifier = Modifier,
  container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = container,
    contentColor = contentColor,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(14.dp),
      )
      Text(label, style = MaterialTheme.typography.labelMedium)
    }
  }
}

@Composable
fun TrendBadge(
  label: String,
  positive: Boolean,
  modifier: Modifier = Modifier,
) {
  val color = if (positive) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.error
  val container =
    if (positive) MaterialTheme.extendedColors.successContainer
    else MaterialTheme.colorScheme.errorContainer
  Row(
    modifier =
      modifier
        .padding(top = 0.dp)
        .then(Modifier),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Surface(shape = RoundedCornerShape(50), color = container) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          painter = painterResource(if (positive) AppIcons.TrendingUp else AppIcons.TrendingDown),
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(14.dp),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
      }
    }
  }
}

@Composable
fun EmptyState(
  title: String,
  description: String,
  @DrawableRes icon: Int = AppIcons.Pets,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 28.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.size(88.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          painter = painterResource(icon),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(40.dp),
        )
      }
    }
    Spacer(Modifier.height(18.dp))
    Text(
      title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      Spacer(Modifier.height(18.dp))
      Button(onClick = onAction) {
        Icon(
          painter = painterResource(AppIcons.Add),
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(actionLabel)
      }
    }
  }
}

@Composable
fun SkeletonBlock(
  modifier: Modifier = Modifier,
  height: Dp = 16.dp,
  shape: Shape = RoundedCornerShape(8.dp),
) {
  val transition = rememberInfiniteTransition(label = "skeleton")
  val alpha by transition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.75f,
    animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
    label = "skeleton-alpha",
  )
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(height)
        .alpha(alpha),
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      shape = shape,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {}
  }
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
  AppCard(modifier = modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      SkeletonBlock(Modifier.size(42.dp), height = 42.dp, shape = RoundedCornerShape(14.dp))
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SkeletonBlock(height = 14.dp, modifier = Modifier.fillMaxWidth(0.65f))
        SkeletonBlock(height = 10.dp, modifier = Modifier.fillMaxWidth(0.9f))
      }
    }
  }
}
