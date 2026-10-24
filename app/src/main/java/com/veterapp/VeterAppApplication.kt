package com.veterapp

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VeterAppApplication : Application() {
  lateinit var container: AppContainer
    private set

  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
    container.syncScheduler.schedulePeriodic()

    ProcessLifecycleOwner.get().lifecycle.addObserver(
      object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
          container.securityRepository.onForeground()
        }

        override fun onStop(owner: LifecycleOwner) {
          container.securityRepository.onBackground()
        }
      },
    )

    applicationScope.launch {
      container.networkMonitor.online.collect { online ->
        if (online) container.syncScheduler.requestSync()
      }
    }
  }
}
