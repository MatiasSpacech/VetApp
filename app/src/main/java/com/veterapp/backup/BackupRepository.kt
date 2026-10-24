package com.veterapp.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.veterapp.BuildConfig
import com.veterapp.data.AppSettings
import com.veterapp.data.AuditRepository
import com.veterapp.data.local.VetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BackupRepository(
  private val context: Context,
  private val database: VetDatabase,
  private val settings: AppSettings,
  private val auditRepository: AuditRepository,
  private val onRestored: () -> Unit = {},
) {
  private val resolver = context.contentResolver
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  suspend fun export(passphrase: CharArray, uri: Uri): Result<Int> =
    withContext(Dispatchers.IO) {
      runCatching {
        require(passphrase.size >= MIN_PASSPHRASE) { "La frase debe tener al menos 8 caracteres" }
        val snapshot =
          database.withTransaction {
            BackupSnapshot(
              exportedAt = System.currentTimeMillis(),
              appVersion = BuildConfig.VERSION_NAME,
              clients = database.clientDao().all(),
              pets = database.petDao().all(),
              petWeights = database.petWeightDao().all(),
              ownerHistory = database.ownerHistoryDao().all(),
              consultations = database.consultationDao().all(),
              consultationAddenda = database.addendumDao().all(),
              vaccinations = database.vaccinationDao().all(),
              vaccinationNotifications = database.vaccinationNotificationDao().all(),
              appointments = database.appointmentDao().all(),
              areas = database.areaDao().all(),
              categories = database.categoryDao().all(),
              catalogItems = database.catalogItemDao().all(),
              stockMovements = database.stockMovementDao().all(),
              paymentMethods = database.paymentMethodDao().all(),
              sales = database.saleDao().all(),
              saleItems = database.saleItemDao().all(),
              payments = database.paymentDao().all(),
              cashSessions = database.cashSessionDao().all(),
              cashMovements = database.cashMovementDao().all(),
              saleCancellations = database.saleCancellationDao().all(),
              saleCounters = database.saleCounterDao().all(),
              areaCostSettings = database.areaCostSettingDao().all(),
              outbox = database.outboxDao().all(),
              audit = database.auditDao().all(),
            )
          }
        val plaintext = json.encodeToString(snapshot).encodeToByteArray()
        val encrypted = BackupCrypto.encrypt(passphrase, plaintext)

        resolver.openOutputStream(uri, "wt")?.use { it.write(encrypted) }
          ?: error("No se pudo escribir el backup")

        val now = System.currentTimeMillis()
        settings.lastBackupAt = now
        val counts =
          buildJsonObject {
            put("clients", snapshot.clients.size)
            put("pets", snapshot.pets.size)
            put("pet_weights", snapshot.petWeights.size)
            put("owner_history", snapshot.ownerHistory.size)
            put("consultations", snapshot.consultations.size)
            put("consultation_addenda", snapshot.consultationAddenda.size)
            put("vaccinations", snapshot.vaccinations.size)
            put("vaccination_notifications", snapshot.vaccinationNotifications.size)
            put("appointments", snapshot.appointments.size)
            put("areas", snapshot.areas.size)
            put("categories", snapshot.categories.size)
            put("catalog_items", snapshot.catalogItems.size)
            put("stock_movements", snapshot.stockMovements.size)
            put("payment_methods", snapshot.paymentMethods.size)
            put("sales", snapshot.sales.size)
            put("sale_items", snapshot.saleItems.size)
            put("payments", snapshot.payments.size)
            put("cash_sessions", snapshot.cashSessions.size)
            put("cash_movements", snapshot.cashMovements.size)
            put("sale_cancellations", snapshot.saleCancellations.size)
            put("sale_counters", snapshot.saleCounters.size)
            put("area_cost_settings", snapshot.areaCostSettings.size)
            put("outbox", snapshot.outbox.size)
            put("audit", snapshot.audit.size)
          }
        runCatching {
          auditRepository.record(
            entity = "backup",
            entityId = null,
            action = "backup.export",
            payload = counts,
          )
        }
        snapshot.pets.size
      }
    }

  suspend fun restore(passphrase: CharArray, uri: Uri): Result<Int> =
    withContext(Dispatchers.IO) {
      runCatching {
        require(passphrase.size >= MIN_PASSPHRASE) { "La frase debe tener al menos 8 caracteres" }
        val encrypted =
          resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("No se pudo leer el backup")
        require(encrypted.size <= MAX_BACKUP_BYTES) { "El backup es demasiado grande" }

        val plaintext =
          try {
            BackupCrypto.decrypt(passphrase, encrypted)
          } catch (error: Throwable) {
            throw IllegalArgumentException("Frase incorrecta o backup dañado", error)
          }

        val snapshot = json.decodeFromString<BackupSnapshot>(plaintext.decodeToString())
        require(snapshot.version in BackupSnapshot.MIN_VERSION..BackupSnapshot.CURRENT_VERSION) {
          "Versión de backup no soportada"
        }
        val expectedClinic = settings.clinicId ?: settings.localClinicId
        val clinicLists =
          listOf(
            snapshot.clients.map { it.clinicId },
            snapshot.pets.map { it.clinicId },
            snapshot.petWeights.map { it.clinicId },
            snapshot.ownerHistory.map { it.clinicId },
            snapshot.consultations.map { it.clinicId },
            snapshot.consultationAddenda.map { it.clinicId },
            snapshot.vaccinations.map { it.clinicId },
            snapshot.vaccinationNotifications.map { it.clinicId },
            snapshot.appointments.map { it.clinicId },
            snapshot.areas.map { it.clinicId },
            snapshot.categories.map { it.clinicId },
            snapshot.catalogItems.map { it.clinicId },
            snapshot.stockMovements.map { it.clinicId },
            snapshot.paymentMethods.map { it.clinicId },
            snapshot.sales.map { it.clinicId },
            snapshot.saleItems.map { it.clinicId },
            snapshot.payments.map { it.clinicId },
            snapshot.cashSessions.map { it.clinicId },
            snapshot.cashMovements.map { it.clinicId },
            snapshot.saleCancellations.map { it.clinicId },
            snapshot.saleCounters.map { it.clinicId },
            snapshot.areaCostSettings.map { it.clinicId },
            snapshot.audit.map { it.clinicId },
          )
        require(clinicLists.all { list -> list.all { it == expectedClinic } }) {
          "El backup pertenece a otra clínica"
        }

        database.withTransaction {
          database.petDao().clear()
          database.petWeightDao().clear()
          database.outboxDao().clear()
          database.auditDao().clear()
          database.clientDao().clear()
          database.ownerHistoryDao().clear()
          database.consultationDao().clear()
          database.addendumDao().clear()
          database.vaccinationDao().clear()
          database.vaccinationNotificationDao().clear()
          database.appointmentDao().clear()
          database.areaDao().clear()
          database.categoryDao().clear()
          database.catalogItemDao().clear()
          database.stockMovementDao().clear()
          database.paymentMethodDao().clear()
          database.saleDao().clear()
          database.saleItemDao().clear()
          database.paymentDao().clear()
          database.cashSessionDao().clear()
          database.cashMovementDao().clear()
          database.saleCancellationDao().clear()
          database.saleCounterDao().clear()
          database.areaCostSettingDao().clear()

          database.clientDao().insertAll(snapshot.clients)
          database.petDao().insertAll(snapshot.pets)
          database.petWeightDao().insertAll(snapshot.petWeights)
          database.ownerHistoryDao().insertAll(snapshot.ownerHistory)
          database.consultationDao().insertAll(snapshot.consultations)
          database.addendumDao().insertAll(snapshot.consultationAddenda)
          database.vaccinationDao().insertAll(snapshot.vaccinations)
          database.vaccinationNotificationDao().insertAll(snapshot.vaccinationNotifications)
          database.appointmentDao().insertAll(snapshot.appointments)
          database.areaDao().insertAll(snapshot.areas)
          database.categoryDao().insertAll(snapshot.categories)
          database.catalogItemDao().insertAll(snapshot.catalogItems)
          database.stockMovementDao().insertAll(snapshot.stockMovements)
          database.paymentMethodDao().insertAll(snapshot.paymentMethods)
          database.saleDao().insertAll(snapshot.sales)
          database.saleItemDao().insertAll(snapshot.saleItems)
          database.paymentDao().insertAll(snapshot.payments)
          database.cashSessionDao().insertAll(snapshot.cashSessions)
          database.cashMovementDao().insertAll(snapshot.cashMovements)
          database.saleCancellationDao().insertAll(snapshot.saleCancellations)
          database.saleCounterDao().insertAll(snapshot.saleCounters)
          database.areaCostSettingDao().insertAll(snapshot.areaCostSettings)
          database.outboxDao().insertAll(snapshot.outbox)
          database.auditDao().insertAll(snapshot.audit)

          reconcileAfterRestore(snapshot)

          val counts =
            buildJsonObject {
              put("clients", snapshot.clients.size)
              put("pets", snapshot.pets.size)
              put("pet_weights", snapshot.petWeights.size)
              put("consultations", snapshot.consultations.size)
              put("vaccinations", snapshot.vaccinations.size)
              put("appointments", snapshot.appointments.size)
              put("catalog_items", snapshot.catalogItems.size)
              put("sales", snapshot.sales.size)
              put("cash_sessions", snapshot.cashSessions.size)
              put("outbox", snapshot.outbox.size)
              put("audit", snapshot.audit.size)
            }
          auditRepository.record(
            entity = "backup",
            entityId = null,
            action = "backup.restore",
            payload = counts,
          )
        }
        settings.lastBackupAt = System.currentTimeMillis()
        onRestored()
        snapshot.pets.size
      }
    }

  private suspend fun reconcileAfterRestore(snapshot: BackupSnapshot) {
    var recalculated = 0
    snapshot.catalogItems.forEach { item ->
      if (!item.trackStock) return@forEach
      val sum = snapshot.stockMovements.filter { it.itemId == item.id }.sumOf { it.quantityDelta }
      if (sum != item.stockCached) {
        database.catalogItemDao().upsert(item.copy(stockCached = sum))
        recalculated++
      }
    }
    if (recalculated > 0) {
      auditRepository.record(
        entity = "backup",
        entityId = null,
        action = "backup.stock.recalculate",
        payload = buildJsonObject { put("items", recalculated) },
      )
    }
    val saleMismatch =
      snapshot.sales.count { sale ->
        val lines = snapshot.saleItems.filter { it.saleId == sale.id }
        lines.sumOf { it.netCents } != sale.totalCents
      }
    val cashMismatch =
      snapshot.cashSessions.count { session ->
        val expected = session.openingCents + snapshot.cashMovements.filter { it.sessionId == session.id }.sumOf { it.amountCents }
        session.status == "closed" && session.expectedCashCents != expected
      }
    if (saleMismatch > 0 || cashMismatch > 0) {
      auditRepository.record(
        entity = "backup",
        entityId = null,
        action = "backup.consistency",
        payload =
          buildJsonObject {
            put("sale_mismatch", saleMismatch)
            put("cash_mismatch", cashMismatch)
          },
      )
    }
  }

  private companion object {
    const val MIN_PASSPHRASE = 8
    const val MAX_BACKUP_BYTES = 40 * 1024 * 1024
  }
}
