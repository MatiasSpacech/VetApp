package com.veterapp

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.veterapp.ui.appointments.AppointmentsViewModel
import com.veterapp.ui.clients.ClientDetailScreen
import com.veterapp.ui.clients.ClientsViewModel
import com.veterapp.ui.clinical.ClinicalViewModel
import com.veterapp.ui.inventory.InventoryScreen
import com.veterapp.ui.main.MainScreen
import com.veterapp.ui.main.MainViewModel
import com.veterapp.ui.pets.PetDetailScreen
import com.veterapp.ui.pets.PetsListScreen
import com.veterapp.ui.pets.PetsViewModel
import com.veterapp.ui.reports.ReportsScreen
import com.veterapp.ui.reports.ReportsViewModel
import com.veterapp.ui.sales.SalesViewModel
import com.veterapp.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
  val context = LocalContext.current
  val app = context.applicationContext as VeterAppApplication
  val mainViewModel: MainViewModel = viewModel { MainViewModel(app.container) }
  val clientsViewModel: ClientsViewModel = viewModel { ClientsViewModel(app.container) }
  val petsViewModel: PetsViewModel = viewModel { PetsViewModel(app.container) }
  val clinicalViewModel: ClinicalViewModel = viewModel { ClinicalViewModel(app.container) }
  val appointmentsViewModel: AppointmentsViewModel = viewModel { AppointmentsViewModel(app.container) }
  val salesViewModel: SalesViewModel = viewModel { SalesViewModel(app.container) }
  val reportsViewModel: ReportsViewModel = viewModel { ReportsViewModel(app.container) }
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
            appointmentsViewModel = appointmentsViewModel,
            salesViewModel = salesViewModel,
            reportsViewModel = reportsViewModel,
            onOpenSettings = { backStack.add(Settings) },
            onOpenClient = { backStack.add(ClientDetail(it)) },
            onOpenPet = { backStack.add(PetDetail(it)) },
            onOpenPetsList = { backStack.add(PetsList) },
            onOpenInventory = { backStack.add(Inventory) },
            onOpenReports = { backStack.add(Reports) },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<Settings> {
          SettingsScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<PetsList> {
          PetsListScreen(
            petsViewModel = petsViewModel,
            clientsViewModel = clientsViewModel,
            onBack = { backStack.removeLastOrNull() },
            onOpenPet = { backStack.add(PetDetail(it)) },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<Inventory> {
          InventoryScreen(
            onBack = { backStack.removeLastOrNull() },
            onOpenSettings = { backStack.add(Settings) },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<Reports> {
          ReportsScreen(
            onBack = { backStack.removeLastOrNull() },
            onOpenPet = { backStack.add(PetDetail(it)) },
            onOpenInventory = { backStack.add(Inventory) },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<ClientDetail> { key ->
          ClientDetailScreen(
            clientId = key.clientId,
            clientsViewModel = clientsViewModel,
            petsViewModel = petsViewModel,
            appointmentsViewModel = appointmentsViewModel,
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
            appointmentsViewModel = appointmentsViewModel,
            onBack = { backStack.removeLastOrNull() },
            onOpenClient = { backStack.add(ClientDetail(it)) },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
      },
  )
}
