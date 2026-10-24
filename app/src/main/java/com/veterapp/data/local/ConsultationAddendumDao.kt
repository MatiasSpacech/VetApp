package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsultationAddendumDao {
  @Query(
    "SELECT a.* FROM consultation_addenda a " +
      "INNER JOIN consultations c ON c.id = a.consultationId " +
      "WHERE c.petId = :petId AND c.deletedAt IS NULL AND a.deletedAt IS NULL " +
      "ORDER BY a.createdAt ASC",
  )
  fun observeForPet(petId: String): Flow<List<ConsultationAddendumEntity>>

  @Query(
    "SELECT * FROM consultation_addenda WHERE consultationId = :consultationId " +
      "AND deletedAt IS NULL ORDER BY createdAt ASC",
  )
  fun observeForConsultation(consultationId: String): Flow<List<ConsultationAddendumEntity>>

  @Query("SELECT * FROM consultation_addenda WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): ConsultationAddendumEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: ConsultationAddendumEntity)

  @Query("SELECT * FROM consultation_addenda ORDER BY createdAt ASC")
  suspend fun all(): List<ConsultationAddendumEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<ConsultationAddendumEntity>)

  @Query("DELETE FROM consultation_addenda")
  suspend fun clear()
}
