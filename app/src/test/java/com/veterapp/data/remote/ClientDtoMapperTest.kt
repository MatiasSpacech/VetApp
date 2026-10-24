package com.veterapp.data.remote

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClientDtoMapperTest {
  @Test
  fun mapsServerTimestampsAndDeviceFallback() {
    val dto =
      ClientDto(
        id = "client-1",
        clinicId = "clinic-1",
        name = "Ana",
        phone = "11 5555-1234",
        document = "30.111.222",
        email = "ana@example.com",
        address = "Calle 1",
        notes = "Vecina",
        createdAt = "2026-10-24T12:00:00Z",
        updatedAt = "2026-10-24T12:30:00Z",
        revision = 3,
        originDeviceId = null,
      )

    val entity = dto.toEntity(fallbackClinicId = "fallback", fallbackDeviceId = "device-1")

    assertEquals("client-1", entity.id)
    assertEquals("clinic-1", entity.clinicId)
    assertEquals("Ana", entity.name)
    assertEquals("11 5555-1234", entity.phone)
    assertEquals("30.111.222", entity.document)
    assertEquals("ana@example.com", entity.email)
    assertEquals("Calle 1", entity.address)
    assertEquals("Vecina", entity.notes)
    assertEquals(Instant.parse("2026-10-24T12:00:00Z").toEpochMilli(), entity.createdAt)
    assertEquals(Instant.parse("2026-10-24T12:30:00Z").toEpochMilli(), entity.updatedAt)
    assertEquals(3L, entity.revision)
    assertEquals("device-1", entity.originDeviceId)
    assertNull(entity.deletedAt)
    assertEquals(false, entity.pendingSync)
  }

  @Test
  fun usesFallbackClinicWhenServerOmitsIt() {
    val dto =
      ClientDto(
        id = "client-2",
        name = "Leo",
      )

    val entity = dto.toEntity(fallbackClinicId = "local-clinic", fallbackDeviceId = "device-2")

    assertEquals("local-clinic", entity.clinicId)
    assertEquals("device-2", entity.originDeviceId)
    assertEquals(1L, entity.revision)
  }
}
