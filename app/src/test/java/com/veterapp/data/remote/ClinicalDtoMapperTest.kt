package com.veterapp.data.remote

import com.veterapp.data.local.ConsultationAddendumEntity
import com.veterapp.data.local.ConsultationEntity
import com.veterapp.data.local.VaccinationEntity
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ClinicalDtoMapperTest {
  private val instant = Instant.parse("2026-10-24T15:30:00Z").toEpochMilli()

  @Test
  fun mapsConsultationRoundTrip() {
    val entity =
      ConsultationEntity(
        id = "consultation-1",
        clinicId = "clinic-1",
        petId = "pet-1",
        consultedAt = instant,
        reason = "Control",
        exam = "Normal",
        diagnosis = "Sano",
        treatment = null,
        medication = null,
        instructions = "Volver en 6 meses",
        createdAt = instant,
        updatedAt = instant,
        revision = 3,
        deletedAt = null,
        originDeviceId = "device-1",
        pendingSync = true,
      )

    val dto = entity.toDto()
    assertEquals("pet-1", dto.petId)
    assertEquals(Instant.ofEpochMilli(instant).toString(), dto.consultedAt)

    val mapped = dto.toEntity("fallback-clinic", "fallback-device")
    assertEquals(entity.copy(pendingSync = false), mapped)
    assertFalse(mapped.pendingSync)
  }

  @Test
  fun mapsAddendumRoundTrip() {
    val entity =
      ConsultationAddendumEntity(
        id = "addendum-1",
        clinicId = "clinic-1",
        consultationId = "consultation-1",
        reason = "Corrección",
        text = "Se ajusta el diagnóstico",
        authorId = "user-1",
        createdAt = instant,
        updatedAt = instant,
        revision = 2,
        deletedAt = null,
        originDeviceId = "device-1",
        pendingSync = true,
      )

    val mapped = entity.toDto().toEntity("fallback-clinic", "fallback-device")
    assertEquals(entity.copy(pendingSync = false), mapped)
    assertFalse(mapped.pendingSync)
  }

  @Test
  fun mapsVaccinationDates() {
    val entity =
      VaccinationEntity(
        id = "vaccination-1",
        clinicId = "clinic-1",
        petId = "pet-1",
        vaccineName = "Rabia",
        appliedOn = LocalDate.of(2026, 10, 24).toEpochDay(),
        nextDueOn = LocalDate.of(2027, 10, 24).toEpochDay(),
        lot = "L-123",
        notes = null,
        createdAt = instant,
        updatedAt = instant,
        revision = 1,
        deletedAt = null,
        originDeviceId = "device-1",
        pendingSync = true,
      )

    val dto = entity.toDto()
    assertEquals("2026-10-24", dto.appliedOn)
    assertEquals("2027-10-24", dto.nextDueOn)

    val mapped = dto.toEntity("fallback-clinic", "fallback-device")
    assertEquals(entity.copy(pendingSync = false), mapped)
    assertFalse(mapped.pendingSync)
  }

  @Test
  fun handlesMissingVaccinationNextDue() {
    val dto =
      VaccinationDto(
        id = "vaccination-1",
        petId = "pet-1",
        vaccineName = "Rabia",
        appliedOn = "2026-10-24",
        nextDueOn = null,
      )

    val mapped = dto.toEntity("clinic-1", "device-1")
    assertNull(mapped.nextDueOn)
    assertEquals("clinic-1", mapped.clinicId)
    assertEquals("device-1", mapped.originDeviceId)
  }
}
