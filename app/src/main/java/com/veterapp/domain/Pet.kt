package com.veterapp.domain

import com.veterapp.data.local.PetEntity

enum class PetStatus(val storage: String, val label: String) {
  Active("active", "Activa"),
  Inactive("inactive", "Inactiva"),
  Deceased("deceased", "Fallecida"),
  ;

  companion object {
    fun fromStorage(value: String?): PetStatus =
      entries.firstOrNull { it.storage == value } ?: Active
  }
}

data class Pet(
  val id: String,
  val clinicId: String,
  val clientId: String?,
  val name: String,
  val species: String,
  val breed: String?,
  val ownerName: String,
  val notes: String?,
  val fotoUrl: String?,
  val status: PetStatus,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val pendingSync: Boolean,
) {
  val subtitle: String
    get() = listOfNotNull(species, breed?.takeIf { it.isNotBlank() }).joinToString(" · ")
}

data class PetDraft(
  val name: String,
  val species: String,
  val breed: String?,
  val notes: String?,
  val status: PetStatus = PetStatus.Active,
)

fun PetEntity.toDomain(): Pet =
  Pet(
    id = id,
    clinicId = clinicId,
    clientId = clientId,
    name = name,
    species = species,
    breed = breed,
    ownerName = ownerName,
    notes = notes,
    fotoUrl = fotoUrl,
    status = PetStatus.fromStorage(status),
    createdAt = createdAt,
    updatedAt = updatedAt,
    revision = revision,
    pendingSync = pendingSync,
  )
