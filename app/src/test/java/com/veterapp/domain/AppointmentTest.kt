package com.veterapp.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentTest {
  private val day = LocalDate.of(2026, 10, 26).toEpochDay()

  @Test
  fun validatesDraftDatesAndReason() {
    val valid =
      AppointmentDraft(
        petId = "pet-1",
        startsAt = ClinicTime.instantAt(day, 10 * 60),
        endsAt = ClinicTime.instantAt(day, 10 * 60 + 30),
        reason = "Control",
        notes = null,
      )
    assertNull(valid.validate())

    assertEquals(
      "Elegí una mascota",
      valid.copy(petId = "").validate(),
    )
    assertEquals(
      "Completá el motivo del turno",
      valid.copy(reason = " ").validate(),
    )
    assertEquals(
      "La hora de fin debe ser posterior a la de inicio",
      valid.copy(endsAt = valid.startsAt).validate(),
    )
    assertEquals(
      "El turno debe durar al menos 5 minutos",
      valid.copy(endsAt = valid.startsAt + 60_000L).validate(),
    )
  }

  @Test
  fun detectsOverlapsUsingHalfOpenIntervals() {
    val base = appointment(startMinute = 10 * 60, endMinute = 10 * 60 + 30)
    val before = appointment(startMinute = 9 * 60, endMinute = 10 * 60)
    val after = appointment(startMinute = 10 * 60 + 30, endMinute = 11 * 60)
    val inside = appointment(startMinute = 10 * 60 + 10, endMinute = 10 * 60 + 20)

    assertFalse(base.overlaps(before))
    assertFalse(base.overlaps(after))
    assertTrue(base.overlaps(inside))
    assertTrue(inside.overlaps(base))
  }

  @Test
  fun checksBusinessHoursByDay() {
    val hours = WeeklyHours()
    val monday = LocalDate.of(2026, 10, 26).toEpochDay()
    val sunday = LocalDate.of(2026, 11, 1).toEpochDay()

    assertTrue(
      hours.isWithin(
        ClinicTime.instantAt(monday, 9 * 60),
        ClinicTime.instantAt(monday, 18 * 60),
      ),
    )
    assertFalse(
      hours.isWithin(
        ClinicTime.instantAt(monday, 8 * 60),
        ClinicTime.instantAt(monday, 10 * 60),
      ),
    )
    assertFalse(
      hours.isWithin(
        ClinicTime.instantAt(monday, 18 * 60),
        ClinicTime.instantAt(monday, 20 * 60),
      ),
    )
    assertFalse(
      hours.isWithin(
        ClinicTime.instantAt(sunday, 10 * 60),
        ClinicTime.instantAt(sunday, 11 * 60),
      ),
    )
  }

  @Test
  fun normalizesArgentinePhoneNumbersForWhatsApp() {
    assertEquals("5491155551234", normalizeWhatsAppPhone("11 5555-1234"))
    assertEquals("5491155551234", normalizeWhatsAppPhone("011 15-5555-1234"))
    assertEquals("5491155551234", normalizeWhatsAppPhone("+54 9 11 5555-1234"))
    assertEquals("5491155551234", normalizeWhatsAppPhone("54 9 11 5555 1234"))
    assertNull(normalizeWhatsAppPhone("123"))
  }

  @Test
  fun buildsWhatsAppMessageAndUrl() {
    val appointment = appointment(startMinute = 10 * 60, endMinute = 10 * 60 + 30)
    val message = buildWhatsAppMessage(appointment)
    assertTrue(message.contains("Luna"))
    assertTrue(message.contains("Control"))
    assertTrue(message.contains("26/10/2026"))
    assertTrue(message.contains("10:00"))

    val url = buildWhatsAppUrl(appointment)
    assertTrue(url!!.startsWith("https://wa.me/5491155551234?text="))
    assertTrue(url.contains("Luna"))
  }

  @Test
  fun mapsStatusFromStorage() {
    assertEquals(AppointmentStatus.Confirmed, AppointmentStatus.fromStorage("confirmed"))
    assertEquals(AppointmentStatus.NoShow, AppointmentStatus.fromStorage("no_show"))
    assertEquals(AppointmentStatus.Scheduled, AppointmentStatus.fromStorage(null))
  }

  private fun appointment(startMinute: Int, endMinute: Int): Appointment =
    Appointment(
      id = "appointment-1",
      clinicId = "clinic-1",
      petId = "pet-1",
      petName = "Luna",
      ownerName = "Ana",
      ownerPhone = "11 5555-1234",
      startsAt = ClinicTime.instantAt(day, startMinute),
      endsAt = ClinicTime.instantAt(day, endMinute),
      reason = "Control",
      notes = null,
      status = AppointmentStatus.Scheduled,
      consultationId = null,
      cancelReason = null,
      createdAt = 0,
      updatedAt = 0,
      revision = 0,
      pendingSync = false,
    )
}
