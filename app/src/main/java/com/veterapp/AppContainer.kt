package com.veterapp

import android.content.Context
import com.veterapp.backup.BackupRepository
import com.veterapp.data.AppSettings
import com.veterapp.data.AppointmentRepository
import com.veterapp.data.AuditRepository
import com.veterapp.data.AuthRepository
import com.veterapp.data.CatalogRepository
import com.veterapp.data.ClientRepository
import com.veterapp.data.ClinicalRepository
import com.veterapp.data.CommerceRepository
import com.veterapp.data.NetworkMonitor
import com.veterapp.data.PetRepository
import com.veterapp.data.ReportRepository
import com.veterapp.data.VaccinationRepository
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.SupabaseClientProvider
import com.veterapp.export.ReportExporter
import com.veterapp.notifications.ReminderNotifications
import com.veterapp.notifications.ReminderScheduler
import com.veterapp.notifications.ReminderService
import com.veterapp.photo.PhotoCompressor
import com.veterapp.photo.PhotoRepository
import com.veterapp.security.SecurityRepository
import com.veterapp.sync.SyncRepository
import com.veterapp.sync.SyncScheduler

class AppContainer(context: Context) {
  val settings = AppSettings(context)
  val database = VetDatabase.build(context)
  val networkMonitor = NetworkMonitor(context)
  val clientProvider = SupabaseClientProvider()
  val authRepository = AuthRepository(clientProvider, settings)
  val auditRepository = AuditRepository(database, settings)
  val clientRepository = ClientRepository(database, settings, auditRepository)
  val petRepository = PetRepository(database, settings, auditRepository)
  val clinicalRepository = ClinicalRepository(database, settings, auditRepository)
  val vaccinationRepository = VaccinationRepository(database, settings, auditRepository)
  val appointmentRepository = AppointmentRepository(database, settings, auditRepository)
  val catalogRepository = CatalogRepository(database, settings, auditRepository)
  val commerceRepository = CommerceRepository(database, settings, auditRepository)
  val reportRepository = ReportRepository(database, settings)
  val reportExporter = ReportExporter(context)
  val photoCompressor = PhotoCompressor(context)
  val photoRepository = PhotoRepository(clientProvider)
  val syncScheduler = SyncScheduler(context)
  val reminderNotifications = ReminderNotifications(context)
  val reminderService =
    ReminderService(
      vaccinationRepository = vaccinationRepository,
      notificationDao = database.vaccinationNotificationDao(),
      notifications = reminderNotifications,
      settings = settings,
    )
  val reminderScheduler = ReminderScheduler(context)
  val securityRepository = SecurityRepository(settings)
  val backupRepository =
    BackupRepository(
      context = context,
      database = database,
      settings = settings,
      auditRepository = auditRepository,
      onRestored = {
        syncScheduler.requestSync()
        reminderScheduler.requestRefresh()
      },
    )
  val syncRepository =
    SyncRepository(
      database = database,
      settings = settings,
      clientProvider = clientProvider,
      authRepository = authRepository,
      petRepository = petRepository,
      clientRepository = clientRepository,
      clinicalRepository = clinicalRepository,
      vaccinationRepository = vaccinationRepository,
      appointmentRepository = appointmentRepository,
      catalogRepository = catalogRepository,
      commerceRepository = commerceRepository,
    )
}
