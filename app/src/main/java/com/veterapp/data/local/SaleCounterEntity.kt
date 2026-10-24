package com.veterapp.data.local

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "sale_counters", primaryKeys = ["clinicId", "day"])
data class SaleCounterEntity(
  val clinicId: String,
  val day: String,
  val lastNumber: Int,
  val updatedAt: Long,
)
