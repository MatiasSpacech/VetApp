package com.veterapp.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
  lightColorScheme(
    primary = Violet40,
    onPrimary = Color.White,
    primaryContainer = Violet90,
    onPrimaryContainer = Violet10,
    inversePrimary = Violet80,
    secondary = Mint40,
    onSecondary = Color.White,
    secondaryContainer = Mint90,
    onSecondaryContainer = Mint10,
    tertiary = Peach50,
    onTertiary = Color.White,
    tertiaryContainer = Peach90,
    onTertiaryContainer = Peach10,
    background = Canvas,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = InkSoft,
    surfaceTint = Violet40,
    inverseSurface = Color(0xFF322F38),
    inverseOnSurface = Color(0xFFF4EFF6),
    error = Coral,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF7A7286),
    outlineVariant = Line,
    scrim = Color.Black,
    surfaceBright = Color(0xFFFFFBFF),
    surfaceDim = Color(0xFFE4DFE9),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF8FF),
    surfaceContainer = Color(0xFFF5F0FA),
    surfaceContainerHigh = Color(0xFFEFE9F5),
    surfaceContainerHighest = Color(0xFFE9E2F0),
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = Violet80,
    onPrimary = Color(0xFF351E73),
    primaryContainer = Color(0xFF4C3396),
    onPrimaryContainer = Violet90,
    inversePrimary = Violet40,
    secondary = Mint70,
    onSecondary = Mint10,
    secondaryContainer = Color(0xFF005142),
    onSecondaryContainer = Mint90,
    tertiary = Peach70,
    onTertiary = Peach10,
    tertiaryContainer = Color(0xFF6E3400),
    onTertiaryContainer = Peach90,
    background = Night,
    onBackground = Moon,
    surface = Night,
    onSurface = Moon,
    surfaceVariant = NightLine,
    onSurfaceVariant = MoonSoft,
    surfaceTint = Violet80,
    inverseSurface = Moon,
    inverseOnSurface = Color(0xFF322F38),
    error = CoralLight,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = CoralContainer,
    outline = Color(0xFF958D9E),
    outlineVariant = NightLine,
    scrim = Color.Black,
    surfaceBright = Color(0xFF3B3445),
    surfaceDim = Night,
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = NightSurface,
    surfaceContainer = Color(0xFF211D27),
    surfaceContainerHigh = NightRaised,
    surfaceContainerHighest = Color(0xFF312A39),
  )

@Composable
fun VeterAppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // La identidad de VeterApp manda sobre el color dinámico.
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }
  val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors

  CompositionLocalProvider(LocalExtendedColors provides extended) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = AppShapes,
      content = content,
    )
  }
}
