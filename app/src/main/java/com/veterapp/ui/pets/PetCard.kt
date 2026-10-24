package com.veterapp.ui.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.veterapp.domain.Pet
import com.veterapp.ui.common.PetStatusChip

@Composable
fun PetCard(pet: Pet, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PetPhoto(pet = pet, size = 64.dp)
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            pet.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
          )
          PetStatusChip(pet.status)
        }
        Text(pet.subtitle, style = MaterialTheme.typography.bodySmall)
        Text(
          "Dueño: ${pet.ownerName}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (pet.pendingSync) {
          Spacer(Modifier.height(4.dp))
          Text(
            "Pendiente de sincronizar",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
          )
        }
      }
    }
  }
}

@Composable
fun PetPhoto(pet: Pet, size: Dp, modifier: Modifier = Modifier) {
  val shape = RoundedCornerShape(12.dp)
  val box =
    modifier
      .size(size)
      .clip(shape)
      .background(MaterialTheme.colorScheme.secondaryContainer)
  if (pet.fotoUrl != null) {
    AsyncImage(
      model = pet.fotoUrl,
      contentDescription = "Foto de ${pet.name}",
      modifier = box,
      contentScale = ContentScale.Crop,
    )
  } else {
    Box(modifier = box, contentAlignment = Alignment.Center) {
      Text(
        pet.name.take(1).uppercase(),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
    }
  }
}
