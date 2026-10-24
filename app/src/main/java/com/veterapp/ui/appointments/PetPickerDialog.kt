package com.veterapp.ui.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Pet
import com.veterapp.ui.common.AppCard
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.SearchField
import com.veterapp.ui.pets.PetPhoto

@Composable
fun PetPickerDialog(
  pets: List<Pet>,
  selectedPetId: String?,
  onSelect: (Pet) -> Unit,
  onDismiss: () -> Unit,
) {
  var search by rememberSaveable { mutableStateOf("") }
  val filtered =
    if (search.isBlank()) {
      pets
    } else {
      pets.filter {
        it.name.contains(search, ignoreCase = true) ||
          it.ownerName.contains(search, ignoreCase = true)
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          painter = painterResource(AppIcons.Pets),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text("Elegir mascota")
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SearchField(
          value = search,
          onValueChange = { search = it },
          placeholder = "Buscar mascota o dueño",
        )
        if (pets.isEmpty()) {
          Text(
            "No hay mascotas activas. Creá una desde la pestaña Mascotas.",
            style = MaterialTheme.typography.bodyMedium,
          )
        } else if (filtered.isEmpty()) {
          Text(
            "Sin resultados para \"$search\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          LazyColumn(
            modifier = Modifier.heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(filtered, key = { it.id }) { pet ->
              AppCard(
                onClick = { onSelect(pet) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  PetPhoto(pet = pet, size = 48.dp)
                  Spacer(Modifier.width(12.dp))
                  Column(Modifier.weight(1f)) {
                    Text(pet.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                      "${pet.ownerName} · ${pet.subtitle}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  if (pet.id == selectedPetId) {
                    Icon(
                      painter = painterResource(AppIcons.CheckCircle),
                      contentDescription = "Seleccionada",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(22.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar") }
    },
  )
}