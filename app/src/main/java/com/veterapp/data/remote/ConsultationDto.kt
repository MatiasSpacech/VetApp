package com.veterapp.data.remote

import com.veterapp.data.local.ConsultationAddendumEntity
import com.veterapp.data.local.ConsultationEntity
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConsultationDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  @SerialName("pet_id") val petId: String,
  @SerialName("consulted_at") val consultedAt: String? = null,
  val reason: String,
  val exam: String? = null,
  val diagnosis: String? = null,
  val treatment: String? = null,
  val medication: String? = null,
  val instructions: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class ConsultationAddendumDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  @SerialName("consultation_id") val consultationId: String,
  val reason: String,
  val text: String,
  @SerialName("author_id") val authorId: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

fun ConsultationEntity.toDto(): ConsultationDto =
  ConsultationDto(
    id = id,
    clinicId = clinicId,
    petId = petId,
    consultedAt = Instant.ofEpochMilli(consultedAt).toString(),
    reason = reason,
    exam = exam,
    diagnosis = diagnosis,
    treatment = treatment,
    medication = medication,
    instructions = instructions,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
    originDeviceId = originDeviceId,
  )

fun ConsultationDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): ConsultationEntity =
  ConsultationEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    petId = petId,
    consultedAt = consultedAt.toEpochMillisOrNow(),
    reason = reason,
    exam = exam,
    diagnosis = diagnosis,
    treatment = treatment,
    medication = medication,
    instructions = instructions,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    deletedAt = deletedAt?.toEpochMillisOrNull(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

fun ConsultationAddendumEntity.toDto(): ConsultationAddendumDto =
  ConsultationAddendumDto(
    id = id,
    clinicId = clinicId,
    consultationId = consultationId,
    reason = reason,
    text = text,
    authorId = authorId,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
    originDeviceId = originDeviceId,
  )

fun ConsultationAddendumDto.toEntity(
  fallbackClinicId: String,
  fallbackDeviceId: String,
): ConsultationAddendumEntity =
  ConsultationAddendumEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    consultationId = consultationId,
    reason = reason,
    text = text,
    authorId = authorId,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    deletedAt = deletedAt?.toEpochMillisOrNull(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )
