package com.veterapp.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veterapp.AppContainer
import com.veterapp.domain.Appointment
import com.veterapp.domain.AppointmentDraft
import com.veterapp.domain.AppointmentSaveResult
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.ConsultationDraft
import com.veterapp.domain.WeeklyHours
import com.veterapp.util.userMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppointmentsViewModel(private val container: AppContainer) : ViewModel() {
  fun observeBetween(from: Long, to: Long): Flow<List<Appointment>> =
    container.appointmentRepository.observeBetween(from, to)

  fun observeForPet(petId: String): Flow<List<Appointment>> =
    container.appointmentRepository.observeUpcomingForPet(petId)

  fun observeForClient(clientId: String): Flow<List<Appointment>> =
    container.appointmentRepository.observeUpcomingForClient(clientId)

  val todayAppointments: StateFlow<List<Appointment>> =
    flow {
        val today = ClinicTime.todayEpochDay()
        emitAll(
          container.appointmentRepository.observeBetween(
            ClinicTime.startOfDay(today),
            ClinicTime.startOfDay(today + 1),
          ),
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun businessHours(): WeeklyHours = container.settings.businessHours()

  fun defaultDurationMinutes(): Int = container.settings.defaultDurationMinutes

  fun create(
    draft: AppointmentDraft,
    force: Boolean,
    onResult: (AppointmentSaveResult) -> Unit,
  ) {
    viewModelScope.launch {
      val result =
        runCatching { container.appointmentRepository.create(draft, force) }
          .getOrElse {
            AppointmentSaveResult.Failure(it.userMessage("No se pudo guardar el turno"))
          }
      if (result is AppointmentSaveResult.Success) container.syncScheduler.requestSync()
      onResult(result)
    }
  }

  fun update(
    appointmentId: String,
    draft: AppointmentDraft,
    force: Boolean,
    onResult: (AppointmentSaveResult) -> Unit,
  ) {
    viewModelScope.launch {
      val result =
        runCatching { container.appointmentRepository.update(appointmentId, draft, force) }
          .getOrElse {
            AppointmentSaveResult.Failure(it.userMessage("No se pudo guardar el turno"))
          }
      if (result is AppointmentSaveResult.Success) container.syncScheduler.requestSync()
      onResult(result)
    }
  }

  fun confirm(appointmentId: String, onResult: (Result<Unit>) -> Unit) {
    statusAction({ container.appointmentRepository.confirm(appointmentId) }, onResult)
  }

  fun cancel(appointmentId: String, reason: String?, onResult: (Result<Unit>) -> Unit) {
    statusAction({ container.appointmentRepository.cancel(appointmentId, reason) }, onResult)
  }

  fun markNoShow(appointmentId: String, onResult: (Result<Unit>) -> Unit) {
    statusAction({ container.appointmentRepository.markNoShow(appointmentId) }, onResult)
  }

  fun attend(
    appointment: Appointment,
    draft: ConsultationDraft,
    onResult: (Result<Unit>) -> Unit,
  ) {
    viewModelScope.launch {
      runCatching {
          val consultation =
            container.clinicalRepository.createConsultation(appointment.petId, draft)
          container.appointmentRepository.markAttended(appointment.id, consultation.id)
        }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }

  private fun statusAction(
    action: suspend () -> Unit,
    onResult: (Result<Unit>) -> Unit,
  ) {
    viewModelScope.launch {
      runCatching { action() }
        .onSuccess { container.syncScheduler.requestSync() }
        .fold(
          onSuccess = { onResult(Result.success(Unit)) },
          onFailure = { onResult(Result.failure(it)) },
        )
    }
  }
}
