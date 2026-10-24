package com.veterapp.ui.pets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veterapp.domain.Pet
import com.veterapp.domain.PetStatus
import com.veterapp.theme.AppSpacing
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.EmptyState
import com.veterapp.ui.common.FilterOption
import com.veterapp.ui.common.FilterPillRow
import com.veterapp.ui.common.SearchField

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

  Column(modifier = modifier.fillMaxSize().padding(horizontal = AppSpacing.lg)) {
    Spacer(Modifier.size(AppSpacing.xs))
    SearchField(
      value = search,
      onValueChange = { search = it },
      placeholder = "Buscar por mascota o dueño",
    )
    Spacer(Modifier.size(AppSpacing.xs))
    FilterPillRow(
      options =
        listOf(
          FilterOption(PetFilter.Active.name, PetFilter.Active.label),
          FilterOption(PetFilter.Inactive.name, PetFilter.Inactive.label),
          FilterOption(PetFilter.Deceased.name, PetFilter.Deceased.label),
        ),
      selectedId = filter.takeIf { it != PetFilter.All }?.name,
      onSelect = { id -> filterStorage = id ?: PetFilter.All.name },
      allLabel = "Todas",
    )
    Spacer(Modifier.size(AppSpacing.xs))
    Text(
      "${visible.size} mascota(s)",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.size(AppSpacing.xs))

    when {
      pets.isEmpty() ->
        EmptyState(
          title = "Todavía no hay mascotas",
          description =
            if (hasClients) {
              "Agregá la primera mascota y empezá su historia clínica."
            } else {
              "Creá un cliente y después su mascota."
            },
          icon = AppIcons.Pets,
          actionLabel = "Nueva mascota",
          onAction = onNewPet,
        )
      visible.isEmpty() ->
        EmptyState(
          title = "Sin resultados",
          description = "No encontramos mascotas para \"$search\".",
          icon = AppIcons.Search,
          actionLabel = "Limpiar búsqueda",
          onAction = { search = "" },
        )
      else ->
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding =
            androidx.compose.foundation.layout.PaddingValues(top = 2.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(visible, key = { it.id }) { pet ->
            PetCard(pet = pet, onClick = { onOpenPet(pet.id) })
          }
        }
    }
  }
}