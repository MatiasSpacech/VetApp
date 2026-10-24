package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "area_cost_settings",
  indices = [Index("clinicId"), Index("areaId"), Index("validFrom"), Index("pendingSync")],
)
data class AreaCostSettingEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val areaId: String,
  val expensesPercentBp: Int,
  val validFrom: String,
  val createdAt: Long,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
