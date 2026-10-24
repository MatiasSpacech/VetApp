package com.veterapp.domain

import com.veterapp.data.local.ClientEntity

data class Client(
  val id: String,
  val clinicId: String,
  val name: String,
  val phone: String?,
  val document: String?,
  val email: String?,
  val address: String?,
  val notes: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val pendingSync: Boolean,
) {
  val contactLine: String?
    get() =
      listOfNotNull(
        phone?.takeIf { it.isNotBlank() },
        document?.takeIf { it.isNotBlank() },
      )
        .joinToString(" · ")
        .takeIf { it.isNotBlank() }

  val initials: String
    get() =
      name
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
}

data class ClientDraft(
  val name: String,
  val phone: String?,
  val document: String?,
  val email: String?,
  val address: String?,
  val notes: String?,
)

sealed interface ClientSaveResult {
  data class Success(val client: Client) : ClientSaveResult

  data class Duplicate(val matches: List<Client>) : ClientSaveResult

  data class Failure(val message: String) : ClientSaveResult
}

fun normalizePhone(value: String?): String? =
  value?.filter { it.isDigit() }?.takeIf { it.isNotBlank() }

fun normalizeDocument(value: String?): String? =
  value?.filter { it.isLetterOrDigit() }?.uppercase()?.takeIf { it.isNotBlank() }

fun Client.matches(draft: ClientDraft): Boolean {
  val storedPhone = normalizePhone(phone)
  val storedDocument = normalizeDocument(document)
  val phoneMatches = storedPhone != null && storedPhone == normalizePhone(draft.phone)
  val documentMatches = storedDocument != null && storedDocument == normalizeDocument(draft.document)
  return phoneMatches || documentMatches
}

fun ClientEntity.toDomain(): Client =
  Client(
    id = id,
    clinicId = clinicId,
    name = name,
    phone = phone,
    document = document,
    email = email,
    address = address,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    revision = revision,
    pendingSync = pendingSync,
  )
