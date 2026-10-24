package com.veterapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veterapp.data.local.VetDatabase
import com.veterapp.domain.AppointmentDraft
import com.veterapp.domain.AppointmentSaveResult
import com.veterapp.domain.AppointmentStatus
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.ClientDraft
import com.veterapp.domain.ClientSaveResult
import com.veterapp.domain.ConsultationDraft
import com.veterapp.domain.PetDraft
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppointmentRepositoryTest {
  @Test
  fun createsWarnsConfirmsCancelsAndAttendsAppointments() {
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
      val appointments = AppointmentRepository(database, settings, audit)
      val petId = UUID.randomUUID().toString()
      val day = LocalDate.of(2026, 10, 26).toEpochDay()

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
        draft = PetDraft(name = "Luna", species = "Perro", breed = null, notes = null),
        fotoUrl = null,
      )

      val first =
        appointments.create(
          AppointmentDraft(
            petId = petId,
            startsAt = ClinicTime.instantAt(day, 10 * 60),
            endsAt = ClinicTime.instantAt(day, 10 * 60 + 30),
            reason = "Control",
            notes = null,
          ),
        )
      assertTrue(first is AppointmentSaveResult.Success)
      val firstAppointment = (first as AppointmentSaveResult.Success).appointment
      assertEquals("Luna", firstAppointment.petName)
      assertEquals("Ana", firstAppointment.ownerName)
      assertEquals("11 5555-1234", firstAppointment.ownerPhone)
      assertEquals(AppointmentStatus.Scheduled, firstAppointment.status)
      assertTrue(firstAppointment.pendingSync)

      val observed = appointments.observeBetween(
        ClinicTime.startOfDay(day),
        ClinicTime.startOfDay(day + 1),
      ).first()
      assertEquals(1, observed.size)

      val conflict =
        appointments.create(
          AppointmentDraft(
            petId = petId,
            startsAt = ClinicTime.instantAt(day, 10 * 60 + 15),
            endsAt = ClinicTime.instantAt(day, 10 * 60 + 45),
            reason = "Vacuna",
            notes = null,
          ),
        )
      assertTrue(conflict is AppointmentSaveResult.Warning)
      assertEquals(1, (conflict as AppointmentSaveResult.Warning).warning.conflicts.size)

      val forced =
        appointments.create(
          AppointmentDraft(
            petId = petId,
            startsAt = ClinicTime.instantAt(day, 10 * 60 + 15),
            endsAt = ClinicTime.instantAt(day, 10 * 60 + 45),
            reason = "Vacuna",
            notes = null,
          ),
          force = true,
        )
      assertTrue(forced is AppointmentSaveResult.Success)

      val outside =
        appointments.create(
          AppointmentDraft(
            petId = petId,
            startsAt = ClinicTime.instantAt(day, 20 * 60),
            endsAt = ClinicTime.instantAt(day, 20 * 60 + 30),
            reason = "Urgencia",
            notes = null,
          ),
        )
      assertTrue(outside is AppointmentSaveResult.Warning)
      assertTrue((outside as AppointmentSaveResult.Warning).warning.outsideBusinessHours)

      appointments.confirm(firstAppointment.id)
      assertEquals(
        AppointmentStatus.Confirmed,
        appointments.findById(firstAppointment.id)!!.status,
      )

      val toCancel = appointments.create(
        AppointmentDraft(
          petId = petId,
          startsAt = ClinicTime.instantAt(day, 11 * 60),
          endsAt = ClinicTime.instantAt(day, 11 * 60 + 30),
          reason = "Consulta",
          notes = null,
        ),
        force = true,
      )
      val cancelId = (toCancel as AppointmentSaveResult.Success).appointment.id
      appointments.cancel(cancelId, "El dueño no puede")
      val cancelled = appointments.findById(cancelId)!!
      assertEquals(AppointmentStatus.Cancelled, cancelled.status)
      assertEquals("El dueño no puede", cancelled.cancelReason)

      val toAttend = appointments.create(
        AppointmentDraft(
          petId = petId,
          startsAt = ClinicTime.instantAt(day, 12 * 60),
          endsAt = ClinicTime.instantAt(day, 12 * 60 + 30),
          reason = "Control",
          notes = null,
        ),
        force = true,
      )
      val attendId = (toAttend as AppointmentSaveResult.Success).appointment.id
      val consultation =
        clinical.createConsultation(
          petId = petId,
          draft =
            ConsultationDraft(
              consultedOn = ClinicTime.todayEpochDay(),
              reason = "Control",
              exam = "Normal",
              diagnosis = "Sana",
              treatment = null,
              medication = null,
              instructions = null,
            ),
        )
      appointments.markAttended(attendId, consultation.id)
      val attended = appointments.findById(attendId)!!
      assertEquals(AppointmentStatus.Attended, attended.status)
      assertEquals(consultation.id, attended.consultationId)

      val toMiss = appointments.create(
        AppointmentDraft(
          petId = petId,
          startsAt = ClinicTime.instantAt(day, 13 * 60),
          endsAt = ClinicTime.instantAt(day, 13 * 60 + 30),
          reason = "Control",
          notes = null,
        ),
        force = true,
      )
      val missId = (toMiss as AppointmentSaveResult.Success).appointment.id
      appointments.markNoShow(missId)
      assertEquals(AppointmentStatus.NoShow, appointments.findById(missId)!!.status)

      assertTrue(database.auditDao().all().any { it.action == "appointment.create" })
      assertTrue(database.auditDao().all().any { it.action == "appointment.cancel" })
      assertTrue(database.auditDao().all().any { it.action == "appointment.attend" })
      assertTrue(database.auditDao().all().any { it.action == "appointment.no_show" })
      assertTrue(database.outboxDao().observeCount().first() >= 5)

      database.close()
    }
  }
}
