package com.veterapp.photo

import kotlin.math.roundToInt

data class Dimensions(val width: Int, val height: Int)

object ImageSizing {
  fun fitWithin(width: Int, height: Int, maxDimension: Int): Dimensions {
    require(width > 0 && height > 0) { "La imagen no tiene dimensiones válidas" }
    require(maxDimension > 0) { "El máximo debe ser positivo" }
    if (width <= maxDimension && height <= maxDimension) return Dimensions(width, height)

    val scale = maxDimension.toDouble() / maxOf(width, height)
    return Dimensions(
      width = (width * scale).roundToInt().coerceAtLeast(1),
      height = (height * scale).roundToInt().coerceAtLeast(1),
    )
  }

  fun scaledDown(width: Int, height: Int, factor: Double): Dimensions =
    Dimensions(
      width = (width * factor).roundToInt().coerceAtLeast(1),
      height = (height * factor).roundToInt().coerceAtLeast(1),
    )
}
