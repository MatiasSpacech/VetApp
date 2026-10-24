package com.veterapp.ui.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Client
import com.veterapp.domain.Pet
import com.veterapp.theme.AppSpacing
import com.veterapp.theme.extendedColors
import com.veterapp.ui.common.AppCard
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.EmptyState
import com.veterapp.ui.common.SearchField
import com.veterapp.ui.common.StatusPill

@Composable
fun ClientsTab(
  clients: List<Client>,
  pets: List<Pet>,
  onOpenClient: (String) -> Unit,
  onNewClient: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var search by rememberSaveable { mutableStateOf("") }
  val petCounts = pets.groupingBy { it.clientId }.eachCount()
  val visible =
    if (search.isBlank()) {
      clients
    } else {
      clients.filter {
        it.name.contains(search, ignoreCase = true) ||
          it.phone.orEmpty().contains(search, ignoreCase = true) ||
          it.document.orEmpty().contains(search, ignoreCase = true) ||
          it.email.orEmpty().contains(search, ignoreCase = true)
      }
    }

  Column(modifier = modifier.fillMaxSize().padding(horizontal = AppSpacing.lg)) {
    Spacer(Modifier.size(AppSpacing.xs))
    SearchField(
      value = search,
      onValueChange = { search = it },
      placeholder = "Buscar por nombre, teléfono o documento",
    )
    Spacer(Modifier.size(AppSpacing.sm))
    Text(
      "${visible.size} cliente(s)",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.size(AppSpacing.xs))
    when {
      clients.isEmpty() ->
        EmptyState(
          title = "Todavía no hay clientes",
          description = "Creá el primer cliente para vincular sus mascotas y su historia clínica.",
          icon = AppIcons.Clients,
          actionLabel = "Crear cliente",
          onAction = onNewClient,
        )
      visible.isEmpty() ->
        EmptyState(
          title = "Sin resultados",
          description = "No encontramos clientes para \"$search\".",
          icon = AppIcons.Search,
          actionLabel = "Limpiar búsqueda",
          onAction = { search = "" },
        )
      else ->
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
              top = 2.dp,
              bottom = 96.dp,
            ),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(visible, key = { it.id }) { client ->
            ClientCard(
              client = client,
              petCount = petCounts[client.id] ?: 0,
              onClick = { onOpenClient(client.id) },
            )
          }
        }
    }
  }
}

@Composable
private fun ClientCard(client: Client, petCount: Int, onClick: () -> Unit) {
  AppCard(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer),
              ),
            ),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          client.initials,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontWeight = FontWeight.Bold,
        )
      }
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          client.name,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          client.contactLine ?: "Sin datos de contacto",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          StatusPill(
            label = when (petCount) {
              0 -> "Sin mascotas"
              1 -> "1 mascota"
              else -> "$petCount mascotas"
            },
            icon = AppIcons.Pets,
            container = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          )
          if (client.pendingSync) {
            StatusPill(
              label = "Pendiente",
              icon = AppIcons.Sync,
              container = MaterialTheme.extendedColors.warningContainer,
              contentColor = MaterialTheme.extendedColors.onWarningContainer,
            )
          }
        }
      }
      Icon(
        painter = painterResource(AppIcons.ChevronRight),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}
