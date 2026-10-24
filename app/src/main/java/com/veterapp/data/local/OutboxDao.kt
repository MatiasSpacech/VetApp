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
      "WHEN 'area' THEN 1 " +
      "WHEN 'category' THEN 2 " +
      "WHEN 'payment_method' THEN 3 " +
      "WHEN 'area_cost' THEN 4 " +
      "WHEN 'pet' THEN 5 " +
      "WHEN 'pet_weight' THEN 6 " +
      "WHEN 'consultation' THEN 7 " +
      "WHEN 'consultation_addendum' THEN 8 " +
      "WHEN 'vaccination' THEN 9 " +
      "WHEN 'appointment' THEN 10 " +
      "WHEN 'catalog_item' THEN 11 " +
      "WHEN 'stock_movement' THEN 12 " +
      "WHEN 'cash_session' THEN 13 " +
      "WHEN 'sale' THEN 14 " +
      "WHEN 'sale_cancellation' THEN 15 " +
      "WHEN 'cash_movement' THEN 16 " +
      "ELSE 17 END, " +
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
