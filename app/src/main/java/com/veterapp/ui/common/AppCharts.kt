package com.veterapp.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MiniBarChart(
  values: List<Float>,
  modifier: Modifier = Modifier,
  height: Dp = 96.dp,
  color: Color = MaterialTheme.colorScheme.primary,
  trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
  val progress = remember { Animatable(0f) }
  LaunchedEffect(values) {
    progress.snapTo(0f)
    progress.animateTo(1f, tween(550))
  }
  Canvas(modifier = modifier.fillMaxWidth().height(height)) {
    if (values.isEmpty()) return@Canvas
    val max = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val slot = size.width / values.size
    val barWidth = (slot * 0.55f).coerceAtLeast(4f)
    val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
    values.forEachIndexed { index, value ->
      val barHeight = (size.height * (value / max) * progress.value).coerceAtLeast(3f)
      val left = slot * index + (slot - barWidth) / 2f
      drawRoundRect(
        color = trackColor,
        topLeft = Offset(left, 0f),
        size = Size(barWidth, size.height),
        cornerRadius = radius,
        alpha = 0.45f,
      )
      drawRoundRect(
        color = color,
        topLeft = Offset(left, size.height - barHeight),
        size = Size(barWidth, barHeight),
        cornerRadius = radius,
      )
    }
  }
}

@Composable
fun MiniLineChart(
  values: List<Float>,
  modifier: Modifier = Modifier,
  height: Dp = 110.dp,
  color: Color = MaterialTheme.colorScheme.secondary,
) {
  val progress = remember { Animatable(0f) }
  LaunchedEffect(values) {
    progress.snapTo(0f)
    progress.animateTo(1f, tween(650))
  }
  Canvas(modifier = modifier.fillMaxWidth().height(height)) {
    if (values.size < 2) return@Canvas
    val max = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val min = values.minOrNull()?.coerceAtMost(0f) ?: 0f
    val range = (max - min).coerceAtLeast(1f)
    val stepX = size.width / (values.size - 1)
    val points =
      values.mapIndexed { index, value ->
        Offset(
          x = stepX * index,
          y = size.height - ((value - min) / range) * size.height * progress.value,
        )
      }
    val area =
      Path().apply {
        moveTo(points.first().x, size.height)
        points.forEach { lineTo(it.x, it.y) }
        lineTo(points.last().x, size.height)
        close()
      }
    drawPath(
      path = area,
      brush =
        Brush.verticalGradient(
          colors = listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.02f)),
        ),
    )
    val line =
      Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
      }
    drawPath(
      path = line,
      color = color,
      style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
    )
    points.forEachIndexed { index, point ->
      if (index == 0 || index == points.lastIndex) {
        drawCircle(color = color, radius = 3.5.dp.toPx(), center = point)
        drawCircle(color = Color.White, radius = 1.6.dp.toPx(), center = point)
      }
    }
  }
}
