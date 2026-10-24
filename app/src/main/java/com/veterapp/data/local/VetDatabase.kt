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
    ConsultationEntity::class,
    ConsultationAddendumEntity::class,
    VaccinationEntity::class,
    VaccinationNotificationEntity::class,
  ],
  version = 4,
  exportSchema = true,
)
abstract class VetDatabase : RoomDatabase() {
  abstract fun petDao(): PetDao

  abstract fun outboxDao(): OutboxDao

  abstract fun auditDao(): AuditDao

  abstract fun clientDao(): ClientDao

  abstract fun ownerHistoryDao(): PetOwnerHistoryDao

  abstract fun consultationDao(): ConsultationDao

  abstract fun addendumDao(): ConsultationAddendumDao

  abstract fun vaccinationDao(): VaccinationDao

  abstract fun vaccinationNotificationDao(): VaccinationNotificationDao

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

    val MIGRATION_3_4 =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `consultations` (" +
              "`id` TEXT NOT NULL, " +
              "`clinicId` TEXT NOT NULL, " +
              "`petId` TEXT NOT NULL, " +
              "`consultedAt` INTEGER NOT NULL, " +
              "`reason` TEXT NOT NULL, " +
              "`exam` TEXT, " +
              "`diagnosis` TEXT, " +
              "`treatment` TEXT, " +
              "`medication` TEXT, " +
              "`instructions` TEXT, " +
              "`createdAt` INTEGER NOT NULL, " +
              "`updatedAt` INTEGER NOT NULL, " +
              "`revision` INTEGER NOT NULL, " +
              "`deletedAt` INTEGER, " +
              "`originDeviceId` TEXT NOT NULL, " +
              "`pendingSync` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_consultations_clinicId` ON `consultations` (`clinicId`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_consultations_petId` ON `consultations` (`petId`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_consultations_consultedAt` ON `consultations` (`consultedAt`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_consultations_pendingSync` ON `consultations` (`pendingSync`)",
          )

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `consultation_addenda` (" +
              "`id` TEXT NOT NULL, " +
              "`clinicId` TEXT NOT NULL, " +
              "`consultationId` TEXT NOT NULL, " +
              "`reason` TEXT NOT NULL, " +
              "`text` TEXT NOT NULL, " +
              "`authorId` TEXT, " +
              "`createdAt` INTEGER NOT NULL, " +
              "`updatedAt` INTEGER NOT NULL, " +
              "`revision` INTEGER NOT NULL, " +
              "`deletedAt` INTEGER, " +
              "`originDeviceId` TEXT NOT NULL, " +
              "`pendingSync` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_consultation_addenda_clinicId` ON `consultation_addenda` (`clinicId`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_consultation_addenda_consultationId` ON `consultation_addenda` (`consultationId`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_consultation_addenda_createdAt` ON `consultation_addenda` (`createdAt`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_consultation_addenda_pendingSync` ON `consultation_addenda` (`pendingSync`)",
          )

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `vaccinations` (" +
              "`id` TEXT NOT NULL, " +
              "`clinicId` TEXT NOT NULL, " +
              "`petId` TEXT NOT NULL, " +
              "`vaccineName` TEXT NOT NULL, " +
              "`appliedOn` INTEGER NOT NULL, " +
              "`nextDueOn` INTEGER, " +
              "`lot` TEXT, " +
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
            "CREATE INDEX IF NOT EXISTS `index_vaccinations_clinicId` ON `vaccinations` (`clinicId`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_vaccinations_petId` ON `vaccinations` (`petId`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_vaccinations_appliedOn` ON `vaccinations` (`appliedOn`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_vaccinations_nextDueOn` ON `vaccinations` (`nextDueOn`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_vaccinations_pendingSync` ON `vaccinations` (`pendingSync`)",
          )

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `vaccination_notifications` (" +
              "`id` TEXT NOT NULL, " +
              "`clinicId` TEXT NOT NULL, " +
              "`vaccinationId` TEXT NOT NULL, " +
              "`petId` TEXT NOT NULL, " +
              "`dueOn` INTEGER NOT NULL, " +
              "`milestone` TEXT NOT NULL, " +
              "`notifiedAt` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
              "`index_vaccination_notifications_vaccinationId_dueOn_milestone` " +
              "ON `vaccination_notifications` (`vaccinationId`, `dueOn`, `milestone`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_vaccination_notifications_notifiedAt` ON `vaccination_notifications` (`notifiedAt`)",
          )
        }
      }

    fun build(context: Context): VetDatabase =
      Room.databaseBuilder(context, VetDatabase::class.java, "veterapp.db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .build()
  }
}
