package com.veterapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.veterapp.theme.VeterAppTheme
import com.veterapp.ui.lock.AppLockGate

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val container = (application as VeterAppApplication).container
    enableEdgeToEdge()
    setContent {
      VeterAppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          AppLockGate(securityRepository = container.securityRepository) { MainNavigation() }
        }
      }
    }
  }
}
