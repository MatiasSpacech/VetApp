package com.veterapp.ui.clinical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veterapp.AppContainer
import com.veterapp.domain.Consultation
import com.veterapp.domain.ConsultationAddendum
import com.veterapp.domain.ConsultationAddendumDraft
import com.veterapp.domain.ConsultationDraft
import com.veterapp.domain.Vaccination
import com.veterapp.domain.VaccinationAlert
import com.veterapp.domain.VaccinationDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClinicalViewModel(private val container: AppContainer) : ViewModel() {
  val alerts: StateFlow<List<VaccinationAlert>> =
    container.vaccinationRepository.alerts()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun observeConsultations(petId: String): Flow<List<Consultation>> =
    container.clinicalRepository.observeConsultations(petId)

  fun observeAddenda(petId: String): Flow<List<ConsultationAddendum>> =
    container.clinicalRepository.observeAddenda(petId)

  fun observeVaccinations(petId: String): Flow<List<Vaccination>> =
    container.vaccinationRepository.observeForPet(petId)

  fun createConsultation(
    petId: String,
    draft: ConsultationDraft,
    onResult: (Result<Consultation>) -> Unit,
  ) {
    viewModelScope.launch {
      runCatching { container.clinicalRepository.createConsultation(petId, draft) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun addAddendum(
    consultationId: String,
    draft: ConsultationAddendumDraft,
    onResult: (Result<ConsultationAddendum>) -> Unit,
  ) {
    viewModelScope.launch {
      runCatching { container.clinicalRepository.addAddendum(consultationId, draft) }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun createVaccination(
    petId: String,
    draft: VaccinationDraft,
    onResult: (Result<Vaccination>) -> Unit,
  ) {
    viewModelScope.launch {
      val previous = runCatching { container.vaccinationRepository.latestFor(petId, draft.vaccineName) }.getOrNull()
      runCatching { container.vaccinationRepository.createVaccination(petId, draft) }
        .onSuccess {
          previous?.let { old -> container.reminderNotifications.cancelForVaccination(old.id) }
          container.syncScheduler.requestSync()
          container.reminderScheduler.requestRefresh()
        }
        .fold(
          onSuccess = { onResult(Result.success(it)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun rescheduleVaccination(
    vaccination: Vaccination,
    newDueOn: Long,
    reason: String?,
    onResult: (Result<Unit>) -> Unit,
  ) {
    viewModelScope.launch {
      runCatching { container.vaccinationRepository.reschedule(vaccination.id, newDueOn, reason) }
        .onSuccess {
          container.reminderNotifications.cancelForVaccination(vaccination.id)
          container.syncScheduler.requestSync()
          container.reminderScheduler.requestRefresh()
        }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  fun refreshReminders() {
    container.reminderScheduler.requestRefresh()
  }
}
