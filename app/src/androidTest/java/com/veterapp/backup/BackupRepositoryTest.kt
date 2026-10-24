package com.veterapp.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veterapp.data.AppSettings
import com.veterapp.data.AuditRepository
import com.veterapp.data.PetRepository
import com.veterapp.data.local.VetDatabase
import com.veterapp.domain.PetDraft
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
  @Test
  fun exportsEncryptedBackupAndRestoresIt() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val database = Room.inMemoryDatabaseBuilder(context, VetDatabase::class.java).build()
      val settings = AppSettings(context)
      settings.clinicId = "local-clinic"
      settings.userId = null
      val audit = AuditRepository(database, settings)
      val pets = PetRepository(database, settings, audit)
      val backup = BackupRepository(context, database, settings, audit)
      val file = File(context.cacheDir, "backup-test.vbackup")
      val passphrase = "backup123"

      pets.createPet(
        clinicId = "local-clinic",
        petId = UUID.randomUUID().toString(),
        draft =
          PetDraft(
            name = "Luna",
            species = "Perro",
            breed = "Mestiza",
            ownerName = "Ana",
            notes = null,
          ),
        fotoUrl = null,
      )

      assertEquals(1, database.auditDao().all().count { it.action == "pet.create" })

      val exported = backup.export(passphrase.toCharArray(), Uri.fromFile(file))
      assertTrue(exported.isSuccess)
      assertEquals(1, exported.getOrThrow())
      assertEquals("VBK1", file.readBytes().copyOfRange(0, 4).decodeToString())
      assertTrue(!file.readBytes().decodeToString().contains("Luna"))

      database.petDao().clear()
      database.outboxDao().clear()
      database.auditDao().clear()
      assertEquals(0, pets.pets.first().size)

      val restored = backup.restore(passphrase.toCharArray(), Uri.fromFile(file))
      assertTrue(restored.isSuccess)
      assertEquals(1, restored.getOrThrow())
      assertEquals(1, pets.pets.first().size)
      assertEquals("Luna", pets.pets.first().first().name)
      assertEquals(1, database.outboxDao().observeCount().first())
      assertTrue(database.auditDao().all().any { it.action == "pet.create" })
      assertTrue(database.auditDao().all().any { it.action == "backup.restore" })

      database.close()
      file.delete()
    }
  }
}
