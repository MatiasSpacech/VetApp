package com.veterapp.data.remote

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetDtoMapperTest {
  @Test
  fun mapsServerTimestampsAndDeviceFallback() {
    val dto =
      PetDto(
        id = "pet-1",
        clinicId = "clinic-1",
        name = "Luna",
        species = "Perro",
        breed = "Mestiza",
        ownerName = "Ana",
        fotoUrl = "https://example.com/luna.webp",
        createdAt = "2026-10-23T12:00:00Z",
        updatedAt = "2026-10-23T12:30:00Z",
        revision = 4,
        originDeviceId = null,
      )

    val entity = dto.toEntity(fallbackClinicId = "fallback", fallbackDeviceId = "device-1")

    assertEquals("pet-1", entity.id)
    assertEquals("clinic-1", entity.clinicId)
    assertEquals("Luna", entity.name)
    assertEquals("Mestiza", entity.breed)
    assertEquals("Ana", entity.ownerName)
    assertEquals("https://example.com/luna.webp", entity.fotoUrl)
    assertEquals(Instant.parse("2026-10-23T12:00:00Z").toEpochMilli(), entity.createdAt)
    assertEquals(Instant.parse("2026-10-23T12:30:00Z").toEpochMilli(), entity.updatedAt)
    assertEquals(4L, entity.revision)
    assertEquals("device-1", entity.originDeviceId)
    assertNull(entity.deletedAt)
    assertEquals(false, entity.pendingSync)
  }

  @Test
  fun usesFallbackClinicWhenServerOmitsIt() {
    val dto =
      PetDto(
        id = "pet-2",
        name = "Michi",
        species = "Gato",
        ownerName = "Leo",
      )

    val entity = dto.toEntity(fallbackClinicId = "local-clinic", fallbackDeviceId = "device-2")

    assertEquals("local-clinic", entity.clinicId)
    assertEquals("device-2", entity.originDeviceId)
    assertEquals(1L, entity.revision)
  }
}
