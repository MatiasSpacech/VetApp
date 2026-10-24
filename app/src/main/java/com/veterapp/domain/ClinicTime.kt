package com.veterapp.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ClinicTime {
  val zone: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")

  fun today(): LocalDate = LocalDate.now(zone)

  fun todayEpochDay(): Long = today().toEpochDay()

  fun instantAtNoon(epochDay: Long): Long =
    LocalDate.ofEpochDay(epochDay).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

  fun instantAt(epochDay: Long, minuteOfDay: Int): Long =
    LocalDate.ofEpochDay(epochDay)
      .atStartOfDay(zone)
      .plusMinutes(minuteOfDay.toLong())
      .toInstant()
      .toEpochMilli()

  fun minuteOfDay(instant: Long): Int {
    val time = Instant.ofEpochMilli(instant).atZone(zone).toLocalTime()
    return time.hour * 60 + time.minute
  }

  fun startOfDay(epochDay: Long): Long =
    LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli()

  fun dateOf(instant: Long): LocalDate = Instant.ofEpochMilli(instant).atZone(zone).toLocalDate()
}
