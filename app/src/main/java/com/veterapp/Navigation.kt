package com.veterapp

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.veterapp.ui.clients.ClientDetailScreen
import com.veterapp.ui.clients.ClientsViewModel
import com.veterapp.ui.clinical.ClinicalViewModel
import com.veterapp.ui.main.MainScreen
import com.veterapp.ui.main.MainViewModel
import com.veterapp.ui.pets.PetDetailScreen
import com.veterapp.ui.pets.PetsViewModel
import com.veterapp.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
  val context = LocalContext.current
  val app = context.applicationContext as VeterAppApplication
  val mainViewModel: MainViewModel = viewModel { MainViewModel(app.container) }
  val clientsViewModel: ClientsViewModel = viewModel { ClientsViewModel(app.container) }
  val petsViewModel: PetsViewModel = viewModel { PetsViewModel(app.container) }
  val clinicalViewModel: ClinicalViewModel = viewModel { ClinicalViewModel(app.container) }
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            mainViewModel = mainViewModel,
            clientsViewModel = clientsViewModel,
            petsViewModel = petsViewModel,
            clinicalViewModel = clinicalViewModel,
            onOpenSettings = { backStack.add(Settings) },
            onOpenClient = { backStack.add(ClientDetail(it)) },
            onOpenPet = { backStack.add(PetDetail(it)) },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<Settings> {
          SettingsScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<ClientDetail> { key ->
          ClientDetailScreen(
            clientId = key.clientId,
            clientsViewModel = clientsViewModel,
            petsViewModel = petsViewModel,
            onBack = { backStack.removeLastOrNull() },
            onOpenPet = { backStack.add(PetDetail(it)) },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<PetDetail> { key ->
          PetDetailScreen(
            petId = key.petId,
            clientsViewModel = clientsViewModel,
            petsViewModel = petsViewModel,
            clinicalViewModel = clinicalViewModel,
            onBack = { backStack.removeLastOrNull() },
            onOpenClient = { backStack.add(ClientDetail(it)) },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
      },
  )
}
