package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CashMovementDao {
  @Query("SELECT * FROM cash_movements WHERE sessionId = :sessionId ORDER BY createdAt ASC, id ASC")
  fun observeForSession(sessionId: String): Flow<List<CashMovementEntity>>

  @Query("SELECT * FROM cash_movements WHERE sessionId = :sessionId ORDER BY createdAt ASC, id ASC")
  suspend fun forSession(sessionId: String): List<CashMovementEntity>

  @Query("SELECT COALESCE(SUM(amountCents), 0) FROM cash_movements WHERE sessionId = :sessionId")
  suspend fun sumForSession(sessionId: String): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: CashMovementEntity)

  @Query("UPDATE cash_movements SET pendingSync = 0 WHERE id = :id")
  suspend fun markSynced(id: String)

  @Query("UPDATE cash_movements SET pendingSync = 0 WHERE saleId = :saleId")
  suspend fun markSyncedForSale(saleId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<CashMovementEntity>)

  @Query("SELECT * FROM cash_movements ORDER BY createdAt ASC")
  suspend fun all(): List<CashMovementEntity>

  @Query("DELETE FROM cash_movements")
  suspend fun clear()
}
