package com.veterapp.domain

import com.veterapp.data.local.AppointmentWithPet
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable

enum class AppointmentStatus(val storage: String, val label: String) {
  Scheduled("scheduled", "Programado"),
  Confirmed("confirmed", "Confirmado"),
  Attended("attended", "Atendido"),
  Cancelled("cancelled", "Cancelado"),
  NoShow("no_show", "Ausente"),
  ;

  val isOpen: Boolean
    get() = this == Scheduled || this == Confirmed

  companion object {
    fun fromStorage(value: String?): AppointmentStatus =
      entries.firstOrNull { it.storage == value } ?: Scheduled
  }
}

@Serializable
data class DayHours(
  val closed: Boolean = false,
  val openMinute: Int = 9 * 60,
  val closeMinute: Int = 19 * 60,
)

@Serializable
data class WeeklyHours(val days: List<DayHours> = defaultDays()) {
  fun hoursFor(date: LocalDate): DayHours {
    val index = date.dayOfWeek.value - 1
    return days.getOrElse(index) { DayHours() }
  }

  fun isWithin(startAt: Long, endAt: Long): Boolean {
    val start = java.time.Instant.ofEpochMilli(startAt).atZone(ClinicTime.zone)
    val end = java.time.Instant.ofEpochMilli(endAt).atZone(ClinicTime.zone)
    if (start.toLocalDate() != end.toLocalDate()) return false
    val hours = hoursFor(start.toLocalDate())
    if (hours.closed) return false
    val startMinute = start.hour * 60 + start.minute
    val endMinute = end.hour * 60 + end.minute
    return startMinute >= hours.openMinute && endMinute <= hours.closeMinute
  }

  companion object {
    fun defaultDays(): List<DayHours> =
      listOf(
        DayHours(openMinute = 9 * 60, closeMinute = 19 * 60),
        DayHours(openMinute = 9 * 60, closeMinute = 19 * 60),
        DayHours(openMinute = 9 * 60, closeMinute = 19 * 60),
        DayHours(openMinute = 9 * 60, closeMinute = 19 * 60),
        DayHours(openMinute = 9 * 60, closeMinute = 19 * 60),
        DayHours(openMinute = 9 * 60, closeMinute = 13 * 60),
        DayHours(closed = true),
      )
  }
}

data class Appointment(
  val id: String,
  val clinicId: String,
  val petId: String,
  val petName: String,
  val ownerName: String,
  val ownerPhone: String?,
  val startsAt: Long,
  val endsAt: Long,
  val reason: String,
  val notes: String?,
  val status: AppointmentStatus,
  val consultationId: String?,
  val cancelReason: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val pendingSync: Boolean,
) {
  val durationMinutes: Long
    get() = (endsAt - startsAt) / 60_000L

  fun overlaps(other: Appointment): Boolean =
    startsAt < other.endsAt && endsAt > other.startsAt
}

data class AppointmentDraft(
  val petId: String,
  val startsAt: Long,
  val endsAt: Long,
  val reason: String,
  val notes: String?,
) {
  fun validate(): String? {
    if (petId.isBlank()) return "Elegí una mascota"
    if (reason.isBlank()) return "Completá el motivo del turno"
    if (endsAt <= startsAt) return "La hora de fin debe ser posterior a la de inicio"
    val duration = (endsAt - startsAt) / 60_000L
    if (duration < MIN_DURATION_MINUTES) return "El turno debe durar al menos $MIN_DURATION_MINUTES minutos"
    if (duration > MAX_DURATION_MINUTES) return "El turno no puede durar más de $MAX_DURATION_MINUTES minutos"
    return null
  }

  companion object {
    const val MIN_DURATION_MINUTES = 5L
    const val MAX_DURATION_MINUTES = 8 * 60L
  }
}

data class AppointmentWarning(
  val conflicts: List<Appointment> = emptyList(),
  val outsideBusinessHours: Boolean = false,
)

sealed interface AppointmentSaveResult {
  data class Success(val appointment: Appointment) : AppointmentSaveResult

  data class Warning(val warning: AppointmentWarning) : AppointmentSaveResult

  data class Failure(val message: String) : AppointmentSaveResult
}

fun AppointmentWithPet.toDomain(): Appointment =
  Appointment(
    id = appointment.id,
    clinicId = appointment.clinicId,
    petId = appointment.petId,
    petName = petName,
    ownerName = ownerName,
    ownerPhone = ownerPhone,
    startsAt = appointment.startsAt,
    endsAt = appointment.endsAt,
    reason = appointment.reason,
    notes = appointment.notes,
    status = AppointmentStatus.fromStorage(appointment.status),
    consultationId = appointment.consultationId,
    cancelReason = appointment.cancelReason,
    createdAt = appointment.createdAt,
    updatedAt = appointment.updatedAt,
    revision = appointment.revision,
    pendingSync = appointment.pendingSync,
  )

fun normalizeWhatsAppPhone(raw: String?): String? {
  val digits = raw?.filter { it.isDigit() }.orEmpty()
  if (digits.length < 8) return null
  if (digits.startsWith("54") && digits.length >= 11) return digits
  if (digits.startsWith("0")) return normalizeWhatsAppPhone(digits.drop(1))
  if (digits.length == 11 && digits.startsWith("9")) return "54$digits"
  for (areaLength in 2..4) {
    if (
      digits.length == areaLength + 10 &&
        digits.regionMatches(areaLength, "15", 0, 2)
    ) {
      return "549" + digits.substring(0, areaLength) + digits.substring(areaLength + 2)
    }
  }
  return "549" + digits.takeLast(10)
}

fun buildWhatsAppMessage(appointment: Appointment): String {
  val date = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    .format(ClinicTime.dateOf(appointment.startsAt))
  val time = DateTimeFormatter.ofPattern("HH:mm")
    .format(java.time.Instant.ofEpochMilli(appointment.startsAt).atZone(ClinicTime.zone))
  return "Hola ${appointment.ownerName}, te recordamos el turno de ${appointment.petName} " +
    "el $date a las $time. Motivo: ${appointment.reason}. Si no podés asistir, avisanos."
}

fun buildWhatsAppUrl(appointment: Appointment): String? {
  val phone = normalizeWhatsAppPhone(appointment.ownerPhone) ?: return null
  val message = URLEncoder.encode(buildWhatsAppMessage(appointment), "UTF-8").replace("+", "%20")
  return "https://wa.me/$phone?text=$message"
}
