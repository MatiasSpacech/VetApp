package com.veterapp.data.remote

import com.veterapp.data.local.VaccinationEntity
import com.veterapp.domain.ClinicTime
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VaccinationDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  @SerialName("pet_id") val petId: String,
  @SerialName("vaccine_name") val vaccineName: String,
  @SerialName("applied_on") val appliedOn: String,
  @SerialName("next_due_on") val nextDueOn: String? = null,
  val lot: String? = null,
  val notes: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

fun VaccinationEntity.toDto(): VaccinationDto =
  VaccinationDto(
    id = id,
    clinicId = clinicId,
    petId = petId,
    vaccineName = vaccineName,
    appliedOn = LocalDate.ofEpochDay(appliedOn).toString(),
    nextDueOn = nextDueOn?.let { LocalDate.ofEpochDay(it).toString() },
    lot = lot,
    notes = notes,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
    originDeviceId = originDeviceId,
  )

fun VaccinationDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): VaccinationEntity =
  VaccinationEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    petId = petId,
    vaccineName = vaccineName,
    appliedOn = appliedOn.toEpochDayOrToday(),
    nextDueOn = nextDueOn.toEpochDayOrNull(),
    lot = lot,
    notes = notes,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    deletedAt = deletedAt?.toEpochMillisOrNull(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

internal fun String?.toEpochDayOrToday(): Long =
  toEpochDayOrNull() ?: ClinicTime.todayEpochDay()

internal fun String?.toEpochDayOrNull(): Long? =
  this?.let { value ->
    runCatching { LocalDate.parse(value).toEpochDay() }.getOrNull()
  }
