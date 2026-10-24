package com.veterapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "categories",
  indices = [Index("clinicId"), Index("parentId"), Index("pendingSync")],
)
data class CategoryEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val name: String,
  val parentId: String?,
  val scope: String,
  val active: Boolean = true,
  val sortOrder: Int = 0,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val deletedAt: Long?,
  val originDeviceId: String,
  val pendingSync: Boolean,
)
