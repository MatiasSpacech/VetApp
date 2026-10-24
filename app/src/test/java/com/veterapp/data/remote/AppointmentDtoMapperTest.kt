package com.veterapp.data.remote

import com.veterapp.data.local.AppointmentEntity
import com.veterapp.domain.AppointmentStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppointmentDtoMapperTest {
  @Test
  fun mapsEntityToDtoAndBack() {
    val entity =
      AppointmentEntity(
        id = "appointment-1",
        clinicId = "clinic-1",
        petId = "pet-1",
        startsAt = Instant.parse("2026-10-26T13:00:00Z").toEpochMilli(),
        endsAt = Instant.parse("2026-10-26T13:30:00Z").toEpochMilli(),
        status = AppointmentStatus.Confirmed.storage,
        reason = "Control",
        notes = "Sin novedades",
        consultationId = "consultation-1",
        cancelReason = null,
        createdAt = Instant.parse("2026-10-24T10:00:00Z").toEpochMilli(),
        updatedAt = Instant.parse("2026-10-24T10:00:00Z").toEpochMilli(),
        revision = 2,
        deletedAt = null,
        originDeviceId = "device-1",
        pendingSync = true,
      )

    val restored = entity.toDto().toEntity("fallback", "fallback-device")

    assertEquals(entity.id, restored.id)
    assertEquals(entity.clinicId, restored.clinicId)
    assertEquals(entity.petId, restored.petId)
    assertEquals(entity.startsAt, restored.startsAt)
    assertEquals(entity.endsAt, restored.endsAt)
    assertEquals(AppointmentStatus.Confirmed.storage, restored.status)
    assertEquals(entity.reason, restored.reason)
    assertEquals(entity.notes, restored.notes)
    assertEquals(entity.consultationId, restored.consultationId)
    assertEquals(2L, restored.revision)
    assertEquals("device-1", restored.originDeviceId)
    assertEquals(false, restored.pendingSync)
  }

  @Test
  fun usesFallbacksForMissingServerFields() {
    val dto =
      AppointmentDto(
        id = "appointment-1",
        petId = "pet-1",
        startsAt = "2026-10-26T13:00:00Z",
        endsAt = "2026-10-26T13:30:00Z",
        status = null,
        reason = "Control",
        createdAt = null,
        updatedAt = null,
        revision = null,
        originDeviceId = null,
      )

    val entity = dto.toEntity("clinic-fallback", "device-fallback")

    assertEquals("clinic-fallback", entity.clinicId)
    assertEquals("device-fallback", entity.originDeviceId)
    assertEquals(AppointmentStatus.Scheduled.storage, entity.status)
    assertEquals(1L, entity.revision)
    assertNull(entity.deletedAt)
  }
}
