package com.veterapp.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

object AppSpacing {
  val xxs = 4.dp
  val xs = 8.dp
  val sm = 12.dp
  val md = 16.dp
  val lg = 20.dp
  val xl = 24.dp
  val xxl = 32.dp
}

object AppDimens {
  val screenPadding = 20.dp
  val cardRadius = 22.dp
  val inputRadius = 16.dp
  val buttonRadius = 14.dp
  val sheetRadius = 28.dp
  val iconSmall = 18.dp
  val iconMedium = 22.dp
  val iconLarge = 28.dp
  val touchTarget = 48.dp
  val cardBorder = 1.dp
}
