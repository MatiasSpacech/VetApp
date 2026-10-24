package com.veterapp.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VetDatabaseMigrationTest {
  @get:Rule
  val helper =
    MigrationTestHelper(
      InstrumentationRegistry.getInstrumentation(),
      VetDatabase::class.java,
    )

  @After
  fun tearDown() {
    InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DATABASE_NAME)
  }

  @Test
  fun migratesFromVersion1To2() {
    helper.createDatabase(DATABASE_NAME, 1).use { database ->
      database.execSQL(
        "INSERT INTO pets (" +
          "id, clinicId, name, species, breed, ownerName, notes, fotoUrl, " +
          "createdAt, updatedAt, revision, deletedAt, originDeviceId, pendingSync" +
          ") VALUES (" +
          "'pet-1', 'clinic-1', 'Luna', 'Perro', NULL, 'Ana', NULL, NULL, " +
          "1, 1, 0, NULL, 'device-1', 0" +
          ")",
      )
    }

    helper.runMigrationsAndValidate(DATABASE_NAME, 2, true, VetDatabase.MIGRATION_1_2).use { database ->
      database.query("SELECT COUNT(*) FROM audit_log").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
      }
      database.query("SELECT name FROM pets").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals("Luna", cursor.getString(0))
      }
    }
  }

  @Test
  fun migratesFromVersion2To3() {
    helper.createDatabase(DATABASE_NAME, 2).use { database ->
      database.execSQL(
        "INSERT INTO pets (" +
          "id, clinicId, name, species, breed, ownerName, notes, fotoUrl, " +
          "createdAt, updatedAt, revision, deletedAt, originDeviceId, pendingSync" +
          ") VALUES (" +
          "'pet-1', 'clinic-1', 'Luna', 'Perro', NULL, 'Ana', NULL, NULL, " +
          "1, 1, 0, NULL, 'device-1', 0" +
          ")",
      )
    }

    helper.runMigrationsAndValidate(DATABASE_NAME, 3, true, VetDatabase.MIGRATION_2_3).use { database ->
      database.query("SELECT status, clientId FROM pets").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals("active", cursor.getString(0))
        assertTrue(cursor.isNull(1))
      }
      database.query("SELECT COUNT(*) FROM clients").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
      }
      database.query("SELECT COUNT(*) FROM pet_owner_history").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
      }
    }
  }

  @Test
  fun migratesFromVersion3To4() {
    helper.createDatabase(DATABASE_NAME, 3).use { database ->
      database.execSQL(
        "INSERT INTO pets (" +
          "id, clinicId, name, species, breed, ownerName, notes, fotoUrl, " +
          "createdAt, updatedAt, revision, deletedAt, originDeviceId, pendingSync, clientId, status" +
          ") VALUES (" +
          "'pet-1', 'clinic-1', 'Luna', 'Perro', NULL, 'Ana', NULL, NULL, " +
          "1, 1, 0, NULL, 'device-1', 0, NULL, 'active'" +
          ")",
      )
    }

    helper.runMigrationsAndValidate(DATABASE_NAME, 4, true, VetDatabase.MIGRATION_3_4).use { database ->
      database.query("SELECT COUNT(*) FROM consultations").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
      }
      database.query("SELECT COUNT(*) FROM consultation_addenda").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
      }
      database.query("SELECT COUNT(*) FROM vaccinations").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
      }
      database.query("SELECT COUNT(*) FROM vaccination_notifications").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
      }
      database.query("SELECT name FROM pets").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals("Luna", cursor.getString(0))
      }
    }
  }

  @Test
  fun migratesFromVersion4To5() {
    helper.createDatabase(DATABASE_NAME, 4).use { database ->
      database.execSQL(
        "INSERT INTO pets (" +
          "id, clinicId, name, species, breed, ownerName, notes, fotoUrl, " +
          "createdAt, updatedAt, revision, deletedAt, originDeviceId, pendingSync, clientId, status" +
          ") VALUES (" +
          "'pet-1', 'clinic-1', 'Luna', 'Perro', NULL, 'Ana', NULL, NULL, " +
          "1, 1, 0, NULL, 'device-1', 0, NULL, 'active'" +
          ")",
      )
    }

    helper.runMigrationsAndValidate(DATABASE_NAME, 5, true, VetDatabase.MIGRATION_4_5).use { database ->
      database.query("SELECT COUNT(*) FROM appointments").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
      }
      database.query("SELECT name FROM pets").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals("Luna", cursor.getString(0))
      }
    }
  }

  @Test
  fun migratesFromVersion5To8() {
    helper.createDatabase(DATABASE_NAME, 5).use { database ->
      database.execSQL(
        "INSERT INTO pets (" +
          "id, clinicId, name, species, breed, ownerName, notes, fotoUrl, " +
          "createdAt, updatedAt, revision, deletedAt, originDeviceId, pendingSync, clientId, status" +
          ") VALUES (" +
          "'pet-1', 'clinic-1', 'Luna', 'Perro', NULL, 'Ana', NULL, NULL, " +
          "1, 1, 0, NULL, 'device-1', 0, NULL, 'active'" +
          ")",
      )
    }

    helper
      .runMigrationsAndValidate(
        DATABASE_NAME,
        8,
        true,
        VetDatabase.MIGRATION_5_6,
        VetDatabase.MIGRATION_6_7,
        VetDatabase.MIGRATION_7_8,
      )
      .use { database ->
        database.query("SELECT sex, birthDate, approxAgeMonths FROM pets").use { cursor ->
          assertTrue(cursor.moveToFirst())
          assertTrue(cursor.isNull(0))
          assertTrue(cursor.isNull(1))
          assertTrue(cursor.isNull(2))
        }
        listOf(
          "pet_weights",
          "areas",
          "categories",
          "catalog_items",
          "stock_movements",
          "payment_methods",
          "sales",
          "sale_items",
          "payments",
          "cash_sessions",
          "cash_movements",
          "sale_cancellations",
          "sale_counters",
          "area_cost_settings",
        ).forEach { table ->
          database.query("SELECT COUNT(*) FROM $table").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
          }
        }
      }
  }
  private companion object {
    const val DATABASE_NAME = "migration-test.db"
  }
}
