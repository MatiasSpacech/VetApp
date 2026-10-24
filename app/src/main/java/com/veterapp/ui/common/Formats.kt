package com.veterapp.ui.common

import com.veterapp.domain.ClinicTime
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun formatDateTime(timestamp: Long): String {
  if (timestamp <= 0L) return "nunca"
  return Instant.ofEpochMilli(timestamp)
    .atZone(ClinicTime.zone)
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}

fun formatDate(timestamp: Long): String {
  if (timestamp <= 0L) return "—"
  return Instant.ofEpochMilli(timestamp)
    .atZone(ClinicTime.zone)
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

fun formatEpochDay(epochDay: Long): String =
  LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun formatConsultationDate(timestamp: Long): String = formatDate(timestamp)
