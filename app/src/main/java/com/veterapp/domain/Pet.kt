package com.veterapp.domain

import com.veterapp.data.local.PetEntity
import com.veterapp.data.local.PetWeightEntity
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

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

enum class PetSex(val storage: String, val label: String) {
  Male("male", "Macho"),
  Female("female", "Hembra"),
  Unknown("unknown", "Sin dato"),
  ;

  companion object {
    fun fromStorage(value: String?): PetSex =
      entries.firstOrNull { it.storage == value } ?: Unknown
  }
}

enum class ReproductiveStatus(val storage: String, val label: String) {
  Intact("intact", "Entero/a"),
  Neutered("neutered", "Castrado/a"),
  Unknown("unknown", "Sin dato"),
  ;

  companion object {
    fun fromStorage(value: String?): ReproductiveStatus =
      entries.firstOrNull { it.storage == value } ?: Unknown
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
  val sex: PetSex,
  val birthDate: Long?,
  val approxAgeMonths: Int?,
  val color: String?,
  val markings: String?,
  val microchip: String?,
  val reproductiveStatus: ReproductiveStatus,
  val allergies: String?,
  val medicalNotes: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val pendingSync: Boolean,
) {
  val subtitle: String
    get() = listOfNotNull(species, breed?.takeIf { it.isNotBlank() }).joinToString(" · ")

  val birthDateValue: LocalDate?
    get() = birthDate?.let(LocalDate::ofEpochDay)

  val ageLabel: String?
    get() {
      val birth = birthDateValue
      if (birth != null) return formatAge(Period.between(birth, ClinicTime.today()))
      val months = approxAgeMonths ?: return null
      return formatAge(Period.ofMonths(months.coerceAtLeast(0)))
    }
}

data class PetDraft(
  val name: String,
  val species: String,
  val breed: String?,
  val notes: String?,
  val status: PetStatus = PetStatus.Active,
  val sex: PetSex = PetSex.Unknown,
  val birthDate: Long? = null,
  val approxAgeMonths: Int? = null,
  val color: String? = null,
  val markings: String? = null,
  val microchip: String? = null,
  val reproductiveStatus: ReproductiveStatus = ReproductiveStatus.Unknown,
  val allergies: String? = null,
  val medicalNotes: String? = null,
) {
  fun validate(today: LocalDate = ClinicTime.today()): String? {
    if (name.isBlank()) return "Completá el nombre"
    if (species.isBlank()) return "Completá la especie"
    if (birthDate != null && approxAgeMonths != null) {
      return "Elegí fecha de nacimiento o edad aproximada, no ambas"
    }
    if (birthDate != null && birthDate > today.toEpochDay()) {
      return "La fecha de nacimiento no puede ser futura"
    }
    if (approxAgeMonths != null && approxAgeMonths < 0) {
      return "La edad aproximada no puede ser negativa"
    }
    return null
  }
}

data class PetWeight(
  val id: String,
  val clinicId: String,
  val petId: String,
  val measuredAt: Long,
  val weightGrams: Int,
  val notes: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val pendingSync: Boolean,
) {
  val weightKg: Double
    get() = weightGrams / 1000.0
}

data class PetWeightDraft(
  val measuredOn: Long,
  val weightGrams: Int,
  val notes: String?,
) {
  fun validate(today: LocalDate = ClinicTime.today()): String? {
    if (weightGrams <= 0) return "El peso debe ser mayor que cero"
    if (weightGrams > MAX_WEIGHT_GRAMS) return "El peso ingresado es demasiado alto"
    if (measuredOn > today.toEpochDay()) return "La fecha de medición no puede ser futura"
    return null
  }

  companion object {
    const val MAX_WEIGHT_GRAMS = 300_000
  }
}

fun formatAge(period: Period): String {
  val years = period.years
  val months = period.months
  return when {
    years > 0 && months > 0 -> "$years año(s) y $months mes(es)"
    years > 0 -> "$years año(s)"
    months > 0 -> "$months mes(es)"
    else -> "menos de un mes"
  }
}

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
    sex = PetSex.fromStorage(sex),
    birthDate = birthDate,
    approxAgeMonths = approxAgeMonths,
    color = color,
    markings = markings,
    microchip = microchip,
    reproductiveStatus = ReproductiveStatus.fromStorage(reproductiveStatus),
    allergies = allergies,
    medicalNotes = medicalNotes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    revision = revision,
    pendingSync = pendingSync,
  )

fun PetWeightEntity.toDomain(): PetWeight =
  PetWeight(
    id = id,
    clinicId = clinicId,
    petId = petId,
    measuredAt = measuredAt,
    weightGrams = weightGrams,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    revision = revision,
    pendingSync = pendingSync,
  )

fun formatWeight(grams: Int): String =
  if (grams % 1000 == 0) "${grams / 1000} kg" else String.format(java.util.Locale("es", "AR"), "%.2f kg", grams / 1000.0)

fun formatBirthDate(epochDay: Long?): String? =
  epochDay?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
