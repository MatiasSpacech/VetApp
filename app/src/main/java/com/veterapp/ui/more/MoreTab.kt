package com.veterapp.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veterapp.theme.AppSpacing
import com.veterapp.ui.common.AppCard
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.GradientHero
import com.veterapp.ui.common.IconBadge

@Composable
fun MoreTab(
  onOpenPets: () -> Unit,
  onOpenInventory: () -> Unit,
  onOpenReports: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = AppSpacing.lg),
    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
  ) {
    Spacer(Modifier.height(AppSpacing.xs))
    GradientHero {
      Text(
        "Más herramientas",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        "Todo lo que necesitás para gestionar tu veterinaria.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.85f),
      )
    }
    Spacer(Modifier.height(AppSpacing.xxs))
    MoreRow(
      title = "Mascotas",
      subtitle = "Fichas, historia, vacunas, turnos y peso.",
      icon = AppIcons.Pets,
      accent = MaterialTheme.colorScheme.primary,
      onClick = onOpenPets,
    )
    MoreRow(
      title = "Inventario",
      subtitle = "Productos, servicios, stock, áreas y categorías.",
      icon = AppIcons.Inventory,
      accent = MaterialTheme.colorScheme.secondary,
      onClick = onOpenInventory,
    )
    MoreRow(
      title = "Reportes",
      subtitle = "Facturación, resultado estimado, caja e inventario.",
      icon = AppIcons.Reports,
      accent = MaterialTheme.colorScheme.tertiary,
      onClick = onOpenReports,
    )
    MoreRow(
      title = "Ajustes",
      subtitle = "Seguridad, agenda, recordatorios, catálogo y backup.",
      icon = AppIcons.Settings,
      accent = MaterialTheme.colorScheme.primary,
      onClick = onOpenSettings,
    )
    Spacer(Modifier.height(96.dp))
  }
}

@Composable
private fun MoreRow(
  title: String,
  subtitle: String,
  icon: Int,
  accent: Color,
  onClick: () -> Unit,
) {
  AppCard(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconBadge(icon = icon, tint = accent, size = 46.dp)
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Icon(
        painter = painterResource(AppIcons.ChevronRight),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}