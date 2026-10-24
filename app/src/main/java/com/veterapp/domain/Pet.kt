package com.veterapp.domain

import com.veterapp.data.local.PetEntity

data class Pet(
  val id: String,
  val clinicId: String,
  val name: String,
  val species: String,
  val breed: String?,
  val ownerName: String,
  val notes: String?,
  val fotoUrl: String?,
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
  val ownerName: String,
  val notes: String?,
)

fun PetEntity.toDomain(): Pet =
  Pet(
    id = id,
    clinicId = clinicId,
    name = name,
    species = species,
    breed = breed,
    ownerName = ownerName,
    notes = notes,
    fotoUrl = fotoUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
    revision = revision,
    pendingSync = pendingSync,
  )
