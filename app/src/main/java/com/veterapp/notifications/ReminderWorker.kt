package com.veterapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.veterapp.VeterAppApplication

class ReminderWorker(
  appContext: Context,
  workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
  override suspend fun doWork(): Result {
    val app = applicationContext as? VeterAppApplication ?: return Result.success()
    return runCatching { app.container.reminderService.run() }
      .fold(
        onSuccess = { Result.success() },
        onFailure = {
          if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
        },
      )
  }

  private companion object {
    const val MAX_RETRIES = 3
  }
}
