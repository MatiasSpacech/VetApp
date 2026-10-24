package com.veterapp.data.remote

import com.veterapp.data.local.PetEntity
import java.time.Instant
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
  val notes: String? = null,
  @SerialName("foto_url") val fotoUrl: String? = null,
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
    notes = notes,
    fotoUrl = fotoUrl,
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
    notes = notes,
    fotoUrl = fotoUrl,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    deletedAt = deletedAt?.toEpochMillisOrNull(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )

private fun String?.toEpochMillisOrNow(): Long = toEpochMillisOrNull() ?: System.currentTimeMillis()

private fun String?.toEpochMillisOrNull(): Long? =
  this?.let { value ->
    runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
  }
