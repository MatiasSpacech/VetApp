package com.veterapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veterapp.data.local.VetDatabase
import com.veterapp.domain.ClientDraft
import com.veterapp.domain.ClientSaveResult
import com.veterapp.domain.PetDraft
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClientRepositoryTest {
  @Test
  fun createsFindsDuplicatesAndArchives() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val database = Room.inMemoryDatabaseBuilder(context, VetDatabase::class.java).build()
      val settings = AppSettings(context)
      settings.clinicId = "local-clinic"
      settings.userId = null
      val audit = AuditRepository(database, settings)
      val clients = ClientRepository(database, settings, audit)
      val pets = PetRepository(database, settings, audit)

      val ana =
        (clients.create(
          ClientDraft(
            name = "Ana",
            phone = "11 5555-1234",
            document = "30.111.222",
            email = "ana@example.com",
            address = null,
            notes = null,
          ),
        ) as ClientSaveResult.Success).client

      assertEquals(1, clients.clients.first().size)
      assertEquals(1, database.auditDao().all().count { it.action == "client.create" })
      assertEquals(1, database.outboxDao().observeCount().first())

      val duplicate =
        clients.create(
          ClientDraft(
            name = "Ana duplicada",
            phone = "1155551234",
            document = null,
            email = null,
            address = null,
            notes = null,
          ),
        )
      assertTrue(duplicate is ClientSaveResult.Duplicate)
      assertEquals(1, (duplicate as ClientSaveResult.Duplicate).matches.size)

      val duplicateByDocument =
        clients.create(
          ClientDraft(
            name = "Ana por documento",
            phone = null,
            document = "30111222",
            email = null,
            address = null,
            notes = null,
          ),
        )
      assertTrue(duplicateByDocument is ClientSaveResult.Duplicate)

      val forced =
        clients.create(
          ClientDraft(
            name = "Ana duplicada",
            phone = "1155551234",
            document = null,
            email = null,
            address = null,
            notes = null,
          ),
          force = true,
        )
      assertTrue(forced is ClientSaveResult.Success)

      val updated =
        clients.update(
          ana.id,
          ClientDraft(
            name = "Ana Pérez",
            phone = "11 5555-9999",
            document = "30.111.222",
            email = null,
            address = "Calle 1",
            notes = null,
          ),
        )
      assertTrue(updated is ClientSaveResult.Success)
      assertEquals("Ana Pérez", clients.clients.first().first { it.id == ana.id }.name)

      pets.createPet(
        clientId = ana.id,
        petId = UUID.randomUUID().toString(),
        draft =
          PetDraft(
            name = "Luna",
            species = "Perro",
            breed = null,
            notes = null,
          ),
        fotoUrl = null,
      )

      val blocked = clients.archive(ana.id)
      assertTrue(blocked.isFailure)
      assertTrue(blocked.exceptionOrNull()?.message.orEmpty().contains("mascota"))

      pets.softDelete(pets.pets.first().first().id)
      val archived = clients.archive(ana.id)
      assertTrue(archived.isSuccess)
      assertTrue(clients.clients.first().none { it.id == ana.id })

      database.close()
    }
  }
}
