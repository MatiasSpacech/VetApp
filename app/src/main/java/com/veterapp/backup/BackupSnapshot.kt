package com.veterapp.backup

import com.veterapp.data.local.AreaCostSettingEntity
import com.veterapp.data.local.AreaEntity
import com.veterapp.data.local.AppointmentEntity
import com.veterapp.data.local.AuditEntity
import com.veterapp.data.local.CashMovementEntity
import com.veterapp.data.local.CashSessionEntity
import com.veterapp.data.local.CatalogItemEntity
import com.veterapp.data.local.CategoryEntity
import com.veterapp.data.local.ClientEntity
import com.veterapp.data.local.ConsultationAddendumEntity
import com.veterapp.data.local.ConsultationEntity
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.PaymentEntity
import com.veterapp.data.local.PaymentMethodEntity
import com.veterapp.data.local.PetEntity
import com.veterapp.data.local.PetOwnerHistoryEntity
import com.veterapp.data.local.PetWeightEntity
import com.veterapp.data.local.SaleCancellationEntity
import com.veterapp.data.local.SaleCounterEntity
import com.veterapp.data.local.SaleEntity
import com.veterapp.data.local.SaleItemEntity
import com.veterapp.data.local.StockMovementEntity
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
  val petWeights: List<PetWeightEntity> = emptyList(),
  val ownerHistory: List<PetOwnerHistoryEntity> = emptyList(),
  val consultations: List<ConsultationEntity> = emptyList(),
  val consultationAddenda: List<ConsultationAddendumEntity> = emptyList(),
  val vaccinations: List<VaccinationEntity> = emptyList(),
  val vaccinationNotifications: List<VaccinationNotificationEntity> = emptyList(),
  val appointments: List<AppointmentEntity> = emptyList(),
  val areas: List<AreaEntity> = emptyList(),
  val categories: List<CategoryEntity> = emptyList(),
  val catalogItems: List<CatalogItemEntity> = emptyList(),
  val stockMovements: List<StockMovementEntity> = emptyList(),
  val paymentMethods: List<PaymentMethodEntity> = emptyList(),
  val sales: List<SaleEntity> = emptyList(),
  val saleItems: List<SaleItemEntity> = emptyList(),
  val payments: List<PaymentEntity> = emptyList(),
  val cashSessions: List<CashSessionEntity> = emptyList(),
  val cashMovements: List<CashMovementEntity> = emptyList(),
  val saleCancellations: List<SaleCancellationEntity> = emptyList(),
  val saleCounters: List<SaleCounterEntity> = emptyList(),
  val areaCostSettings: List<AreaCostSettingEntity> = emptyList(),
  val outbox: List<OutboxEntity> = emptyList(),
  val audit: List<AuditEntity> = emptyList(),
) {
  companion object {
    const val CURRENT_VERSION = 6
    const val MIN_VERSION = 1
  }
}
