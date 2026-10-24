package com.veterapp.data.remote

import com.veterapp.data.local.PetEntity
import com.veterapp.data.local.PetWeightEntity
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PetDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  val name: String,
  val species: String,
  val breed: String? = null,
  @SerialName("owner_name") val ownerName: String,
  @SerialName("client_id") val clientId: String? = null,
  val status: String? = null,
  val notes: String? = null,
  @SerialName("foto_url") val fotoUrl: String? = null,
  val sex: String? = null,
  @SerialName("birth_date") val birthDate: String? = null,
  @SerialName("approx_age_months") val approxAgeMonths: Int? = null,
  val color: String? = null,
  val markings: String? = null,
  val microchip: String? = null,
  @SerialName("reproductive_status") val reproductiveStatus: String? = null,
  val allergies: String? = null,
  @SerialName("medical_notes") val medicalNotes: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class PetWeightDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  @SerialName("pet_id") val petId: String,
  @SerialName("measured_at") val measuredAt: String,
  @SerialName("weight_grams") val weightGrams: Int,
  val notes: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

@Serializable
data class ClinicIdDto(
  @SerialName("clinic_id") val clinicId: String,
)

fun PetEntity.toDto(): PetDto =
  PetDto(
    id = id,
    clinicId = clinicId,
    name = name,
    species = species,
    breed = breed,
    ownerName = ownerName,
    clientId = clientId,
    status = status,
    notes = notes,
    fotoUrl = fotoUrl,
    sex = sex,
    birthDate = birthDate?.let { LocalDate.ofEpochDay(it).toString() },
    approxAgeMonths = approxAgeMonths,
    color = color,
    markings = markings,
    microchip = microchip,
    reproductiveStatus = reproductiveStatus,
    allergies = allergies,
    medicalNotes = medicalNotes,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
    originDeviceId = originDeviceId,
  )

fun PetDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): PetEntity =
  PetEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    name = name,
    species = species,
    breed = breed,
    ownerName = ownerName,
    clientId = clientId,
    status = status ?: PetEntity.DEFAULT_STATUS,
    notes = notes,
    fotoUrl = fotoUrl,
    sex = sex,
    birthDate = birthDate.toEpochDayOrNull(),
    approxAgeMonths = approxAgeMonths,
    color = color,
    markings = markings,
    microchip = microchip,
    reproductiveStatus = reproductiveStatus,
    allergies = allergies,
    medicalNotes = medicalNotes,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    deletedAt = deletedAt?.toEpochMillisOrNull(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

fun PetWeightEntity.toDto(): PetWeightDto =
  PetWeightDto(
    id = id,
    clinicId = clinicId,
    petId = petId,
    measuredAt = Instant.ofEpochMilli(measuredAt).toString(),
    weightGrams = weightGrams,
    notes = notes,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
    originDeviceId = originDeviceId,
  )

fun PetWeightDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): PetWeightEntity =
  PetWeightEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    petId = petId,
    measuredAt = measuredAt.toEpochMillisOrNow(),
    weightGrams = weightGrams,
    notes = notes,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    deletedAt = deletedAt?.toEpochMillisOrNull(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

internal fun String?.toEpochMillisOrNow(): Long = toEpochMillisOrNull() ?: System.currentTimeMillis()

internal fun String?.toEpochMillisOrNull(): Long? =
  this?.let { value ->
    runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
  }
