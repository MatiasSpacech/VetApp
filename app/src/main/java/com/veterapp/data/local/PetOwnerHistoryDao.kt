package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PetOwnerHistoryDao {
  @Query("SELECT * FROM pet_owner_history WHERE petId = :petId ORDER BY changedAt DESC")
  fun observeForPet(petId: String): Flow<List<PetOwnerHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: PetOwnerHistoryEntity)

  @Query("SELECT * FROM pet_owner_history ORDER BY changedAt ASC")
  suspend fun all(): List<PetOwnerHistoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<PetOwnerHistoryEntity>)

  @Query("DELETE FROM pet_owner_history")
  suspend fun clear()
}
