package com.veterapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [PetEntity::class, OutboxEntity::class, AuditEntity::class],
  version = 2,
  exportSchema = true,
)
abstract class VetDatabase : RoomDatabase() {
  abstract fun petDao(): PetDao

  abstract fun outboxDao(): OutboxDao

  abstract fun auditDao(): AuditDao

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

    fun build(context: Context): VetDatabase =
      Room.databaseBuilder(context, VetDatabase::class.java, "veterapp.db")
        .addMigrations(MIGRATION_1_2)
        .build()
  }
}
