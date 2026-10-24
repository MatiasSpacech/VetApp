package com.veterapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veterapp.data.local.VetDatabase
import com.veterapp.domain.PetDraft
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
  fun createsPetAndOutboxInOneTransaction() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val database = Room.inMemoryDatabaseBuilder(context, VetDatabase::class.java).build()
      val repository = PetRepository(database, AppSettings(context))
      val petId = UUID.randomUUID().toString()

      repository.createPet(
        clinicId = "local-clinic",
        petId = petId,
        draft =
          PetDraft(
            name = "Luna",
            species = "Perro",
            breed = "Mestiza",
            ownerName = "Ana",
            notes = "Control anual",
          ),
        fotoUrl = null,
      )

      val pets = repository.pets.first()
      assertEquals(1, pets.size)
      assertEquals(petId, pets.first().id)
      assertEquals("Luna", pets.first().name)
      assertTrue(pets.first().pendingSync)
      assertNull(pets.first().fotoUrl)
      assertEquals(1, repository.pendingCount.first())
      assertEquals(1, database.outboxDao().observeCount().first())

      repository.updatePet(
        petId = petId,
        draft =
          PetDraft(
            name = "Luna actualizada",
            species = "Perro",
            breed = "Mestiza",
            ownerName = "Ana",
            notes = "Control anual",
          ),
      )
      val updated = repository.pets.first().single()
      assertEquals("Luna actualizada", updated.name)
      assertTrue(updated.pendingSync)
      assertEquals(2, database.outboxDao().observeCount().first())

      repository.softDelete(petId)
      assertTrue(repository.pets.first().isEmpty())
      assertEquals(3, database.outboxDao().observeCount().first())

      database.close()
    }
  }
}
