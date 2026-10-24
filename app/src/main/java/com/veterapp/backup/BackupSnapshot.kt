package com.veterapp.backup

import com.veterapp.data.local.AppointmentEntity
import com.veterapp.data.local.AuditEntity
import com.veterapp.data.local.ClientEntity
import com.veterapp.data.local.ConsultationAddendumEntity
import com.veterapp.data.local.ConsultationEntity
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.PetEntity
import com.veterapp.data.local.PetOwnerHistoryEntity
import com.veterapp.data.local.VaccinationEntity
import com.veterapp.data.local.VaccinationNotificationEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupSnapshot(
  val version: Int = CURRENT_VERSION,
  val exportedAt: Long = System.currentTimeMillis(),
  val appVersion: String = "",
  val clients: List<ClientEntity> = emptyList(),
  val pets: List<PetEntity> = emptyList(),
  val ownerHistory: List<PetOwnerHistoryEntity> = emptyList(),
  val consultations: List<ConsultationEntity> = emptyList(),
  val consultationAddenda: List<ConsultationAddendumEntity> = emptyList(),
  val vaccinations: List<VaccinationEntity> = emptyList(),
  val vaccinationNotifications: List<VaccinationNotificationEntity> = emptyList(),
  val appointments: List<AppointmentEntity> = emptyList(),
  val outbox: List<OutboxEntity> = emptyList(),
  val audit: List<AuditEntity> = emptyList(),
) {
  companion object {
    const val CURRENT_VERSION = 3
    const val MIN_VERSION = 1
  }
}
