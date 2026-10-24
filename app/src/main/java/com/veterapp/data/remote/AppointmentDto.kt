package com.veterapp.data.remote

import com.veterapp.data.local.AppointmentEntity
import com.veterapp.domain.AppointmentStatus
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  @SerialName("pet_id") val petId: String,
  @SerialName("starts_at") val startsAt: String,
  @SerialName("ends_at") val endsAt: String,
  val status: String? = null,
  val reason: String,
  val notes: String? = null,
  @SerialName("consultation_id") val consultationId: String? = null,
  @SerialName("cancel_reason") val cancelReason: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

fun AppointmentEntity.toDto(): AppointmentDto =
  AppointmentDto(
    id = id,
    clinicId = clinicId,
    petId = petId,
    startsAt = Instant.ofEpochMilli(startsAt).toString(),
    endsAt = Instant.ofEpochMilli(endsAt).toString(),
    status = status,
    reason = reason,
    notes = notes,
    consultationId = consultationId,
    cancelReason = cancelReason,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
    originDeviceId = originDeviceId,
  )

fun AppointmentDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): AppointmentEntity =
  AppointmentEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    petId = petId,
    startsAt = startsAt.toEpochMillisOrNow(),
    endsAt = endsAt.toEpochMillisOrNow(),
    status = AppointmentStatus.fromStorage(status).storage,
    reason = reason,
    notes = notes,
    consultationId = consultationId,
    cancelReason = cancelReason,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    deletedAt = deletedAt?.toEpochMillisOrNull(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )
