package com.veterapp.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
  fun schedulePeriodic() {
    val request =
      PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(1, TimeUnit.HOURS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      UNIQUE_PERIODIC,
      ExistingPeriodicWorkPolicy.KEEP,
      request,
    )
  }

  fun requestRefresh() {
    val request =
      OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(2, TimeUnit.SECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
      UNIQUE_REFRESH,
      ExistingWorkPolicy.REPLACE,
      request,
    )
  }

  private companion object {
    const val UNIQUE_PERIODIC = "veterapp-reminders-periodic"
    const val UNIQUE_REFRESH = "veterapp-reminders-refresh"
  }
}
