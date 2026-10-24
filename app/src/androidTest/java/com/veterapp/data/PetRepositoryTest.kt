package com.veterapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veterapp.data.local.VetDatabase
import com.veterapp.domain.ClientDraft
import com.veterapp.domain.ClientSaveResult
import com.veterapp.domain.PetDraft
import com.veterapp.domain.PetStatus
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PetRepositoryTest {
  @Test
  fun createsPetWithOwnerAndRecordsOwnerChanges() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val database = Room.inMemoryDatabaseBuilder(context, VetDatabase::class.java).build()
      val settings = AppSettings(context)
      settings.clinicId = "local-clinic"
      settings.userId = null
      val audit = AuditRepository(database, settings)
      val clients = ClientRepository(database, settings, audit)
      val repository = PetRepository(database, settings, audit)
      val petId = UUID.randomUUID().toString()

      val ana =
        (clients.create(
          ClientDraft(
            name = "Ana",
            phone = "11 5555-1234",
            document = "30.111.222",
            email = null,
            address = null,
            notes = null,
          ),
        ) as ClientSaveResult.Success).client
      val leo =
        (clients.create(
          ClientDraft(
            name = "Leo",
            phone = "11 4444-0000",
            document = null,
            email = null,
            address = null,
            notes = null,
          ),
        ) as ClientSaveResult.Success).client

      repository.createPet(
        clientId = ana.id,
        petId = petId,
        draft =
          PetDraft(
            name = "Luna",
            species = "Perro",
            breed = "Mestiza",
            notes = "Control anual",
            status = PetStatus.Active,
          ),
        fotoUrl = null,
      )

      val pets = repository.pets.first()
      assertEquals(1, pets.size)
      assertEquals(petId, pets.first().id)
      assertEquals("Luna", pets.first().name)
      assertEquals(ana.id, pets.first().clientId)
      assertEquals("Ana", pets.first().ownerName)
      assertEquals(PetStatus.Active, pets.first().status)
      assertTrue(pets.first().pendingSync)
      assertNull(pets.first().fotoUrl)
      assertEquals(3, repository.pendingCount.first())
      assertEquals(3, database.outboxDao().observeCount().first())
      assertEquals(1, database.auditDao().all().count { it.action == "pet.create" })

      repository.updatePet(
        petId = petId,
        draft =
          PetDraft(
            name = "Luna actualizada",
            species = "Perro",
            breed = "Mestiza",
            notes = "Control anual",
            status = PetStatus.Inactive,
          ),
      )
      val updated = repository.pets.first().single()
      assertEquals("Luna actualizada", updated.name)
      assertEquals(PetStatus.Inactive, updated.status)
      assertTrue(updated.pendingSync)
      assertEquals(4, database.outboxDao().observeCount().first())

      repository.changeOwner(petId, leo.id, "Cambio de familia")
      val moved = repository.pets.first().single()
      assertEquals(leo.id, moved.clientId)
      assertEquals("Leo", moved.ownerName)
      val history = repository.observeOwnerHistory(petId).first()
      assertEquals(1, history.size)
      assertEquals("Ana", history.first().previousOwnerName)
      assertEquals("Leo", history.first().newOwnerName)
      assertEquals("Cambio de familia", history.first().reason)
      assertEquals(1, database.auditDao().all().count { it.action == "pet.owner.change" })

      repository.softDelete(petId)
      assertTrue(repository.pets.first().isEmpty())
      assertEquals(6, database.outboxDao().observeCount().first())

      database.close()
    }
  }
}
