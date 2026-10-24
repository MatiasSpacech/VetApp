package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "payment_methods",
  indices = [Index("clinicId"), Index("pendingSync")],
)
data class PaymentMethodEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val name: String,
  val affectsCash: Boolean,
  val active: Boolean = true,
  val sortOrder: Int = 0,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val deletedAt: Long?,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
