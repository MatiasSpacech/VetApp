package com.veterapp.data.remote

import com.veterapp.data.local.ClientEntity
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientDto(
  val id: String,
  @SerialName("clinic_id") val clinicId: String? = null,
  val name: String,
  val phone: String? = null,
  val document: String? = null,
  val email: String? = null,
  val address: String? = null,
  val notes: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  val revision: Long? = null,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("origin_device_id") val originDeviceId: String? = null,
)

fun ClientEntity.toDto(): ClientDto =
  ClientDto(
    id = id,
    clinicId = clinicId,
    name = name,
    phone = phone,
    document = document,
    email = email,
    address = address,
    notes = notes,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    revision = revision,
    deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
    originDeviceId = originDeviceId,
  )

fun ClientDto.toEntity(fallbackClinicId: String, fallbackDeviceId: String): ClientEntity =
  ClientEntity(
    id = id,
    clinicId = clinicId ?: fallbackClinicId,
    name = name,
    phone = phone,
    document = document,
    email = email,
    address = address,
    notes = notes,
    createdAt = createdAt.toEpochMillisOrNow(),
    updatedAt = updatedAt.toEpochMillisOrNow(),
    revision = revision ?: 1L,
    deletedAt = deletedAt?.toEpochMillisOrNull(),
    originDeviceId = originDeviceId ?: fallbackDeviceId,
    pendingSync = false,
  )
