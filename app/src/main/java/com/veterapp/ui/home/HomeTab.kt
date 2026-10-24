package com.veterapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Client
import com.veterapp.domain.Pet
import com.veterapp.domain.PetStatus
import com.veterapp.ui.common.formatDateTime
import com.veterapp.ui.main.MainUiState

@Composable
fun HomeTab(
  state: MainUiState,
  clients: List<Client>,
  pets: List<Pet>,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      "Resumen",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      StatCard(
        label = "Clientes",
        value = clients.size,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f),
      )
      StatCard(
        label = "Mascotas activas",
        value = pets.count { it.status == PetStatus.Active },
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f),
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      StatCard(
        label = "Inactivas",
        value = pets.count { it.status == PetStatus.Inactive },
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.weight(1f),
      )
      StatCard(
        label = "Fallecidas",
        value = pets.count { it.status == PetStatus.Deceased },
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.weight(1f),
      )
    }
    Spacer(Modifier.height(4.dp))
    SyncSummary(state)
    Spacer(Modifier.height(24.dp))
  }
}

@Composable
private fun StatCard(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(
        value.toString(),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = color,
      )
      Text(
        label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SyncSummary(state: MainUiState) {
  val status =
    when {
      !state.configured -> "Modo local"
      !state.online -> "Sin conexión"
      state.syncing -> "Sincronizando…"
      state.pendingCount > 0 -> "${state.pendingCount} cambio(s) pendiente(s)"
      else -> "Sincronizado"
    }
  val color =
    when {
      !state.configured -> MaterialTheme.colorScheme.onSurfaceVariant
      !state.online -> MaterialTheme.colorScheme.error
      state.pendingCount > 0 -> MaterialTheme.colorScheme.tertiary
      else -> MaterialTheme.colorScheme.primary
    }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(status, color = color, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(4.dp))
      Text(
        "Último sync: ${formatDateTime(state.lastSyncAt)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      val backupPending =
        state.lastBackupAt <= 0L ||
          System.currentTimeMillis() - state.lastBackupAt > 7L * 24L * 60L * 60L * 1000L
      if (backupPending) {
        Spacer(Modifier.height(4.dp))
        Text(
          "Backup semanal pendiente: abrí Ajustes",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
      if (state.syncError != null) {
        Spacer(Modifier.height(6.dp))
        Text(
          state.syncError,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}
