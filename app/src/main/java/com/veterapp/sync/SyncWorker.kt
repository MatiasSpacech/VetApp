package com.veterapp.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.veterapp.VeterAppApplication

class SyncWorker(
  appContext: Context,
  workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
  override suspend fun doWork(): Result {
    val app = applicationContext as? VeterAppApplication ?: return Result.success()
    return when (val result = app.container.syncRepository.syncNow()) {
      SyncRepository.SyncResult.Success -> {
        app.container.reminderScheduler.requestRefresh()
        Result.success()
      }
      SyncRepository.SyncResult.NotConfigured,
      SyncRepository.SyncResult.NotAuthenticated -> Result.success()
      is SyncRepository.SyncResult.Failure -> {
        if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
      }
    }
  }

  private companion object {
    const val MAX_RETRIES = 5
  }
}
