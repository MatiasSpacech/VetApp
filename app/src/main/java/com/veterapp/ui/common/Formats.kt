package com.veterapp.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatDateTime(timestamp: Long): String {
  if (timestamp <= 0L) return "nunca"
  return Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}

fun formatDate(timestamp: Long): String {
  if (timestamp <= 0L) return "—"
  return Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}
