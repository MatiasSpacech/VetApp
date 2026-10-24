package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "payments",
  indices = [Index("clinicId"), Index("saleId"), Index("methodId"), Index("pendingSync")],
)
data class PaymentEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val saleId: String,
  val methodId: String,
  val amountCents: Long,
  val tenderedCents: Long?,
  val changeCents: Long?,
  val createdAt: Long,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
