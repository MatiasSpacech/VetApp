package com.veterapp.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(context: Context) {
  private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
  private val _online = MutableStateFlow(isOnline())
  val online: StateFlow<Boolean> = _online.asStateFlow()

  init {
    runCatching {
      connectivityManager.registerDefaultNetworkCallback(
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            _online.value = isOnline()
          }

          override fun onLost(network: Network) {
            _online.value = isOnline()
          }

          override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _online.value = isOnline()
          }
        },
      )
    }
  }

  fun isOnline(): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }
}
