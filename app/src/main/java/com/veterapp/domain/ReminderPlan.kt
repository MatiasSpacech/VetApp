package com.veterapp.domain

import java.time.LocalDate

enum class ReminderMilestone(val storage: String) {
  Month("30d"),
  Week("7d"),
  Overdue("overdue"),
  ;

  companion object {
    fun fromStorage(value: String?): ReminderMilestone? =
      entries.firstOrNull { it.storage == value }
  }
}

data class ReminderKey(
  val vaccinationId: String,
  val dueOn: Long,
  val milestone: ReminderMilestone,
)

data class ReminderPlan(
  val vaccinationId: String,
  val clinicId: String,
  val petId: String,
  val petName: String,
  val vaccineName: String,
  val dueOn: Long,
  val daysUntilDue: Long,
  val milestone: ReminderMilestone,
) {
  val key: ReminderKey
    get() = ReminderKey(vaccinationId, dueOn, milestone)
}

fun planReminders(
  today: LocalDate,
  candidates: List<VaccinationReminderCandidate>,
  alreadyNotified: Set<ReminderKey>,
  monthDays: Int = 30,
  weekDays: Int = 7,
  includeOverdue: Boolean = true,
): List<ReminderPlan> =
  candidates
    .latestCandidatesPerPetAndVaccine()
    .mapNotNull { candidate ->
      val vaccination = candidate.vaccination
      val dueOn = vaccination.nextDueOn ?: return@mapNotNull null
      val days = dueOn - today.toEpochDay()
      val milestone =
        when {
          days < 0 -> if (includeOverdue) ReminderMilestone.Overdue else return@mapNotNull null
          days <= weekDays -> ReminderMilestone.Week
          days <= monthDays -> ReminderMilestone.Month
          else -> return@mapNotNull null
        }
      val key = ReminderKey(vaccination.id, dueOn, milestone)
      if (key in alreadyNotified) {
        null
      } else {
        ReminderPlan(
          vaccinationId = vaccination.id,
          clinicId = vaccination.clinicId,
          petId = vaccination.petId,
          petName = candidate.petName,
          vaccineName = vaccination.vaccineName,
          dueOn = dueOn,
          daysUntilDue = days,
          milestone = milestone,
        )
      }
    }
    .sortedWith(compareBy<ReminderPlan>({ it.daysUntilDue }, { it.petName.lowercase() }))
