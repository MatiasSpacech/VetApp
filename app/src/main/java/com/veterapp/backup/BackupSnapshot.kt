package com.veterapp.backup

import com.veterapp.data.local.AuditEntity
import com.veterapp.data.local.ClientEntity
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.PetEntity
import com.veterapp.data.local.PetOwnerHistoryEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupSnapshot(
  val version: Int = CURRENT_VERSION,
  val exportedAt: Long = System.currentTimeMillis(),
  val appVersion: String = "",
  val clients: List<ClientEntity> = emptyList(),
  val pets: List<PetEntity> = emptyList(),
  val ownerHistory: List<PetOwnerHistoryEntity> = emptyList(),
  val outbox: List<OutboxEntity> = emptyList(),
  val audit: List<AuditEntity> = emptyList(),
) {
  companion object {
    const val CURRENT_VERSION = 1
  }
}
