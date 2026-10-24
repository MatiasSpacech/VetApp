package com.veterapp.domain

import com.veterapp.data.local.ConsultationAddendumEntity
import com.veterapp.data.local.ConsultationEntity
import java.time.LocalDate

data class Consultation(
  val id: String,
  val clinicId: String,
  val petId: String,
  val consultedAt: Long,
  val reason: String,
  val exam: String?,
  val diagnosis: String?,
  val treatment: String?,
  val medication: String?,
  val instructions: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val pendingSync: Boolean,
) {
  val consultedOn: LocalDate
    get() = ClinicTime.dateOf(consultedAt)
}

data class ConsultationDraft(
  val consultedOn: Long,
  val reason: String,
  val exam: String?,
  val diagnosis: String?,
  val treatment: String?,
  val medication: String?,
  val instructions: String?,
) {
  fun validate(today: LocalDate = ClinicTime.today()): String? {
    if (reason.isBlank()) return "Completá el motivo de la consulta"
    if (consultedOn > today.toEpochDay()) return "La fecha de la consulta no puede ser futura"
    return null
  }
}

data class ConsultationAddendum(
  val id: String,
  val clinicId: String,
  val consultationId: String,
  val reason: String,
  val text: String,
  val authorId: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val pendingSync: Boolean,
)

data class ConsultationAddendumDraft(
  val reason: String,
  val text: String,
) {
  fun validate(): String? {
    if (reason.isBlank()) return "Indicá el motivo de la corrección"
    if (text.isBlank()) return "Escribí el contenido del addendum"
    return null
  }
}

fun ConsultationEntity.toDomain(): Consultation =
  Consultation(
    id = id,
    clinicId = clinicId,
    petId = petId,
    consultedAt = consultedAt,
    reason = reason,
    exam = exam,
    diagnosis = diagnosis,
    treatment = treatment,
    medication = medication,
    instructions = instructions,
    createdAt = createdAt,
    updatedAt = updatedAt,
    revision = revision,
    pendingSync = pendingSync,
  )

fun ConsultationAddendumEntity.toDomain(): ConsultationAddendum =
  ConsultationAddendum(
    id = id,
    clinicId = clinicId,
    consultationId = consultationId,
    reason = reason,
    text = text,
    authorId = authorId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    revision = revision,
    pendingSync = pendingSync,
  )
