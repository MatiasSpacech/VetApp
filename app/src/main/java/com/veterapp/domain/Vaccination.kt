package com.veterapp.domain

import com.veterapp.data.local.VaccinationEntity
import java.time.LocalDate

enum class VaccinationSchedule(val storage: String, val label: String) {
  SixMonths("6m", "6 meses"),
  TwelveMonths("12m", "12 meses"),
  Manual("manual", "Fecha manual"),
  None("none", "Sin próximo aviso"),
  ;

  companion object {
    fun fromStorage(value: String?): VaccinationSchedule =
      entries.firstOrNull { it.storage == value } ?: SixMonths
  }
}

enum class VaccineStatus(val label: String) {
  Overdue("Vencida"),
  DueSoon("Por vencer"),
  Upcoming("Próxima"),
  Scheduled("Programada"),
  ;

  companion object {
    fun fromDays(daysUntilDue: Long): VaccineStatus =
      when {
        daysUntilDue < 0 -> Overdue
        daysUntilDue <= 7 -> DueSoon
        daysUntilDue <= 30 -> Upcoming
        else -> Scheduled
      }
  }
}

data class Vaccination(
  val id: String,
  val clinicId: String,
  val petId: String,
  val vaccineName: String,
  val appliedOn: Long,
  val nextDueOn: Long?,
  val lot: String?,
  val notes: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val pendingSync: Boolean,
) {
  val appliedDate: LocalDate
    get() = LocalDate.ofEpochDay(appliedOn)

  val nextDueDate: LocalDate?
    get() = nextDueOn?.let(LocalDate::ofEpochDay)

  fun status(today: LocalDate = ClinicTime.today()): VaccineStatus? =
    nextDueOn?.let { VaccineStatus.fromDays(it - today.toEpochDay()) }

  fun daysUntilDue(today: LocalDate = ClinicTime.today()): Long? =
    nextDueOn?.let { it - today.toEpochDay() }
}

data class VaccinationDraft(
  val vaccineName: String,
  val appliedOn: Long,
  val schedule: VaccinationSchedule,
  val manualNextDueOn: Long?,
  val lot: String?,
  val notes: String?,
) {
  fun validate(today: LocalDate = ClinicTime.today()): String? {
    if (vaccineName.isBlank()) return "Completá el nombre de la vacuna"
    if (appliedOn > today.toEpochDay()) return "La fecha de aplicación no puede ser futura"
    if (schedule == VaccinationSchedule.Manual) {
      val manual = manualNextDueOn ?: return "Elegí la fecha del próximo aviso"
      if (manual <= appliedOn) return "El próximo aviso debe ser posterior a la aplicación"
    }
    return null
  }

  fun nextDueOn(): Long? =
    when (schedule) {
      VaccinationSchedule.SixMonths -> LocalDate.ofEpochDay(appliedOn).plusMonths(6).toEpochDay()
      VaccinationSchedule.TwelveMonths -> LocalDate.ofEpochDay(appliedOn).plusMonths(12).toEpochDay()
      VaccinationSchedule.Manual -> manualNextDueOn
      VaccinationSchedule.None -> null
    }
}

data class VaccinationAlert(
  val vaccination: Vaccination,
  val petName: String,
  val daysUntilDue: Long,
) {
  val status: VaccineStatus
    get() = VaccineStatus.fromDays(daysUntilDue)
}

data class VaccinationReminderCandidate(
  val vaccination: Vaccination,
  val petName: String,
)

fun List<Vaccination>.latestPerPetAndVaccine(): List<Vaccination> =
  groupBy { it.petId to it.vaccineName.trim().lowercase() }
    .values
    .mapNotNull { list ->
      list.maxWithOrNull(compareBy<Vaccination>({ it.appliedOn }, { it.createdAt }))
    }

fun List<VaccinationReminderCandidate>.latestCandidatesPerPetAndVaccine(): List<VaccinationReminderCandidate> =
  groupBy { it.vaccination.petId to it.vaccination.vaccineName.trim().lowercase() }
    .values
    .mapNotNull { list ->
      list.maxWithOrNull(
        compareBy<VaccinationReminderCandidate>(
          { it.vaccination.appliedOn },
          { it.vaccination.createdAt },
        ),
      )
    }

fun VaccinationEntity.toDomain(): Vaccination =
  Vaccination(
    id = id,
    clinicId = clinicId,
    petId = petId,
    vaccineName = vaccineName,
    appliedOn = appliedOn,
    nextDueOn = nextDueOn,
    lot = lot,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    revision = revision,
    pendingSync = pendingSync,
  )
