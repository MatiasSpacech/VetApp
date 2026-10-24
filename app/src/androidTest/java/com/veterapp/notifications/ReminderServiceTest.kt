package com.veterapp.notifications

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veterapp.data.AppSettings
import com.veterapp.data.AuditRepository
import com.veterapp.data.ClientRepository
import com.veterapp.data.PetRepository
import com.veterapp.data.VaccinationRepository
import com.veterapp.data.local.VetDatabase
import com.veterapp.domain.ClientDraft
import com.veterapp.domain.ClientSaveResult
import com.veterapp.domain.PetDraft
import com.veterapp.domain.PetStatus
import com.veterapp.domain.ReminderMilestone
import com.veterapp.domain.ReminderPlan
import com.veterapp.domain.VaccinationDraft
import com.veterapp.domain.VaccinationSchedule
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderServiceTest {
  @Test
  fun notifiesEachMilestoneOnceAndSkipsInactivePets() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val database = Room.inMemoryDatabaseBuilder(context, VetDatabase::class.java).build()
      val settings = AppSettings(context)
      settings.clinicId = "local-clinic"
      settings.userId = null
      val audit = AuditRepository(database, settings)
      val clients = ClientRepository(database, settings, audit)
      val pets = PetRepository(database, settings, audit)
      val vaccinations = VaccinationRepository(database, settings, audit)
      val notifier = FakeNotifier()
      val service = ReminderService(vaccinations, database.vaccinationNotificationDao(), notifier)
      val petId = UUID.randomUUID().toString()
      val today = LocalDate.of(2026, 10, 24)

      val client =
        (clients.create(
          ClientDraft(
            name = "Ana",
            phone = null,
            document = null,
            email = null,
            address = null,
            notes = null,
          ),
        ) as ClientSaveResult.Success).client
      val pet =
        pets.createPet(
          clientId = client.id,
          petId = petId,
          draft =
            PetDraft(
              name = "Luna",
              species = "Perro",
              breed = null,
              notes = null,
            ),
          fotoUrl = null,
        )
      vaccinations.createVaccination(
        petId = pet.id,
        draft =
          VaccinationDraft(
            vaccineName = "Rabia",
            appliedOn = today.toEpochDay(),
            schedule = VaccinationSchedule.Manual,
            manualNextDueOn = today.plusDays(10).toEpochDay(),
            lot = null,
            notes = null,
          ),
      )

      assertEquals(1, service.run(today))
      assertEquals(ReminderMilestone.Month, notifier.shown.single().milestone)
      assertEquals(1, database.vaccinationNotificationDao().all().size)

      assertEquals(0, service.run(today))
      assertEquals(1, notifier.shown.size)

      assertEquals(1, service.run(today.plusDays(5)))
      assertEquals(ReminderMilestone.Week, notifier.shown.last().milestone)
      assertEquals(2, database.vaccinationNotificationDao().all().size)

      pets.changeStatus(petId, PetStatus.Inactive)
      vaccinations.createVaccination(
        petId = pet.id,
        draft =
          VaccinationDraft(
            vaccineName = "Sextuple",
            appliedOn = today.toEpochDay(),
            schedule = VaccinationSchedule.Manual,
            manualNextDueOn = today.plusDays(1).toEpochDay(),
            lot = null,
            notes = null,
          ),
      )
      assertEquals(0, service.run(today.plusDays(1)))
      assertEquals(2, notifier.shown.size)

      notifier.allowed = false
      assertTrue(!notifier.canPost())
      assertEquals(0, service.run(today.plusDays(2)))

      database.close()
    }
  }

  private class FakeNotifier : ReminderNotifier {
    val shown = mutableListOf<ReminderPlan>()
    var allowed = true

    override fun canPost(): Boolean = allowed

    override fun show(plan: ReminderPlan): Boolean {
      if (!allowed) return false
      shown += plan
      return true
    }

    override fun cancelForVaccination(vaccinationId: String) = Unit
  }
}
