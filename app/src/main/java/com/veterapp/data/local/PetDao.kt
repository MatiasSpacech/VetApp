package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
  @Query("SELECT * FROM pets WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
  fun observeActive(): Flow<List<PetEntity>>

  @Query("SELECT * FROM pets WHERE id = :id AND deletedAt IS NULL LIMIT 1")
  fun observeById(id: String): Flow<PetEntity?>

  @Query(
    "SELECT * FROM pets WHERE clientId = :clientId AND deletedAt IS NULL ORDER BY name COLLATE NOCASE",
  )
  fun observeForClient(clientId: String): Flow<List<PetEntity>>

  @Query("SELECT * FROM pets WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): PetEntity?

  @Query("SELECT COUNT(*) FROM pets WHERE clientId = :clientId AND deletedAt IS NULL")
  suspend fun countActiveForClient(clientId: String): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(pet: PetEntity)

  @Query("SELECT * FROM pets ORDER BY createdAt ASC")
  suspend fun all(): List<PetEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(pets: List<PetEntity>)

  @Query("DELETE FROM pets")
  suspend fun clear()
}
