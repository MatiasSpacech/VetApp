package com.veterapp.ui.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.veterapp.domain.Pet
import com.veterapp.theme.extendedColors
import com.veterapp.ui.common.AppCard
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.PetStatusChip
import com.veterapp.ui.common.StatusPill

@Composable
fun PetCard(pet: Pet, onClick: () -> Unit, modifier: Modifier = Modifier) {
  AppCard(modifier = modifier, onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      PetPhoto(pet = pet, size = 68.dp)
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            pet.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
          )
          PetStatusChip(pet.status)
        }
        Text(
          pet.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          "Dueño: ${pet.ownerName}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (pet.pendingSync) {
          StatusPill(
            label = "Pendiente",
            icon = AppIcons.Sync,
            container = MaterialTheme.extendedColors.warningContainer,
            contentColor = MaterialTheme.extendedColors.onWarningContainer,
          )
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

@Composable
fun PetPhoto(pet: Pet, size: Dp, modifier: Modifier = Modifier) {
  val shape = RoundedCornerShape(18.dp)
  val box =
    modifier
      .size(size)
      .clip(shape)
      .background(
        Brush.linearGradient(
          listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
          ),
        ),
      )
  if (pet.fotoUrl != null) {
    AsyncImage(
      model = pet.fotoUrl,
      contentDescription = "Foto de ${pet.name}",
      modifier = box,
      contentScale = ContentScale.Crop,
    )
  } else {
    Box(modifier = box, contentAlignment = Alignment.Center) {
      Icon(
        painter = painterResource(AppIcons.Pets),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(size * 0.46f),
      )
    }
  }
}