package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
  @Query(
    "SELECT * FROM sync_outbox WHERE nextAttemptAt <= :now " +
      "ORDER BY CASE entityType " +
      "WHEN 'client' THEN 0 " +
      "WHEN 'pet' THEN 1 " +
      "WHEN 'consultation' THEN 2 " +
      "WHEN 'consultation_addendum' THEN 3 " +
      "WHEN 'appointment' THEN 4 " +
      "WHEN 'vaccination' THEN 5 " +
      "ELSE 6 END, " +
      "createdAt ASC LIMIT :limit",
  )
  suspend fun due(now: Long, limit: Int): List<OutboxEntity>

  @Query("SELECT COUNT(*) FROM sync_outbox")
  fun observeCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM sync_outbox WHERE entityId = :entityId")
  suspend fun countForEntity(entityId: String): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: OutboxEntity)

  @Update
  suspend fun update(item: OutboxEntity)

  @Query("DELETE FROM sync_outbox WHERE operationId = :operationId")
  suspend fun delete(operationId: String)

  @Query("SELECT * FROM sync_outbox ORDER BY createdAt ASC")
  suspend fun all(): List<OutboxEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<OutboxEntity>)

  @Query("DELETE FROM sync_outbox")
  suspend fun clear()
}
