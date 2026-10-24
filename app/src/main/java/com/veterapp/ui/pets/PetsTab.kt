package com.veterapp.ui.pets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Pet
import com.veterapp.domain.PetStatus

private enum class PetFilter(val label: String) {
  All("Todas"),
  Active("Activas"),
  Inactive("Inactivas"),
  Deceased("Fallecidas"),
}

@Composable
fun PetsTab(
  pets: List<Pet>,
  hasClients: Boolean,
  onOpenPet: (String) -> Unit,
  onNewPet: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var search by rememberSaveable { mutableStateOf("") }
  var filterStorage by rememberSaveable { mutableStateOf(PetFilter.All.name) }
  val filter = runCatching { PetFilter.valueOf(filterStorage) }.getOrDefault(PetFilter.All)

  val visible =
    pets.filter { pet ->
      val matchesSearch =
        search.isBlank() ||
          pet.name.contains(search, ignoreCase = true) ||
          pet.ownerName.contains(search, ignoreCase = true)
      val matchesFilter =
        when (filter) {
          PetFilter.All -> true
          PetFilter.Active -> pet.status == PetStatus.Active
          PetFilter.Inactive -> pet.status == PetStatus.Inactive
          PetFilter.Deceased -> pet.status == PetStatus.Deceased
        }
      matchesSearch && matchesFilter
    }

  Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
      value = search,
      onValueChange = { search = it },
      label = { Text("Buscar por mascota o dueño") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PetFilter.entries.forEach { option ->
        FilterChip(
          selected = filter == option,
          onClick = { filterStorage = option.name },
          label = { Text(option.label) },
        )
      }
    }
    Spacer(Modifier.height(12.dp))

    when {
      pets.isEmpty() -> EmptyPets(hasClients, onNewPet)
      visible.isEmpty() -> NoPetResults(search)
      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(visible, key = { it.id }) { pet ->
            PetCard(pet = pet, onClick = { onOpenPet(pet.id) })
          }
          item { Spacer(Modifier.height(88.dp)) }
        }
      }
    }
  }
}

@Composable
private fun EmptyPets(hasClients: Boolean, onNewPet: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Todavía no hay mascotas", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
    Text(
      if (hasClients) {
        "Agregá la primera con el botón de abajo."
      } else {
        "Creá un cliente y después su mascota."
      },
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onNewPet) { Text("Nueva mascota") }
  }
}

@Composable
private fun NoPetResults(search: String) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Sin resultados para \"$search\"", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
    Text(
      "Probá con otro nombre, dueño o estado.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
