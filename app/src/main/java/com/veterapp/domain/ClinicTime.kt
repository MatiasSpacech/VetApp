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

  fun dateOf(instant: Long): LocalDate = Instant.ofEpochMilli(instant).atZone(zone).toLocalDate()
}
