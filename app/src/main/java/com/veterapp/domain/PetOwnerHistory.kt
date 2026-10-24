package com.veterapp.domain

import com.veterapp.data.local.PetOwnerHistoryEntity

data class PetOwnerHistory(
  val id: String,
  val petId: String,
  val previousClientId: String?,
  val previousOwnerName: String,
  val newClientId: String,
  val newOwnerName: String,
  val reason: String?,
  val actorId: String?,
  val changedAt: Long,
)

fun PetOwnerHistoryEntity.toDomain(): PetOwnerHistory =
  PetOwnerHistory(
    id = id,
    petId = petId,
    previousClientId = previousClientId,
    previousOwnerName = previousOwnerName,
    newClientId = newClientId,
    newOwnerName = newOwnerName,
    reason = reason,
    actorId = actorId,
    changedAt = changedAt,
  )
