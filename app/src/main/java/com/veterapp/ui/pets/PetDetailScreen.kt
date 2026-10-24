package com.veterapp.ui.pets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veterapp.domain.Client
import com.veterapp.domain.Consultation
import com.veterapp.domain.Pet
import com.veterapp.domain.PetOwnerHistory
import com.veterapp.domain.PetStatus
import com.veterapp.domain.Vaccination
import com.veterapp.ui.clinical.AddendumDialog
import com.veterapp.ui.clinical.ClinicalHistoryTab
import com.veterapp.ui.clinical.ClinicalViewModel
import com.veterapp.ui.clinical.ConsultationFormDialog
import com.veterapp.ui.clients.ClientPickerDialog
import com.veterapp.ui.clients.ClientsViewModel
import com.veterapp.ui.common.PetStatusChip
import com.veterapp.ui.common.formatDate
import com.veterapp.ui.vaccines.RescheduleDialog
import com.veterapp.ui.vaccines.VaccinationFormDialog
import com.veterapp.ui.vaccines.VaccinationPrefill
import com.veterapp.ui.vaccines.VaccinationsTab
import com.veterapp.util.userMessage
import kotlinx.coroutines.launch

private enum class PetDetailTab(val label: String) {
  Summary("Resumen"),
  Clinical("Historia"),
  Vaccines("Vacunas"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
  petId: String,
  clientsViewModel: ClientsViewModel,
  petsViewModel: PetsViewModel,
  clinicalViewModel: ClinicalViewModel,
  onBack: () -> Unit,
  onOpenClient: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val pet by petsViewModel.observePet(petId).collectAsStateWithLifecycle(initialValue = null)
  val ownerHistory by petsViewModel.observeOwnerHistory(petId)
    .collectAsStateWithLifecycle(initialValue = emptyList())
  val consultations by clinicalViewModel.observeConsultations(petId)
    .collectAsStateWithLifecycle(initialValue = emptyList())
  val addenda by clinicalViewModel.observeAddenda(petId)
    .collectAsStateWithLifecycle(initialValue = emptyList())
  val vaccinations by clinicalViewModel.observeVaccinations(petId)
    .collectAsStateWithLifecycle(initialValue = emptyList())
  val clients by clientsViewModel.clients.collectAsStateWithLifecycle()
  val online by petsViewModel.online.collectAsStateWithLifecycle()
  val snackbar = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var selectedTabName by rememberSaveable(petId) { mutableStateOf(PetDetailTab.Summary.name) }
  val selectedTab =
    runCatching { PetDetailTab.valueOf(selectedTabName) }.getOrDefault(PetDetailTab.Summary)
  var showEdit by remember { mutableStateOf(false) }
  var showArchive by remember { mutableStateOf(false) }
  var showOwnerPicker by remember { mutableStateOf(false) }
  var newOwner by remember { mutableStateOf<Client?>(null) }
  var statusBusy by remember { mutableStateOf(false) }
  var showConsultationForm by remember { mutableStateOf(false) }
  var addendumFor by remember { mutableStateOf<Consultation?>(null) }
  var showVaccinationForm by remember { mutableStateOf(false) }
  var vaccinationPrefill by remember { mutableStateOf<VaccinationPrefill?>(null) }
  var rescheduleFor by remember { mutableStateOf<Vaccination?>(null) }

  fun message(text: String) {
    scope.launch { snackbar.showSnackbar(text) }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(pet?.name ?: "Ficha de la mascota") },
        navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } },
      )
    },
    snackbarHost = { SnackbarHost(snackbar) },
  ) { padding ->
    val current = pet
    if (current == null) {
      Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          "Mascota no encontrada o archivada.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      return@Scaffold
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
      PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
        PetDetailTab.entries.forEach { tab ->
          Tab(
            selected = selectedTab == tab,
            onClick = { selectedTabName = tab.name },
            text = { Text(tab.label) },
          )
        }
      }
      when (selectedTab) {
        PetDetailTab.Summary ->
          PetSummaryTab(
            pet = current,
            owner = clients.firstOrNull { it.id == current.clientId },
            ownerHistory = ownerHistory,
            statusBusy = statusBusy,
            onChangeStatus = { status ->
              if (status != current.status) {
                statusBusy = true
                petsViewModel.changeStatus(current, status) { result ->
                  statusBusy = false
                  result.onFailure { message(it.userMessage("No se pudo cambiar el estado")) }
                }
              }
            },
            onOpenClient = { current.clientId?.let(onOpenClient) },
            onChangeOwner = {
              if (clients.count { it.id != current.clientId } == 0) {
                message("No hay otro cliente. Creá uno desde la pestaña Clientes.")
              } else {
                showOwnerPicker = true
              }
            },
            onEdit = { showEdit = true },
            onArchive = { showArchive = true },
            modifier = Modifier.weight(1f),
          )
        PetDetailTab.Clinical ->
          ClinicalHistoryTab(
            consultations = consultations,
            addenda = addenda,
            onAddConsultation = { showConsultationForm = true },
            onAddAddendum = { addendumFor = it },
            modifier = Modifier.weight(1f),
          )
        PetDetailTab.Vaccines ->
          VaccinationsTab(
            vaccinations = vaccinations,
            onAddVaccination = {
              vaccinationPrefill = null
              showVaccinationForm = true
            },
            onApplyDose = {
              vaccinationPrefill = VaccinationPrefill(vaccineName = it.vaccineName)
              showVaccinationForm = true
            },
            onReschedule = { rescheduleFor = it },
            modifier = Modifier.weight(1f),
          )
      }
    }
  }

  if (showEdit && pet != null) {
    val editing = pet!!
    PetFormDialog(
      pet = editing,
      clients = clients,
      configured = petsViewModel.configured,
      online = online,
      onCompressPhoto = petsViewModel::compressPhoto,
      onSave = { _, draft, photo, removePhoto, onResult ->
        petsViewModel.updatePet(editing, draft, photo, removePhoto) { result ->
          onResult(result.map { Unit })
        }
      },
      onDismiss = { showEdit = false },
    )
  }

  if (showOwnerPicker && pet != null) {
    ClientPickerDialog(
      clients = clients.filter { it.id != pet?.clientId },
      selectedClientId = null,
      title = "Elegir nuevo propietario",
      onSelect = {
        showOwnerPicker = false
        newOwner = it
      },
      onDismiss = { showOwnerPicker = false },
    )
  }

  if (newOwner != null && pet != null) {
    val target = newOwner!!
    val currentPet = pet!!
    var reason by remember(target.id) { mutableStateOf("") }
    var saving by remember(target.id) { mutableStateOf(false) }
    var error by remember(target.id) { mutableStateOf<String?>(null) }
    AlertDialog(
      onDismissRequest = { if (!saving) newOwner = null },
      title = { Text("Cambiar propietario") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "La mascota pasará de ${currentPet.ownerName} a ${target.name}. El cambio queda en el historial.",
            style = MaterialTheme.typography.bodyMedium,
          )
          OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Motivo (opcional)") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
          )
          error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            saving = true
            error = null
            petsViewModel.changeOwner(currentPet, target.id, reason) { result ->
              saving = false
              result
                .onSuccess {
                  newOwner = null
                  message("Propietario actualizado")
                }
                .onFailure { error = it.userMessage("No se pudo cambiar el propietario") }
            }
          },
          enabled = !saving,
        ) {
          Text(if (saving) "Guardando…" else "Cambiar")
        }
      },
      dismissButton = {
        TextButton(onClick = { newOwner = null }, enabled = !saving) { Text("Cancelar") }
      },
    )
  }

  if (showArchive && pet != null) {
    val archiving = pet!!
    AlertDialog(
      onDismissRequest = { showArchive = false },
      title = { Text("Archivar mascota") },
      text = {
        Text(
          "Se marcará a ${archiving.name} como archivada. Los registros clínicos y las fotos no se borran.",
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showArchive = false
            petsViewModel.archivePet(archiving) { result ->
              result
                .onSuccess { onBack() }
                .onFailure { message(it.userMessage("No se pudo archivar la mascota")) }
            }
          },
        ) {
          Text("Archivar", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showArchive = false }) { Text("Cancelar") }
      },
    )
  }

  if (showConsultationForm) {
    ConsultationFormDialog(
      onSave = { draft, onResult ->
        clinicalViewModel.createConsultation(petId, draft) { result ->
          onResult(result.map { Unit })
        }
      },
      onDismiss = { showConsultationForm = false },
    )
  }

  addendumFor?.let { consultation ->
    AddendumDialog(
      consultationDate = consultation.consultedAt,
      onSave = { draft, onResult ->
        clinicalViewModel.addAddendum(consultation.id, draft) { result ->
          onResult(result.map { Unit })
        }
      },
      onDismiss = { addendumFor = null },
    )
  }

  if (showVaccinationForm) {
    VaccinationFormDialog(
      prefill = vaccinationPrefill,
      onSave = { draft, onResult ->
        clinicalViewModel.createVaccination(petId, draft) { result ->
          onResult(result.map { Unit })
        }
      },
      onDismiss = {
        showVaccinationForm = false
        vaccinationPrefill = null
      },
    )
  }

  rescheduleFor?.let { vaccination ->
    RescheduleDialog(
      vaccination = vaccination,
      onSave = { newDueOn, reason, onResult ->
        clinicalViewModel.rescheduleVaccination(vaccination, newDueOn, reason, onResult)
      },
      onDismiss = { rescheduleFor = null },
    )
  }
}

@Composable
private fun PetSummaryTab(
  pet: Pet,
  owner: Client?,
  ownerHistory: List<PetOwnerHistory>,
  statusBusy: Boolean,
  onChangeStatus: (PetStatus) -> Unit,
  onOpenClient: () -> Unit,
  onChangeOwner: () -> Unit,
  onEdit: () -> Unit,
  onArchive: () -> Unit,
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
    PetHeader(pet)
    OwnerCard(
      pet = pet,
      owner = owner,
      onOpenClient = onOpenClient,
      onChangeOwner = onChangeOwner,
    )
    StatusCard(pet = pet, busy = statusBusy, onChange = onChangeStatus)
    if (!pet.notes.isNullOrBlank()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        Column(Modifier.padding(16.dp)) {
          Text("Notas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
          Spacer(Modifier.height(4.dp))
          Text(pet.notes, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
        Text("Editar mascota")
      }
      OutlinedButton(onClick = onArchive, modifier = Modifier.weight(1f)) {
        Text("Archivar", color = MaterialTheme.colorScheme.error)
      }
    }

    HorizontalDivider()
    Text(
      "Historial de propietario",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
    if (ownerHistory.isEmpty()) {
      Text(
        "Sin cambios de propietario.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    } else {
      ownerHistory.forEach { item -> OwnerHistoryRow(item) }
    }
    Spacer(Modifier.height(24.dp))
  }
}

@Composable
private fun PetHeader(pet: Pet) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PetPhoto(pet = pet, size = 88.dp)
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Text(
          pet.name,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(pet.subtitle, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        PetStatusChip(pet.status)
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
private fun OwnerCard(
  pet: Pet,
  owner: Client?,
  onOpenClient: () -> Unit,
  onChangeOwner: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text("Propietario", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Text(owner?.name ?: pet.ownerName, style = MaterialTheme.typography.bodyLarge)
      owner?.contactLine?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (pet.clientId == null) {
        Text(
          "Cliente no vinculado.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onOpenClient, enabled = owner != null) {
          Text("Ver ficha")
        }
        OutlinedButton(onClick = onChangeOwner) {
          Text("Cambiar propietario")
        }
      }
    }
  }
}

@Composable
private fun StatusCard(pet: Pet, busy: Boolean, onChange: (PetStatus) -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text("Estado", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        PetStatus.entries.forEach { status ->
          FilterChip(
            selected = pet.status == status,
            onClick = { onChange(status) },
            enabled = !busy,
            label = { Text(status.label) },
          )
        }
      }
    }
  }
}

@Composable
private fun OwnerHistoryRow(item: PetOwnerHistory) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(Modifier.padding(12.dp)) {
      Text(
        "${item.previousOwnerName} → ${item.newOwnerName}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        formatDate(item.changedAt),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      item.reason?.let {
        Text(it, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}
