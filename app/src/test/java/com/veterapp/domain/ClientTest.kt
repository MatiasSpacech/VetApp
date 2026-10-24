package com.veterapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientTest {
  private val client =
    Client(
      id = "client-1",
      clinicId = "clinic-1",
      name = "Ana Pérez",
      phone = "11 5555-1234",
      document = "30.111.222",
      email = null,
      address = null,
      notes = null,
      createdAt = 1,
      updatedAt = 1,
      revision = 0,
      pendingSync = false,
    )

  @Test
  fun normalizesContactData() {
    assertEquals("1155551234", normalizePhone("11 5555-1234"))
    assertEquals("30111222", normalizeDocument("30.111.222"))
    assertNull(normalizePhone("---"))
    assertNull(normalizeDocument(" "))
  }

  @Test
  fun detectsDuplicatesByPhoneOrDocument() {
    assertTrue(client.matches(draft(phone = "1155551234", document = null)))
    assertTrue(client.matches(draft(phone = null, document = "30111222")))
    assertFalse(client.matches(draft(phone = "1100000000", document = "99999999")))
    assertFalse(client.matches(draft(phone = null, document = null)))
  }

  @Test
  fun buildsInitials() {
    assertEquals("AP", client.initials)
    assertEquals("L", client.copy(name = "Luna").initials)
    assertEquals("?", client.copy(name = " ").initials)
  }

  @Test
  fun mapsPetStatus() {
    assertEquals(PetStatus.Inactive, PetStatus.fromStorage("inactive"))
    assertEquals(PetStatus.Deceased, PetStatus.fromStorage("deceased"))
    assertEquals(PetStatus.Active, PetStatus.fromStorage("unknown"))
    assertEquals(PetStatus.Active, PetStatus.fromStorage(null))
  }

  private fun draft(phone: String?, document: String?): ClientDraft =
    ClientDraft(
      name = "Otra",
      phone = phone,
      document = document,
      email = null,
      address = null,
      notes = null,
    )
}
