package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsultationDao {
  @Query(
    "SELECT * FROM consultations WHERE petId = :petId AND deletedAt IS NULL " +
      "ORDER BY consultedAt DESC, createdAt DESC",
  )
  fun observeForPet(petId: String): Flow<List<ConsultationEntity>>

  @Query("SELECT * FROM consultations WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): ConsultationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: ConsultationEntity)

  @Query("SELECT * FROM consultations ORDER BY createdAt ASC")
  suspend fun all(): List<ConsultationEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<ConsultationEntity>)

  @Query("DELETE FROM consultations")
  suspend fun clear()
}
