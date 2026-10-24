package com.veterapp

import android.content.Context
import com.veterapp.backup.BackupRepository
import com.veterapp.data.AppSettings
import com.veterapp.data.AuditRepository
import com.veterapp.data.AuthRepository
import com.veterapp.data.NetworkMonitor
import com.veterapp.data.PetRepository
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.SupabaseClientProvider
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
  val petRepository = PetRepository(database, settings, auditRepository)
  val photoCompressor = PhotoCompressor(context)
  val photoRepository = PhotoRepository(clientProvider)
  val syncScheduler = SyncScheduler(context)
  val securityRepository = SecurityRepository(settings)
  val backupRepository =
    BackupRepository(
      context = context,
      database = database,
      settings = settings,
      auditRepository = auditRepository,
      onRestored = { syncScheduler.requestSync() },
    )
  val syncRepository =
    SyncRepository(
      database = database,
      settings = settings,
      clientProvider = clientProvider,
      authRepository = authRepository,
      petRepository = petRepository,
    )
}
