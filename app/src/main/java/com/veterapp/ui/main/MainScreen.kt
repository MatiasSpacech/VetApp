package com.veterapp.ui.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.ui.res.painterResource
import com.veterapp.theme.extendedColors
import com.veterapp.ui.common.AppIcons
import com.veterapp.ui.common.AppTopBar
import com.veterapp.ui.common.SyncStatusChip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Brush
import com.veterapp.ui.common.BrandMark
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veterapp.domain.ClinicTime
import com.veterapp.ui.appointments.AppointmentFormDialog
import com.veterapp.ui.appointments.AppointmentsTab
import com.veterapp.ui.appointments.AppointmentsViewModel
import com.veterapp.ui.clients.ClientFormDialog
import com.veterapp.ui.clients.ClientsTab
import com.veterapp.ui.clients.ClientsViewModel
import com.veterapp.ui.clinical.ClinicalViewModel
import com.veterapp.ui.home.HomeTab
import com.veterapp.ui.home.QuickAction
import com.veterapp.ui.more.MoreTab
import com.veterapp.ui.pets.PetFormDialog
import com.veterapp.ui.pets.PetsViewModel
import com.veterapp.ui.reports.ReportsViewModel
import com.veterapp.ui.sales.SalesTab
import com.veterapp.ui.sales.SalesViewModel

private enum class MainTab(val label: String) {
  Home("Inicio"),
  Agenda("Agenda"),
  Clients("Clientes"),
  Sales("Ventas"),
  More("Más"),
}

@DrawableRes
private fun MainTab.icon(selected: Boolean): Int =
  when (this) {
    MainTab.Home -> if (selected) AppIcons.HomeFilled else AppIcons.Home
    MainTab.Agenda -> if (selected) AppIcons.AgendaFilled else AppIcons.Agenda
    MainTab.Clients -> if (selected) AppIcons.ClientsFilled else AppIcons.Clients
    MainTab.Sales -> if (selected) AppIcons.SalesFilled else AppIcons.Sales
    MainTab.More -> if (selected) AppIcons.MoreFilled else AppIcons.More
  }
@Composable
fun MainScreen(
  mainViewModel: MainViewModel,
  clientsViewModel: ClientsViewModel,
  petsViewModel: PetsViewModel,
  clinicalViewModel: ClinicalViewModel,
  appointmentsViewModel: AppointmentsViewModel,
  salesViewModel: SalesViewModel,
  reportsViewModel: ReportsViewModel,
  onOpenSettings: () -> Unit,
  onOpenClient: (String) -> Unit,
  onOpenPet: (String) -> Unit,
  onOpenPetsList: () -> Unit,
  onOpenInventory: () -> Unit,
  onOpenReports: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by mainViewModel.uiState.collectAsStateWithLifecycle()

  when (val auth = state.auth) {
    AuthUiState.Loading -> LoadingScreen()
    is AuthUiState.SignedOut -> LoginScreen(auth.message, mainViewModel)
    is AuthUiState.Error -> ErrorScreen(auth.message)
    is AuthUiState.Local,
    is AuthUiState.SignedIn ->
      AppContent(
        state = state,
        mainViewModel = mainViewModel,
        clientsViewModel = clientsViewModel,
        petsViewModel = petsViewModel,
        clinicalViewModel = clinicalViewModel,
        appointmentsViewModel = appointmentsViewModel,
        salesViewModel = salesViewModel,
        reportsViewModel = reportsViewModel,
        onOpenSettings = onOpenSettings,
        onOpenClient = onOpenClient,
        onOpenPet = onOpenPet,
        onOpenPetsList = onOpenPetsList,
        onOpenInventory = onOpenInventory,
        onOpenReports = onOpenReports,
        modifier = modifier,
      )
  }
}

@Composable
private fun LoadingScreen() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}

@Composable
private fun ErrorScreen(message: String) {
  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("No se pudo iniciar", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
    Text(message, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(12.dp))
    Text(
      "Revisá la configuración de Supabase y el SQL de las etapas 0 a 6.",
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun LoginScreen(message: String?, viewModel: MainViewModel) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              MaterialTheme.colorScheme.background,
              MaterialTheme.colorScheme.surfaceContainerHigh,
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            ),
          ),
        )
        .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    BrandMark(size = 76.dp)
    Spacer(Modifier.height(18.dp))
    Text("VeterApp", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
    Text(
      "Gestión veterinaria, simple y cerca.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(28.dp))
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 8.dp,
    ) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Bienvenido", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
          "Ingresá con tu cuenta para continuar.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email") },
          singleLine = true,
          leadingIcon = {
            Icon(
              painter = painterResource(AppIcons.Mail),
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Contraseña") },
          singleLine = true,
          leadingIcon = {
            Icon(
              painter = painterResource(AppIcons.Lock),
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
          },
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth(),
        )
        if (message != null) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                painter = painterResource(AppIcons.Warning),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text(message, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
        Button(
          onClick = { viewModel.signIn(email, password) },
          modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
          Icon(
            painter = painterResource(AppIcons.VerifiedUser),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text("Entrar")
        }
      }
    }
    Spacer(Modifier.height(16.dp))
    Text(
      "El primer inicio de sesión necesita conexión. Después la app funciona offline.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
  }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContent(
  state: MainUiState,
  mainViewModel: MainViewModel,
  clientsViewModel: ClientsViewModel,
  petsViewModel: PetsViewModel,
  clinicalViewModel: ClinicalViewModel,
  appointmentsViewModel: AppointmentsViewModel,
  salesViewModel: SalesViewModel,
  reportsViewModel: ReportsViewModel,
  onOpenSettings: () -> Unit,
  onOpenClient: (String) -> Unit,
  onOpenPet: (String) -> Unit,
  onOpenPetsList: () -> Unit,
  onOpenInventory: () -> Unit,
  onOpenReports: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val clients by clientsViewModel.clients.collectAsStateWithLifecycle()
  val pets by petsViewModel.pets.collectAsStateWithLifecycle()
  val alerts by clinicalViewModel.alerts.collectAsStateWithLifecycle()
  val todayAppointments by appointmentsViewModel.todayAppointments.collectAsStateWithLifecycle()
  val dashboard by reportsViewModel.dashboard.collectAsStateWithLifecycle()
  val todaySales by salesViewModel.todaySales.collectAsStateWithLifecycle()
  var selectedTabName by rememberSaveable { mutableStateOf(MainTab.Home.name) }
  val selectedTab =
    runCatching { MainTab.valueOf(selectedTabName) }.getOrDefault(MainTab.Home)
  var showClientForm by remember { mutableStateOf(false) }
  var showPetForm by remember { mutableStateOf(false) }
  var showAppointmentForm by remember { mutableStateOf(false) }
  val signedIn = state.auth is AuthUiState.SignedIn

  LaunchedEffect(todaySales.size, todaySales.sumOf { it.totalCents }, dashboard?.todayAppointments?.size) {
    reportsViewModel.refreshDashboard()
  }

  val topBarContent: @Composable () -> Unit = {
    val syncLabel =
      when {
        !state.configured -> "Local"
        state.syncing -> "Sync"
        !state.online -> "Offline"
        state.pendingCount > 0 -> "${state.pendingCount}"
        else -> "Al día"
      }
    val syncIcon =
      when {
        !state.configured -> AppIcons.CloudOff
        state.syncing -> AppIcons.Sync
        !state.online -> AppIcons.CloudOff
        state.pendingCount > 0 -> AppIcons.Sync
        else -> AppIcons.CloudDone
      }
    val syncContainer =
      when {
        !state.configured -> MaterialTheme.colorScheme.surfaceContainerHigh
        !state.online -> MaterialTheme.colorScheme.errorContainer
        state.pendingCount > 0 -> MaterialTheme.extendedColors.warningContainer
        else -> MaterialTheme.extendedColors.successContainer
      }
    val syncContent =
      when {
        !state.configured -> MaterialTheme.colorScheme.onSurfaceVariant
        !state.online -> MaterialTheme.colorScheme.onErrorContainer
        state.pendingCount > 0 -> MaterialTheme.extendedColors.onWarningContainer
        else -> MaterialTheme.extendedColors.onSuccessContainer
      }
    AppTopBar(
      title = "VeterApp",
      subtitle = "Gestión veterinaria",
      brand = true,
      actions = {
        SyncStatusChip(
          label = syncLabel,
          icon = syncIcon,
          container = syncContainer,
          contentColor = syncContent,
          onClick = if (state.configured) mainViewModel::syncNow else null,
        )
        IconButton(onClick = onOpenSettings) {
          Icon(
            painter = painterResource(AppIcons.Settings),
            contentDescription = "Ajustes",
            modifier = Modifier.size(22.dp),
          )
        }
        if (signedIn) {
          IconButton(onClick = mainViewModel::signOut) {
            Icon(
              painter = painterResource(AppIcons.Logout),
              contentDescription = "Cerrar sesión",
              modifier = Modifier.size(22.dp),
            )
          }
        }
      },
    )
  }

  val floatingAction: @Composable () -> Unit = {
    when (selectedTab) {
      MainTab.Clients ->
        ExtendedFloatingActionButton(
          onClick = { showClientForm = true },
          icon = {
            Icon(
              painter = painterResource(AppIcons.Add),
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
          },
          text = { Text("Nuevo cliente") },
        )
      MainTab.Agenda ->
        ExtendedFloatingActionButton(
          onClick = { showAppointmentForm = true },
          icon = {
            Icon(
              painter = painterResource(AppIcons.Add),
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
          },
          text = { Text("Nuevo turno") },
        )
      else -> Unit
    }
  }

  val tabContent: @Composable (PaddingValues) -> Unit = { padding ->
    Box(Modifier.fillMaxSize().padding(padding)) {
      when (selectedTab) {
        MainTab.Home ->
          HomeTab(
            state = state,
            clients = clients,
            pets = pets,
            alerts = alerts,
            todayAppointments = todayAppointments,
            dashboard = dashboard,
            onOpenPet = onOpenPet,
            onOpenAppointments = { selectedTabName = MainTab.Agenda.name },
            onOpenReports = onOpenReports,
            onQuickAction = { action ->
              when (action) {
                QuickAction.Client -> showClientForm = true
                QuickAction.Pet -> showPetForm = true
                QuickAction.Appointment -> showAppointmentForm = true
                QuickAction.Consultation,
                QuickAction.Vaccination -> onOpenPetsList()
                QuickAction.Item -> onOpenInventory()
                QuickAction.Sale -> selectedTabName = MainTab.Sales.name
              }
            },
            onRefreshReminders = clinicalViewModel::refreshReminders,
            modifier = Modifier.fillMaxSize(),
          )
        MainTab.Agenda ->
          AppointmentsTab(
            appointmentsViewModel = appointmentsViewModel,
            pets = pets,
            onOpenPet = onOpenPet,
            onNewAppointment = { showAppointmentForm = true },
            modifier = Modifier.fillMaxSize(),
          )
        MainTab.Clients ->
          ClientsTab(
            clients = clients,
            pets = pets,
            onOpenClient = onOpenClient,
            onNewClient = { showClientForm = true },
            modifier = Modifier.fillMaxSize(),
          )
        MainTab.Sales ->
          SalesTab(
            viewModel = salesViewModel,
            modifier = Modifier.fillMaxSize(),
          )
        MainTab.More ->
          MoreTab(
            onOpenPets = onOpenPetsList,
            onOpenInventory = onOpenInventory,
            onOpenReports = onOpenReports,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.fillMaxSize(),
          )
      }
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp
    if (useRail) {
      Row(Modifier.fillMaxSize()) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
          MainTab.entries.forEach { tab ->
            NavigationRailItem(
              selected = selectedTab == tab,
              onClick = { selectedTabName = tab.name },
              icon = {
                Icon(
                  painter = painterResource(tab.icon(selectedTab == tab)),
                  contentDescription = tab.label,
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text(tab.label) },
              colors =
                NavigationRailItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
          }
        }
        Scaffold(
          modifier = Modifier.weight(1f),
          topBar = topBarContent,
          floatingActionButton = floatingAction,
        ) { padding -> tabContent(padding) }
      }
    } else {
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = topBarContent,
        bottomBar = {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
          ) {
            MainTab.entries.forEach { tab ->
              NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { selectedTabName = tab.name },
                icon = {
                  Icon(
                    painter = painterResource(tab.icon(selectedTab == tab)),
                    contentDescription = tab.label,
                    modifier = Modifier.size(22.dp),
                  )
                },
                label = { Text(tab.label) },
                colors =
                  NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
              )
            }
          }
        },
        floatingActionButton = floatingAction,
      ) { padding -> tabContent(padding) }
    }
  }

  if (showClientForm) {
    ClientFormDialog(
      client = null,
      onSave = { draft, force, onResult ->
        clientsViewModel.save(clientId = null, draft = draft, force = force, onResult = onResult)
      },
      onDismiss = { showClientForm = false },
    )
  }

  if (showPetForm) {
    PetFormDialog(
      pet = null,
      clients = clients,
      configured = petsViewModel.configured,
      online = state.online,
      onCompressPhoto = petsViewModel::compressPhoto,
      onSave = { clientId, draft, photo, _, onResult ->
        if (clientId == null) {
          onResult(Result.failure(IllegalArgumentException("Elegí un cliente")))
        } else {
          petsViewModel.createPet(clientId, draft, photo) { result ->
            onResult(result.map { Unit })
          }
        }
      },
      onDismiss = { showPetForm = false },
      onGoToClients = {
        showPetForm = false
        selectedTabName = MainTab.Clients.name
        showClientForm = true
      },
    )
  }

  if (showAppointmentForm) {
    AppointmentFormDialog(
      appointment = null,
      pets = pets,
      defaultDurationMinutes = appointmentsViewModel.defaultDurationMinutes(),
      onSave = { draft, force, onResult ->
        appointmentsViewModel.create(draft, force, onResult)
      },
      onDismiss = { showAppointmentForm = false },
    )
  }
}
