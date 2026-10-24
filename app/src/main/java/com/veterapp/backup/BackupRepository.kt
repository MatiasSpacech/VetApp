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
              ownerHistory = database.ownerHistoryDao().all(),
              consultations = database.consultationDao().all(),
              consultationAddenda = database.addendumDao().all(),
              vaccinations = database.vaccinationDao().all(),
              vaccinationNotifications = database.vaccinationNotificationDao().all(),
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
        runCatching {
          auditRepository.record(
            entity = "backup",
            entityId = null,
            action = "backup.export",
            payload =
              buildJsonObject {
                put("clients", snapshot.clients.size)
                put("pets", snapshot.pets.size)
                put("owner_history", snapshot.ownerHistory.size)
                put("consultations", snapshot.consultations.size)
                put("consultation_addenda", snapshot.consultationAddenda.size)
                put("vaccinations", snapshot.vaccinations.size)
                put("vaccination_notifications", snapshot.vaccinationNotifications.size)
                put("outbox", snapshot.outbox.size)
                put("audit", snapshot.audit.size)
              },
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
        require(snapshot.clients.all { it.clinicId == expectedClinic }) {
          "El backup pertenece a otra clínica"
        }
        require(snapshot.pets.all { it.clinicId == expectedClinic }) {
          "El backup pertenece a otra clínica"
        }
        require(snapshot.ownerHistory.all { it.clinicId == expectedClinic }) {
          "El backup pertenece a otra clínica"
        }
        require(snapshot.consultations.all { it.clinicId == expectedClinic }) {
          "El backup pertenece a otra clínica"
        }
        require(snapshot.consultationAddenda.all { it.clinicId == expectedClinic }) {
          "El backup pertenece a otra clínica"
        }
        require(snapshot.vaccinations.all { it.clinicId == expectedClinic }) {
          "El backup pertenece a otra clínica"
        }
        require(snapshot.vaccinationNotifications.all { it.clinicId == expectedClinic }) {
          "El backup pertenece a otra clínica"
        }
        require(snapshot.audit.all { it.clinicId == expectedClinic }) {
          "El backup pertenece a otra clínica"
        }

        database.withTransaction {
          database.petDao().clear()
          database.outboxDao().clear()
          database.auditDao().clear()
          database.clientDao().clear()
          database.ownerHistoryDao().clear()
          database.consultationDao().clear()
          database.addendumDao().clear()
          database.vaccinationDao().clear()
          database.vaccinationNotificationDao().clear()
          database.clientDao().insertAll(snapshot.clients)
          database.petDao().insertAll(snapshot.pets)
          database.ownerHistoryDao().insertAll(snapshot.ownerHistory)
          database.consultationDao().insertAll(snapshot.consultations)
          database.addendumDao().insertAll(snapshot.consultationAddenda)
          database.vaccinationDao().insertAll(snapshot.vaccinations)
          database.vaccinationNotificationDao().insertAll(snapshot.vaccinationNotifications)
          database.outboxDao().insertAll(snapshot.outbox)
          database.auditDao().insertAll(snapshot.audit)
          auditRepository.record(
            entity = "backup",
            entityId = null,
            action = "backup.restore",
            payload =
              buildJsonObject {
                put("clients", snapshot.clients.size)
                put("pets", snapshot.pets.size)
                put("owner_history", snapshot.ownerHistory.size)
                put("consultations", snapshot.consultations.size)
                put("consultation_addenda", snapshot.consultationAddenda.size)
                put("vaccinations", snapshot.vaccinations.size)
                put("vaccination_notifications", snapshot.vaccinationNotifications.size)
                put("outbox", snapshot.outbox.size)
                put("audit", snapshot.audit.size)
              },
          )
        }
        settings.lastBackupAt = System.currentTimeMillis()
        onRestored()
        snapshot.pets.size
      }
    }

  private companion object {
    const val MIN_PASSPHRASE = 8
    const val MAX_BACKUP_BYTES = 20 * 1024 * 1024
  }
}
