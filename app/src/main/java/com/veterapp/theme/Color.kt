package com.veterapp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Marca
val Violet10 = Color(0xFF250C63)
val Violet20 = Color(0xFF3A1E86)
val Violet40 = Color(0xFF6C4DD9)
val Violet60 = Color(0xFF8C74E8)
val Violet80 = Color(0xFFCFBDFF)
val Violet90 = Color(0xFFE9E0FF)
val Violet95 = Color(0xFFF5F0FF)

val Mint10 = Color(0xFF00382D)
val Mint30 = Color(0xFF006B57)
val Mint40 = Color(0xFF1E9E85)
val Mint70 = Color(0xFF7EDCC7)
val Mint90 = Color(0xFFCFF6EC)

val Peach10 = Color(0xFF3B1600)
val Peach30 = Color(0xFF8A4A1B)
val Peach50 = Color(0xFFE8894A)
val Peach70 = Color(0xFFFFB68A)
val Peach90 = Color(0xFFFFE4D1)

// Neutros claros con tinte lavanda
val Ink = Color(0xFF1D1A24)
val InkSoft = Color(0xFF4A4458)
val InkMuted = Color(0xFF77707F)
val Canvas = Color(0xFFF8F6FC)
val Paper = Color(0xFFFFFCFF)
val Mist = Color(0xFFEFE9F7)
val Line = Color(0xFFD8D0E4)

// Neutros oscuros
val Night = Color(0xFF141118)
val NightSurface = Color(0xFF1C1922)
val NightRaised = Color(0xFF26212D)
val NightLine = Color(0xFF4A4458)
val Moon = Color(0xFFE8E2EC)
val MoonSoft = Color(0xFFCAC4D0)

// Semánticos
val SuccessDark = Color(0xFF2E7D5B)
val SuccessLight = Color(0xFF7EDCB2)
val WarningDark = Color(0xFFA96500)
val WarningLight = Color(0xFFFFC46B)
val InfoDark = Color(0xFF2F6FB3)
val InfoLight = Color(0xFF9CC7FF)
val Coral = Color(0xFFB3261E)
val CoralLight = Color(0xFFF2B8B5)
val CoralContainer = Color(0xFFF9DEDC)
val AmberContainer = Color(0xFFFFE2B8)
val MintContainer = Color(0xFFD3F5E4)
val BlueContainer = Color(0xFFD7E9FF)

@Immutable
data class ExtendedColors(
  val success: Color,
  val onSuccess: Color,
  val successContainer: Color,
  val onSuccessContainer: Color,
  val warning: Color,
  val onWarning: Color,
  val warningContainer: Color,
  val onWarningContainer: Color,
  val info: Color,
  val onInfo: Color,
  val infoContainer: Color,
  val onInfoContainer: Color,
  val heroGradient: List<Color>,
  val successGradient: List<Color>,
  val warnGradient: List<Color>,
)

val LightExtendedColors =
  ExtendedColors(
    success = SuccessDark,
    onSuccess = Color.White,
    successContainer = MintContainer,
    onSuccessContainer = Color(0xFF06301F),
    warning = WarningDark,
    onWarning = Color.White,
    warningContainer = AmberContainer,
    onWarningContainer = Color(0xFF351F00),
    info = InfoDark,
    onInfo = Color.White,
    infoContainer = BlueContainer,
    onInfoContainer = Color(0xFF001B36),
    heroGradient = listOf(Violet40, Color(0xFF9A72F0), Mint40),
    successGradient = listOf(Color(0xFF2E9E7F), Color(0xFF7EDCC7)),
    warnGradient = listOf(Peach50, Color(0xFFF4BE7E)),
  )

val DarkExtendedColors =
  ExtendedColors(
    success = SuccessLight,
    onSuccess = Color(0xFF00382D),
    successContainer = Color(0xFF005142),
    onSuccessContainer = Mint90,
    warning = WarningLight,
    onWarning = Color(0xFF3B2500),
    warningContainer = Color(0xFF5C3A00),
    onWarningContainer = Color(0xFFFFE2B8),
    info = InfoLight,
    onInfo = Color(0xFF002F5A),
    infoContainer = Color(0xFF17456F),
    onInfoContainer = BlueContainer,
    heroGradient = listOf(Color(0xFF3B1F7A), Color(0xFF5B3AA8), Color(0xFF0F5C4F)),
    successGradient = listOf(Color(0xFF0F6B56), Color(0xFF3AA58C)),
    warnGradient = listOf(Color(0xFF7A3F12), Color(0xFFB87435)),
  )

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.extendedColors: ExtendedColors
  @Composable
  @ReadOnlyComposable
  get() = LocalExtendedColors.current
