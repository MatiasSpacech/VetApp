package com.veterapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "pets",
  indices = [Index("clinicId"), Index("clientId"), Index("updatedAt"), Index("pendingSync")],
)
data class PetEntity(
  @PrimaryKey val id: String,
  val clinicId: String,
  val name: String,
  val species: String,
  val breed: String?,
  val ownerName: String,
  val notes: String?,
  val fotoUrl: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val revision: Long,
  val deletedAt: Long?,
  val originDeviceId: String,
  val pendingSync: Boolean,
  val clientId: String? = null,
  @ColumnInfo(defaultValue = "active") val status: String = "active",
  val sex: String? = null,
  val birthDate: Long? = null,
  val approxAgeMonths: Int? = null,
  val color: String? = null,
  val markings: String? = null,
  val microchip: String? = null,
  val reproductiveStatus: String? = null,
  val allergies: String? = null,
  val medicalNotes: String? = null,
) {
  companion object {
    const val DEFAULT_STATUS = "active"
  }
}
