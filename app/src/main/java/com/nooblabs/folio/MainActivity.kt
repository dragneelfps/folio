package com.nooblabs.folio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.nooblabs.folio.theme.FolioTheme

import androidx.activity.SystemBarStyle

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val settingsRepository = (application as FolioApplication).container.settingsRepository

    setContent {
      val themeState by settingsRepository.appTheme.collectAsState()
      val darkTheme = when (themeState) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
      }

      // Update system bars icon coloring dynamically based on chosen app theme
      val statusBarStyle = if (darkTheme) {
        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
      } else {
        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
      }
      enableEdgeToEdge(statusBarStyle = statusBarStyle, navigationBarStyle = statusBarStyle)

      FolioTheme(darkTheme = darkTheme) {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          com.nooblabs.folio.ui.MainNavigation(settingsRepository = settingsRepository)
        }
      }
    }
  }
}
