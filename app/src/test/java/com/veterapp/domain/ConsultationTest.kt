package com.veterapp.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConsultationTest {
  private val today = LocalDate.of(2026, 10, 24)

  @Test
  fun validatesConsultationDraft() {
    assertNull(validDraft().validate(today))
    assertEquals(
      "Completá el motivo de la consulta",
      validDraft().copy(reason = " ").validate(today),
    )
    assertEquals(
      "La fecha de la consulta no puede ser futura",
      validDraft().copy(consultedOn = today.plusDays(1).toEpochDay()).validate(today),
    )
  }

  @Test
  fun validatesAddendumDraft() {
    assertNull(ConsultationAddendumDraft(reason = "Corrección", text = "Diagnóstico ajustado").validate())
    assertEquals(
      "Indicá el motivo de la corrección",
      ConsultationAddendumDraft(reason = "", text = "Texto").validate(),
    )
    assertEquals(
      "Escribí el contenido del addendum",
      ConsultationAddendumDraft(reason = "Motivo", text = " ").validate(),
    )
  }

  @Test
  fun mapsConsultationDateInClinicZone() {
    val consultation =
      Consultation(
        id = "c1",
        clinicId = "clinic",
        petId = "pet",
        consultedAt = ClinicTime.instantAtNoon(today.toEpochDay()),
        reason = "Control",
        exam = null,
        diagnosis = null,
        treatment = null,
        medication = null,
        instructions = null,
        createdAt = 1,
        updatedAt = 1,
        revision = 0,
        pendingSync = false,
      )

    assertEquals(today, consultation.consultedOn)
  }

  private fun validDraft(): ConsultationDraft =
    ConsultationDraft(
      consultedOn = today.toEpochDay(),
      reason = "Control anual",
      exam = null,
      diagnosis = null,
      treatment = null,
      medication = null,
      instructions = null,
    )
}
