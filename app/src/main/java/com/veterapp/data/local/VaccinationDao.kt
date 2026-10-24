package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class VaccinationWithPet(
  @Embedded val vaccination: VaccinationEntity,
  val petName: String,
  val petStatus: String,
)

@Dao
interface VaccinationDao {
  @Query(
    "SELECT * FROM vaccinations WHERE petId = :petId AND deletedAt IS NULL " +
      "ORDER BY appliedOn DESC, createdAt DESC",
  )
  fun observeForPet(petId: String): Flow<List<VaccinationEntity>>

  @Query("SELECT * FROM vaccinations WHERE petId = :petId AND deletedAt IS NULL")
  suspend fun allForPet(petId: String): List<VaccinationEntity>

  @Query("SELECT * FROM vaccinations WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): VaccinationEntity?

  @Query(
    "SELECT * FROM vaccinations WHERE petId = :petId AND deletedAt IS NULL " +
      "AND lower(trim(vaccineName)) = lower(trim(:vaccineName)) " +
      "ORDER BY appliedOn DESC, createdAt DESC LIMIT 1",
  )
  suspend fun findLatestFor(petId: String, vaccineName: String): VaccinationEntity?

  @Query(
    "SELECT v.*, p.name AS petName, p.status AS petStatus FROM vaccinations v " +
      "INNER JOIN pets p ON p.id = v.petId " +
      "WHERE v.deletedAt IS NULL AND p.deletedAt IS NULL AND p.status = 'active' " +
      "ORDER BY v.appliedOn ASC, v.createdAt ASC",
  )
  fun observeActiveWithPet(): Flow<List<VaccinationWithPet>>

  @Query(
    "SELECT v.*, p.name AS petName, p.status AS petStatus FROM vaccinations v " +
      "INNER JOIN pets p ON p.id = v.petId " +
      "WHERE v.deletedAt IS NULL AND p.deletedAt IS NULL AND p.status = 'active' " +
      "ORDER BY v.appliedOn ASC, v.createdAt ASC",
  )
  suspend fun activeWithPet(): List<VaccinationWithPet>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: VaccinationEntity)

  @Query("SELECT * FROM vaccinations ORDER BY createdAt ASC")
  suspend fun all(): List<VaccinationEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<VaccinationEntity>)

  @Query("DELETE FROM vaccinations")
  suspend fun clear()
}
