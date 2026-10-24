package com.veterapp.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaccinationTest {
  @Test
  fun computesNextDoseForSchedules() {
    val applied = LocalDate.of(2026, 1, 31)
    assertEquals(
      LocalDate.of(2026, 7, 31).toEpochDay(),
      draft(applied, VaccinationSchedule.SixMonths).nextDueOn(),
    )
    assertEquals(
      LocalDate.of(2027, 1, 31).toEpochDay(),
      draft(applied, VaccinationSchedule.TwelveMonths).nextDueOn(),
    )
    assertEquals(
      LocalDate.of(2026, 3, 15).toEpochDay(),
      draft(
        applied,
        VaccinationSchedule.Manual,
        manual = LocalDate.of(2026, 3, 15).toEpochDay(),
      ).nextDueOn(),
    )
    assertNull(draft(applied, VaccinationSchedule.None).nextDueOn())
  }

  @Test
  fun handlesLeapYearAndMonthEnd() {
    val leap = LocalDate.of(2024, 2, 29)
    assertEquals(
      LocalDate.of(2025, 2, 28).toEpochDay(),
      draft(leap, VaccinationSchedule.TwelveMonths).nextDueOn(),
    )
    val monthEnd = LocalDate.of(2026, 8, 31)
    assertEquals(
      LocalDate.of(2027, 2, 28).toEpochDay(),
      draft(monthEnd, VaccinationSchedule.SixMonths).nextDueOn(),
    )
  }

  @Test
  fun classifiesStatusByDaysUntilDue() {
    assertEquals(VaccineStatus.Overdue, VaccineStatus.fromDays(-1))
    assertEquals(VaccineStatus.DueSoon, VaccineStatus.fromDays(0))
    assertEquals(VaccineStatus.DueSoon, VaccineStatus.fromDays(7))
    assertEquals(VaccineStatus.Upcoming, VaccineStatus.fromDays(8))
    assertEquals(VaccineStatus.Upcoming, VaccineStatus.fromDays(30))
    assertEquals(VaccineStatus.Scheduled, VaccineStatus.fromDays(31))
  }

  @Test
  fun keepsOnlyLatestDosePerPetAndVaccine() {
    val older = vaccination(id = "old", appliedOn = 10, nextDueOn = 100)
    val newer = vaccination(id = "new", appliedOn = 20, nextDueOn = 200)
    val otherVaccine = vaccination(id = "other", name = "Sextuple", appliedOn = 5, nextDueOn = 90)
    val otherPet = vaccination(id = "other-pet", petId = "pet-2", appliedOn = 5, nextDueOn = 90)

    val latest = listOf(older, newer, otherVaccine, otherPet).latestPerPetAndVaccine()

    assertEquals(setOf("new", "other", "other-pet"), latest.map { it.id }.toSet())
  }

  @Test
  fun validatesDraft() {
    val today = LocalDate.of(2026, 10, 24)
    assertNull(
      VaccinationDraft(
        vaccineName = "Rabia",
        appliedOn = today.toEpochDay(),
        schedule = VaccinationSchedule.SixMonths,
        manualNextDueOn = null,
        lot = null,
        notes = null,
      ).validate(today),
    )
    assertEquals(
      "Completá el nombre de la vacuna",
      draft(today, VaccinationSchedule.SixMonths, name = " ").validate(today),
    )
    assertEquals(
      "La fecha de aplicación no puede ser futura",
      draft(today.plusDays(1), VaccinationSchedule.SixMonths).validate(today),
    )
    assertEquals(
      "Elegí la fecha del próximo aviso",
      draft(today, VaccinationSchedule.Manual, manual = null).validate(today),
    )
    assertEquals(
      "El próximo aviso debe ser posterior a la aplicación",
      draft(today, VaccinationSchedule.Manual, manual = today.toEpochDay()).validate(today),
    )
  }

  @Test
  fun plansOnlyTheMostUrgentMilestoneAndSkipsNotified() {
    val today = LocalDate.of(2026, 11, 1)
    val month = candidate("month", "Luna", "Rabia", dueInDays = 20, today = today)
    val week = candidate("week", "Milo", "Sextuple", dueInDays = 5, today = today)
    val overdue = candidate("overdue", "Nala", "Leptospirosis", dueInDays = -2, today = today)
    val far = candidate("far", "Toby", "Moquillo", dueInDays = 45, today = today)
    val already = candidate("already", "Kira", "Rabia", dueInDays = 20, today = today)

    val plans =
      planReminders(
        today = today,
        candidates = listOf(month, week, overdue, far, already),
        alreadyNotified =
          setOf(
            ReminderKey(
              vaccinationId = already.vaccination.id,
              dueOn = already.vaccination.nextDueOn!!,
              milestone = ReminderMilestone.Month,
            ),
          ),
      )

    assertEquals(3, plans.size)
    assertEquals(ReminderMilestone.Overdue, plans[0].milestone)
    assertEquals(ReminderMilestone.Week, plans[1].milestone)
    assertEquals(ReminderMilestone.Month, plans[2].milestone)
    assertTrue(plans.none { it.vaccinationId == "far" || it.vaccinationId == "already" })
  }

  @Test
  fun doesNotRemindOlderDoseWhenANewerDoseExists() {
    val today = LocalDate.of(2026, 11, 1)
    val older = candidate("older", "Luna", "Rabia", dueInDays = 5, today = today)
    val newer =
      VaccinationReminderCandidate(
        vaccination =
          older.vaccination.copy(
            id = "newer",
            appliedOn = today.toEpochDay(),
            nextDueOn = today.plusMonths(6).toEpochDay(),
            createdAt = 2,
          ),
        petName = "Luna",
      )

    val plans = planReminders(today, listOf(older, newer), emptySet())

    assertTrue(plans.isEmpty())
  }

  private fun draft(
    appliedOn: LocalDate,
    schedule: VaccinationSchedule,
    manual: Long? = null,
    name: String = "Rabia",
  ): VaccinationDraft =
    VaccinationDraft(
      vaccineName = name,
      appliedOn = appliedOn.toEpochDay(),
      schedule = schedule,
      manualNextDueOn = manual,
      lot = null,
      notes = null,
    )

  private fun candidate(
    id: String,
    petName: String,
    vaccineName: String,
    dueInDays: Long,
    today: LocalDate,
  ): VaccinationReminderCandidate =
    VaccinationReminderCandidate(
      vaccination =
        vaccination(
          id = id,
          name = vaccineName,
          appliedOn = today.minusMonths(1).toEpochDay(),
          nextDueOn = today.plusDays(dueInDays).toEpochDay(),
        ),
      petName = petName,
    )

  private fun vaccination(
    id: String,
    petId: String = "pet-1",
    name: String = "Rabia",
    appliedOn: Long,
    nextDueOn: Long?,
    createdAt: Long = appliedOn,
  ): Vaccination =
    Vaccination(
      id = id,
      clinicId = "clinic-1",
      petId = petId,
      vaccineName = name,
      appliedOn = appliedOn,
      nextDueOn = nextDueOn,
      lot = null,
      notes = null,
      createdAt = createdAt,
      updatedAt = createdAt,
      revision = 0,
      pendingSync = false,
    )
}
