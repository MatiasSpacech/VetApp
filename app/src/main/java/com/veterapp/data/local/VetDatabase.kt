package com.veterapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    PetEntity::class,
    OutboxEntity::class,
    AuditEntity::class,
    ClientEntity::class,
    PetOwnerHistoryEntity::class,
  ],
  version = 3,
  exportSchema = true,
)
abstract class VetDatabase : RoomDatabase() {
  abstract fun petDao(): PetDao

  abstract fun outboxDao(): OutboxDao

  abstract fun auditDao(): AuditDao

  abstract fun clientDao(): ClientDao

  abstract fun ownerHistoryDao(): PetOwnerHistoryDao

  companion object {
    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `audit_log` (" +
              "`id` TEXT NOT NULL, " +
              "`clinicId` TEXT NOT NULL, " +
              "`actorId` TEXT, " +
              "`entity` TEXT NOT NULL, " +
              "`entityId` TEXT, " +
              "`action` TEXT NOT NULL, " +
              "`payload` TEXT, " +
              "`createdAt` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_audit_log_createdAt` ON `audit_log` (`createdAt`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_audit_log_entity_entityId` ON `audit_log` (`entity`, `entityId`)",
          )
        }
      }

    val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `clients` (" +
              "`id` TEXT NOT NULL, " +
              "`clinicId` TEXT NOT NULL, " +
              "`name` TEXT NOT NULL, " +
              "`phone` TEXT, " +
              "`document` TEXT, " +
              "`email` TEXT, " +
              "`address` TEXT, " +
              "`notes` TEXT, " +
              "`createdAt` INTEGER NOT NULL, " +
              "`updatedAt` INTEGER NOT NULL, " +
              "`revision` INTEGER NOT NULL, " +
              "`deletedAt` INTEGER, " +
              "`originDeviceId` TEXT NOT NULL, " +
              "`pendingSync` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_clients_clinicId` ON `clients` (`clinicId`)",
          )
          db.execSQL("CREATE INDEX IF NOT EXISTS `index_clients_name` ON `clients` (`name`)")
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_clients_updatedAt` ON `clients` (`updatedAt`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_clients_pendingSync` ON `clients` (`pendingSync`)",
          )

          db.execSQL("ALTER TABLE `pets` ADD COLUMN `clientId` TEXT")
          db.execSQL("ALTER TABLE `pets` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'active'")
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pets_clientId` ON `pets` (`clientId`)",
          )

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pet_owner_history` (" +
              "`id` TEXT NOT NULL, " +
              "`clinicId` TEXT NOT NULL, " +
              "`petId` TEXT NOT NULL, " +
              "`previousClientId` TEXT, " +
              "`previousOwnerName` TEXT NOT NULL, " +
              "`newClientId` TEXT NOT NULL, " +
              "`newOwnerName` TEXT NOT NULL, " +
              "`reason` TEXT, " +
              "`actorId` TEXT, " +
              "`changedAt` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pet_owner_history_clinicId` ON `pet_owner_history` (`clinicId`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pet_owner_history_petId` ON `pet_owner_history` (`petId`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pet_owner_history_changedAt` ON `pet_owner_history` (`changedAt`)",
          )
        }
      }

    fun build(context: Context): VetDatabase =
      Room.databaseBuilder(context, VetDatabase::class.java, "veterapp.db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
  }
}
