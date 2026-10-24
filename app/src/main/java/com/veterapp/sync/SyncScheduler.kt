package com.veterapp.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SyncScheduler(private val context: Context) {
  fun requestSync() {
    val request =
      OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(
          Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
        )
        .setInitialDelay(700, TimeUnit.MILLISECONDS)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
      UNIQUE_SYNC,
      ExistingWorkPolicy.APPEND_OR_REPLACE,
      request,
    )
  }

  fun schedulePeriodic() {
    val request =
      PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
        .setConstraints(
          Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
        )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      UNIQUE_PERIODIC,
      ExistingPeriodicWorkPolicy.KEEP,
      request,
    )
  }

  private companion object {
    const val UNIQUE_SYNC = "veterapp-sync"
    const val UNIQUE_PERIODIC = "veterapp-sync-periodic"
  }
}
