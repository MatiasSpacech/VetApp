package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PetWeightDao {
  @Query(
    "SELECT * FROM pet_weights WHERE petId = :petId AND deletedAt IS NULL " +
      "ORDER BY measuredAt DESC, createdAt DESC",
  )
  fun observeForPet(petId: String): Flow<List<PetWeightEntity>>

  @Query("SELECT * FROM pet_weights WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): PetWeightEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: PetWeightEntity)

  @Query("SELECT * FROM pet_weights ORDER BY createdAt ASC")
  suspend fun all(): List<PetWeightEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<PetWeightEntity>)

  @Query("DELETE FROM pet_weights")
  suspend fun clear()
}
