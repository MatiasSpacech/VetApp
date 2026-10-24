package com.veterapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veterapp.data.local.VaccinationNotificationEntity
import com.veterapp.data.local.VetDatabase
import com.veterapp.domain.ClientDraft
import com.veterapp.domain.ClientSaveResult
import com.veterapp.domain.ConsultationAddendumDraft
import com.veterapp.domain.ConsultationDraft
import com.veterapp.domain.PetDraft
import com.veterapp.domain.ReminderKey
import com.veterapp.domain.ReminderMilestone
import com.veterapp.domain.VaccinationDraft
import com.veterapp.domain.VaccinationSchedule
import com.veterapp.domain.VaccineStatus
import com.veterapp.domain.planReminders
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClinicalRepositoryTest {
  @Test
  fun recordsClinicalHistoryAndVaccinationReminders() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val database = Room.inMemoryDatabaseBuilder(context, VetDatabase::class.java).build()
      val settings = AppSettings(context)
      settings.clinicId = "local-clinic"
      settings.userId = null
      val audit = AuditRepository(database, settings)
      val clients = ClientRepository(database, settings, audit)
      val pets = PetRepository(database, settings, audit)
      val clinical = ClinicalRepository(database, settings, audit)
      val vaccinations = VaccinationRepository(database, settings, audit)
      val petId = UUID.randomUUID().toString()
      val today = LocalDate.of(2026, 10, 24)

      val client =
        (clients.create(
          ClientDraft(
            name = "Ana",
            phone = "11 5555-1234",
            document = null,
            email = null,
            address = null,
            notes = null,
          ),
        ) as ClientSaveResult.Success).client
      pets.createPet(
        clientId = client.id,
        petId = petId,
        draft =
          PetDraft(
            name = "Luna",
            species = "Perro",
            breed = "Mestiza",
            notes = null,
          ),
        fotoUrl = null,
      )

      val consultation =
        clinical.createConsultation(
          petId = petId,
          draft =
            ConsultationDraft(
              consultedOn = today.toEpochDay(),
              reason = "Control anual",
              exam = "Normal",
              diagnosis = "Sana",
              treatment = null,
              medication = null,
              instructions = "Volver en 6 meses",
            ),
        )
      val consultations = clinical.observeConsultations(petId).first()
      assertEquals(1, consultations.size)
      assertEquals(consultation.id, consultations.first().id)
      assertEquals(today, consultations.first().consultedOn)
      assertTrue(consultations.first().pendingSync)

      clinical.addAddendum(
        consultationId = consultation.id,
        draft = ConsultationAddendumDraft(reason = "Corrección", text = "Se ajusta la indicación"),
      )
      assertEquals(1, clinical.observeAddenda(petId).first().size)
      assertEquals(1, database.auditDao().all().count { it.action == "consultation.addendum" })

      val vaccination =
        vaccinations.createVaccination(
          petId = petId,
          draft =
            VaccinationDraft(
              vaccineName = "Rabia",
              appliedOn = today.toEpochDay(),
              schedule = VaccinationSchedule.SixMonths,
              manualNextDueOn = null,
              lot = "L-123",
              notes = null,
            ),
        )
      assertEquals(today.plusMonths(6).toEpochDay(), vaccination.nextDueOn)
      assertEquals(1, vaccinations.observeForPet(petId).first().size)

      val alerts = vaccinations.alertsFor(today).first()
      assertTrue(alerts.isEmpty())
      assertTrue(planReminders(today, vaccinations.reminderCandidates(), emptySet()).isEmpty())

      val newDue = today.plusDays(10).toEpochDay()
      vaccinations.reschedule(vaccination.id, newDue, "Viaje")
      val updated = vaccinations.observeForPet(petId).first().single()
      assertEquals(newDue, updated.nextDueOn)
      val rescheduledAlerts = vaccinations.alertsFor(today).first()
      assertEquals(1, rescheduledAlerts.size)
      assertEquals("Luna", rescheduledAlerts.first().petName)
      assertEquals(VaccineStatus.Upcoming, rescheduledAlerts.first().status)
      val plan = planReminders(today, vaccinations.reminderCandidates(), emptySet()).single()
      assertEquals(ReminderMilestone.Month, plan.milestone)
      assertEquals(1, database.auditDao().all().count { it.action == "vaccination.reschedule" })

      database.vaccinationNotificationDao().insert(
        VaccinationNotificationEntity(
          id = UUID.randomUUID().toString(),
          clinicId = "local-clinic",
          vaccinationId = vaccination.id,
          petId = petId,
          dueOn = newDue,
          milestone = ReminderMilestone.Month.storage,
          notifiedAt = today.toEpochDay(),
        ),
      )
      assertEquals(1, database.vaccinationNotificationDao().all().size)
      assertTrue(
        planReminders(
          today,
          vaccinations.reminderCandidates(),
          setOf(ReminderKey(vaccination.id, newDue, ReminderMilestone.Month)),
        ).isEmpty(),
      )
      assertEquals(6, database.outboxDao().observeCount().first())

      database.close()
    }
  }
}
