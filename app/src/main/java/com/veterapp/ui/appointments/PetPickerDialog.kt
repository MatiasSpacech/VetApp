package com.veterapp.ui.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Pet

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
    title = { Text("Elegir mascota") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = search,
          onValueChange = { search = it },
          label = { Text("Buscar mascota o dueño") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
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
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(filtered, key = { it.id }) { pet ->
              Card(
                onClick = { onSelect(pet) },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  RadioButton(
                    selected = pet.id == selectedPetId,
                    onClick = { onSelect(pet) },
                  )
                  Column(Modifier.padding(start = 8.dp)) {
                    Text(pet.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                      "${pet.ownerName} · ${pet.subtitle}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
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
