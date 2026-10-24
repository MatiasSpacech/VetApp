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
    AppointmentEntity::class,
    PetWeightEntity::class,
    AreaEntity::class,
    CategoryEntity::class,
    CatalogItemEntity::class,
    StockMovementEntity::class,
    PaymentMethodEntity::class,
    SaleEntity::class,
    SaleItemEntity::class,
    PaymentEntity::class,
    CashSessionEntity::class,
    CashMovementEntity::class,
    SaleCancellationEntity::class,
    SaleCounterEntity::class,
    AreaCostSettingEntity::class,
  ],
  version = 8,
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

  abstract fun appointmentDao(): AppointmentDao

  abstract fun petWeightDao(): PetWeightDao

  abstract fun areaDao(): AreaDao

  abstract fun categoryDao(): CategoryDao

  abstract fun catalogItemDao(): CatalogItemDao

  abstract fun stockMovementDao(): StockMovementDao

  abstract fun paymentMethodDao(): PaymentMethodDao

  abstract fun saleDao(): SaleDao

  abstract fun saleItemDao(): SaleItemDao

  abstract fun paymentDao(): PaymentDao

  abstract fun cashSessionDao(): CashSessionDao

  abstract fun cashMovementDao(): CashMovementDao

  abstract fun saleCancellationDao(): SaleCancellationDao

  abstract fun saleCounterDao(): SaleCounterDao

  abstract fun areaCostSettingDao(): AreaCostSettingDao

  abstract fun reportDao(): ReportDao

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

    val MIGRATION_4_5 =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `appointments` (" +
              "`id` TEXT NOT NULL, " +
              "`clinicId` TEXT NOT NULL, " +
              "`petId` TEXT NOT NULL, " +
              "`startsAt` INTEGER NOT NULL, " +
              "`endsAt` INTEGER NOT NULL, " +
              "`status` TEXT NOT NULL, " +
              "`reason` TEXT NOT NULL, " +
              "`notes` TEXT, " +
              "`consultationId` TEXT, " +
              "`cancelReason` TEXT, " +
              "`createdAt` INTEGER NOT NULL, " +
              "`updatedAt` INTEGER NOT NULL, " +
              "`revision` INTEGER NOT NULL, " +
              "`deletedAt` INTEGER, " +
              "`originDeviceId` TEXT NOT NULL, " +
              "`pendingSync` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_appointments_clinicId` ON `appointments` (`clinicId`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_appointments_petId` ON `appointments` (`petId`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_appointments_startsAt` ON `appointments` (`startsAt`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_appointments_status` ON `appointments` (`status`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_appointments_pendingSync` ON `appointments` (`pendingSync`)",
          )
        }
      }

    val MIGRATION_5_6 =
      object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE `pets` ADD COLUMN `sex` TEXT")
          db.execSQL("ALTER TABLE `pets` ADD COLUMN `birthDate` INTEGER")
          db.execSQL("ALTER TABLE `pets` ADD COLUMN `approxAgeMonths` INTEGER")
          db.execSQL("ALTER TABLE `pets` ADD COLUMN `color` TEXT")
          db.execSQL("ALTER TABLE `pets` ADD COLUMN `markings` TEXT")
          db.execSQL("ALTER TABLE `pets` ADD COLUMN `microchip` TEXT")
          db.execSQL("ALTER TABLE `pets` ADD COLUMN `reproductiveStatus` TEXT")
          db.execSQL("ALTER TABLE `pets` ADD COLUMN `allergies` TEXT")
          db.execSQL("ALTER TABLE `pets` ADD COLUMN `medicalNotes` TEXT")

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pet_weights` (" +
              "`id` TEXT NOT NULL, " +
              "`clinicId` TEXT NOT NULL, " +
              "`petId` TEXT NOT NULL, " +
              "`measuredAt` INTEGER NOT NULL, " +
              "`weightGrams` INTEGER NOT NULL, " +
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
            "CREATE INDEX IF NOT EXISTS `index_pet_weights_clinicId` ON `pet_weights` (`clinicId`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pet_weights_petId` ON `pet_weights` (`petId`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pet_weights_measuredAt` ON `pet_weights` (`measuredAt`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pet_weights_pendingSync` ON `pet_weights` (`pendingSync`)",
          )
        }
      }

    val MIGRATION_6_7 =
      object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `areas` (" +
              "`id` TEXT NOT NULL, `clinicId` TEXT NOT NULL, `name` TEXT NOT NULL, " +
              "`active` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, " +
              "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `revision` INTEGER NOT NULL, " +
              "`deletedAt` INTEGER, `originDeviceId` TEXT NOT NULL, `pendingSync` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          db.execSQL("CREATE INDEX IF NOT EXISTS `index_areas_clinicId` ON `areas` (`clinicId`)")
          db.execSQL("CREATE INDEX IF NOT EXISTS `index_areas_pendingSync` ON `areas` (`pendingSync`)")

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `categories` (" +
              "`id` TEXT NOT NULL, `clinicId` TEXT NOT NULL, `name` TEXT NOT NULL, `parentId` TEXT, " +
              "`scope` TEXT NOT NULL, `active` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, " +
              "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `revision` INTEGER NOT NULL, " +
              "`deletedAt` INTEGER, `originDeviceId` TEXT NOT NULL, `pendingSync` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_clinicId` ON `categories` (`clinicId`)")
          db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_parentId` ON `categories` (`parentId`)")
          db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_pendingSync` ON `categories` (`pendingSync`)")

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `catalog_items` (" +
              "`id` TEXT NOT NULL, `clinicId` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
              "`internalCode` TEXT NOT NULL, `barcode` TEXT, `name` TEXT NOT NULL, " +
              "`description` TEXT, `categoryId` TEXT, `brand` TEXT, `unitCostCents` INTEGER, " +
              "`salePriceCents` INTEGER NOT NULL, `trackStock` INTEGER NOT NULL, " +
              "`stockCached` INTEGER NOT NULL, `stockMin` INTEGER NOT NULL, `areaId` TEXT NOT NULL, " +
              "`lot` TEXT, `expiresOn` TEXT, `durationMinutes` INTEGER, `active` INTEGER NOT NULL, " +
              "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `revision` INTEGER NOT NULL, " +
              "`deletedAt` INTEGER, `originDeviceId` TEXT NOT NULL, `pendingSync` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          listOf(
            "clinicId",
            "kind",
            "name",
            "categoryId",
            "areaId",
            "internalCode",
            "barcode",
            "active",
            "pendingSync",
          ).forEach { column ->
            db.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_catalog_items_$column` " +
                "ON `catalog_items` (`$column`)",
            )
          }
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_catalog_items_clinicId_internalCode` " +
              "ON `catalog_items` (`clinicId`, `internalCode`)",
          )
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_catalog_items_clinicId_barcode` " +
              "ON `catalog_items` (`clinicId`, `barcode`)",
          )

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `stock_movements` (" +
              "`id` TEXT NOT NULL, `clinicId` TEXT NOT NULL, `itemId` TEXT NOT NULL, " +
              "`type` TEXT NOT NULL, `quantityDelta` INTEGER NOT NULL, `unitCostCents` INTEGER, " +
              "`reason` TEXT, `saleId` TEXT, `saleItemId` TEXT, `saleCancellationId` TEXT, " +
              "`createdAt` INTEGER NOT NULL, `originDeviceId` TEXT NOT NULL, `pendingSync` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          listOf("clinicId", "itemId", "createdAt", "pendingSync").forEach { column ->
            db.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_stock_movements_$column` " +
                "ON `stock_movements` (`$column`)",
            )
          }
        }
      }

    val MIGRATION_7_8 =
      object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `payment_methods` (" +
              "`id` TEXT NOT NULL, `clinicId` TEXT NOT NULL, `name` TEXT NOT NULL, " +
              "`affectsCash` INTEGER NOT NULL, `active` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, " +
              "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `revision` INTEGER NOT NULL, " +
              "`deletedAt` INTEGER, `originDeviceId` TEXT NOT NULL, `pendingSync` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_methods_clinicId` ON `payment_methods` (`clinicId`)")
          db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_methods_pendingSync` ON `payment_methods` (`pendingSync`)")

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sales` (" +
              "`id` TEXT NOT NULL, `clinicId` TEXT NOT NULL, `number` TEXT NOT NULL, " +
              "`clientId` TEXT, `clientName` TEXT, `status` TEXT NOT NULL, " +
              "`subtotalCents` INTEGER NOT NULL, `discountCents` INTEGER NOT NULL, " +
              "`totalCents` INTEGER NOT NULL, `notes` TEXT, `soldAt` INTEGER NOT NULL, " +
              "`cancelledAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
              "`revision` INTEGER NOT NULL, `originDeviceId` TEXT NOT NULL, `pendingSync` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          listOf("clinicId", "clientId", "soldAt", "status", "pendingSync").forEach { column ->
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_$column` ON `sales` (`$column`)")
          }
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_sales_clinicId_number` ON `sales` (`clinicId`, `number`)",
          )

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sale_items` (" +
              "`id` TEXT NOT NULL, `clinicId` TEXT NOT NULL, `saleId` TEXT NOT NULL, " +
              "`itemId` TEXT NOT NULL, `itemKind` TEXT NOT NULL, `itemName` TEXT NOT NULL, " +
              "`areaId` TEXT NOT NULL, `areaName` TEXT NOT NULL, `quantity` INTEGER NOT NULL, " +
              "`unitPriceCents` INTEGER NOT NULL, `unitCostCents` INTEGER, `discountCents` INTEGER NOT NULL, " +
              "`grossCents` INTEGER NOT NULL, `netCents` INTEGER NOT NULL, `expensesPercentBp` INTEGER NOT NULL, " +
              "`lot` TEXT, `expiresOn` TEXT, `createdAt` INTEGER NOT NULL, " +
              "`originDeviceId` TEXT NOT NULL, `pendingSync` INTEGER NOT NULL, PRIMARY KEY(`id`))",
          )
          listOf("clinicId", "saleId", "itemId", "areaId", "pendingSync").forEach { column ->
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_$column` ON `sale_items` (`$column`)")
          }

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `payments` (" +
              "`id` TEXT NOT NULL, `clinicId` TEXT NOT NULL, `saleId` TEXT NOT NULL, " +
              "`methodId` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `tenderedCents` INTEGER, " +
              "`changeCents` INTEGER, `createdAt` INTEGER NOT NULL, `originDeviceId` TEXT NOT NULL, " +
              "`pendingSync` INTEGER NOT NULL, PRIMARY KEY(`id`))",
          )
          listOf("clinicId", "saleId", "methodId", "pendingSync").forEach { column ->
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_$column` ON `payments` (`$column`)")
          }

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_sessions` (" +
              "`id` TEXT NOT NULL, `clinicId` TEXT NOT NULL, `openedAt` INTEGER NOT NULL, " +
              "`closedAt` INTEGER, `openingCents` INTEGER NOT NULL, `expectedCashCents` INTEGER, " +
              "`countedCashCents` INTEGER, `differenceCents` INTEGER, `status` TEXT NOT NULL, " +
              "`notes` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
              "`revision` INTEGER NOT NULL, `originDeviceId` TEXT NOT NULL, `pendingSync` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          listOf("clinicId", "status", "pendingSync").forEach { column ->
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_sessions_$column` ON `cash_sessions` (`$column`)")
          }

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_movements` (" +
              "`id` TEXT NOT NULL, `clinicId` TEXT NOT NULL, `sessionId` TEXT NOT NULL, " +
              "`type` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `reason` TEXT, `saleId` TEXT, " +
              "`paymentId` TEXT, `saleCancellationId` TEXT, `createdAt` INTEGER NOT NULL, " +
              "`originDeviceId` TEXT NOT NULL, `pendingSync` INTEGER NOT NULL, PRIMARY KEY(`id`))",
          )
          listOf("clinicId", "sessionId", "createdAt", "pendingSync").forEach { column ->
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movements_$column` ON `cash_movements` (`$column`)")
          }

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sale_cancellations` (" +
              "`id` TEXT NOT NULL, `clinicId` TEXT NOT NULL, `saleId` TEXT NOT NULL, " +
              "`reason` TEXT NOT NULL, `cancelledAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
              "`originDeviceId` TEXT NOT NULL, `pendingSync` INTEGER NOT NULL, PRIMARY KEY(`id`))",
          )
          listOf("clinicId", "saleId", "pendingSync").forEach { column ->
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_cancellations_$column` ON `sale_cancellations` (`$column`)")
          }

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sale_counters` (" +
              "`clinicId` TEXT NOT NULL, `day` TEXT NOT NULL, `lastNumber` INTEGER NOT NULL, " +
              "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`clinicId`, `day`))",
          )

          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `area_cost_settings` (" +
              "`id` TEXT NOT NULL, `clinicId` TEXT NOT NULL, `areaId` TEXT NOT NULL, " +
              "`expensesPercentBp` INTEGER NOT NULL, `validFrom` TEXT NOT NULL, " +
              "`createdAt` INTEGER NOT NULL, `originDeviceId` TEXT NOT NULL, `pendingSync` INTEGER NOT NULL, " +
              "PRIMARY KEY(`id`))",
          )
          listOf("clinicId", "areaId", "validFrom", "pendingSync").forEach { column ->
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_area_cost_settings_$column` ON `area_cost_settings` (`$column`)")
          }
        }
      }

    fun build(context: Context): VetDatabase =
      Room.databaseBuilder(context, VetDatabase::class.java, "veterapp.db")
        .addMigrations(
          MIGRATION_1_2,
          MIGRATION_2_3,
          MIGRATION_3_4,
          MIGRATION_4_5,
          MIGRATION_5_6,
          MIGRATION_6_7,
          MIGRATION_7_8,
        )
        .build()
  }
}
