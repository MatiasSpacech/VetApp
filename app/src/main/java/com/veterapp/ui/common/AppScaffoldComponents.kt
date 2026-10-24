package com.veterapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.veterapp.theme.extendedColors

@Composable
fun BrandMark(size: Dp = 38.dp, modifier: Modifier = Modifier) {
  val accent = MaterialTheme.colorScheme.secondary
  Box(
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            listOf(MaterialTheme.colorScheme.primary, accent),
          ),
        ),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painter = painterResource(AppIcons.Pets),
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(size * 0.54f),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  brand: Boolean = false,
  navigationIcon: (@Composable () -> Unit)? = null,
  actions: @Composable RowScope.() -> Unit = {},
) {
  TopAppBar(
    modifier = modifier,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (brand) {
          BrandMark()
          Spacer(Modifier.width(10.dp))
        }
        Column {
          Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          if (subtitle != null) {
            Text(
              subtitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
    navigationIcon = { navigationIcon?.invoke() },
    actions = actions,
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
      ),
  )
}

@Composable
fun AppBackTopBar(
  title: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  actions: @Composable RowScope.() -> Unit = {},
) {
  AppTopBar(
    title = title,
    subtitle = subtitle,
    modifier = modifier,
    navigationIcon = {
      Surface(
        onClick = onBack,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
      ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
          Icon(
            painter = painterResource(AppIcons.ArrowBack),
            contentDescription = "Volver",
            modifier = Modifier.size(20.dp),
          )
        }
      }
    },
    actions = actions,
  )
}

@Composable
fun SyncStatusChip(
  label: String,
  icon: Int,
  container: Color,
  contentColor: Color,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier.then(
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
      ),
    shape = RoundedCornerShape(50),
    color = container,
    contentColor = contentColor,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(15.dp),
      )
      Text(label, style = MaterialTheme.typography.labelMedium)
    }
  }
}

@Composable
fun SectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.weight(1f),
    )
    if (actionLabel != null && onAction != null) {
      TextButton(onClick = onAction) {
        Text(actionLabel, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(2.dp))
        Icon(
          painter = painterResource(AppIcons.ChevronRight),
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

@Composable
fun GradientHero(
  modifier: Modifier = Modifier,
  colors: List<Color> = MaterialTheme.extendedColors.heroGradient,
  content: @Composable ColumnScope.() -> Unit,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(28.dp))
        .background(Brush.linearGradient(colors))
        .drawBehind {
          drawCircle(
            color = Color.White.copy(alpha = 0.09f),
            radius = size.width * 0.42f,
            center = Offset(size.width * 0.93f, size.height * 0.05f),
          )
          drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = size.width * 0.26f,
            center = Offset(size.width * 0.12f, size.height * 0.95f),
          )
        }
        .padding(horizontal = 20.dp, vertical = 18.dp),
  ) {
    Column(content = content)
  }
}

@Composable
fun AppSoftSpacer(modifier: Modifier = Modifier) {
  Spacer(modifier.height(8.dp))
}
