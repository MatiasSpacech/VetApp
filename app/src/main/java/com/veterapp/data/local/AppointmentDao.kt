package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class AppointmentWithPet(
  @Embedded val appointment: AppointmentEntity,
  val petName: String,
  val petStatus: String,
  val ownerName: String,
  val ownerPhone: String?,
)

@Dao
interface AppointmentDao {
  @Query(
    "SELECT a.*, p.name AS petName, p.status AS petStatus, " +
      "COALESCE(c.name, p.ownerName) AS ownerName, c.phone AS ownerPhone " +
      "FROM appointments a " +
      "INNER JOIN pets p ON p.id = a.petId " +
      "LEFT JOIN clients c ON c.id = p.clientId " +
      "WHERE a.deletedAt IS NULL AND p.deletedAt IS NULL " +
      "AND a.startsAt >= :from AND a.startsAt < :to " +
      "ORDER BY a.startsAt ASC",
  )
  fun observeBetween(from: Long, to: Long): Flow<List<AppointmentWithPet>>

  @Query(
    "SELECT a.*, p.name AS petName, p.status AS petStatus, " +
      "COALESCE(c.name, p.ownerName) AS ownerName, c.phone AS ownerPhone " +
      "FROM appointments a " +
      "INNER JOIN pets p ON p.id = a.petId " +
      "LEFT JOIN clients c ON c.id = p.clientId " +
      "WHERE a.deletedAt IS NULL AND p.deletedAt IS NULL " +
      "AND a.startsAt >= :from AND a.startsAt < :to " +
      "ORDER BY a.startsAt ASC",
  )
  suspend fun between(from: Long, to: Long): List<AppointmentWithPet>

  @Query(
    "SELECT a.*, p.name AS petName, p.status AS petStatus, " +
      "COALESCE(c.name, p.ownerName) AS ownerName, c.phone AS ownerPhone " +
      "FROM appointments a " +
      "INNER JOIN pets p ON p.id = a.petId " +
      "LEFT JOIN clients c ON c.id = p.clientId " +
      "WHERE a.deletedAt IS NULL AND p.deletedAt IS NULL " +
      "AND a.petId = :petId AND a.startsAt >= :from " +
      "ORDER BY a.startsAt ASC",
  )
  fun observeForPetFrom(petId: String, from: Long): Flow<List<AppointmentWithPet>>

  @Query(
    "SELECT a.*, p.name AS petName, p.status AS petStatus, " +
      "COALESCE(c.name, p.ownerName) AS ownerName, c.phone AS ownerPhone " +
      "FROM appointments a " +
      "INNER JOIN pets p ON p.id = a.petId " +
      "LEFT JOIN clients c ON c.id = p.clientId " +
      "WHERE a.deletedAt IS NULL AND p.deletedAt IS NULL " +
      "AND p.clientId = :clientId AND a.startsAt >= :from " +
      "ORDER BY a.startsAt ASC",
  )
  fun observeForClientFrom(clientId: String, from: Long): Flow<List<AppointmentWithPet>>

  @Query(
    "SELECT a.*, p.name AS petName, p.status AS petStatus, " +
      "COALESCE(c.name, p.ownerName) AS ownerName, c.phone AS ownerPhone " +
      "FROM appointments a " +
      "INNER JOIN pets p ON p.id = a.petId " +
      "LEFT JOIN clients c ON c.id = p.clientId " +
      "WHERE a.id = :id AND a.deletedAt IS NULL LIMIT 1",
  )
  suspend fun findByIdWithPet(id: String): AppointmentWithPet?

  @Query(
    "SELECT a.*, p.name AS petName, p.status AS petStatus, " +
      "COALESCE(c.name, p.ownerName) AS ownerName, c.phone AS ownerPhone " +
      "FROM appointments a " +
      "INNER JOIN pets p ON p.id = a.petId " +
      "LEFT JOIN clients c ON c.id = p.clientId " +
      "WHERE a.deletedAt IS NULL AND a.id <> :excludeId " +
      "AND a.status IN ('scheduled', 'confirmed') " +
      "AND a.startsAt < :endsAt AND a.endsAt > :startsAt " +
      "ORDER BY a.startsAt ASC",
  )
  suspend fun overlapping(
    startsAt: Long,
    endsAt: Long,
    excludeId: String,
  ): List<AppointmentWithPet>

  @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): AppointmentEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: AppointmentEntity)

  @Query("SELECT * FROM appointments ORDER BY createdAt ASC")
  suspend fun all(): List<AppointmentEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<AppointmentEntity>)

  @Query("DELETE FROM appointments")
  suspend fun clear()
}
