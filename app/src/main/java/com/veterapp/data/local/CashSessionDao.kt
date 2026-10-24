package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CashSessionDao {
  @Query("SELECT * FROM cash_sessions WHERE status = 'open' ORDER BY openedAt DESC LIMIT 1")
  fun observeOpen(): Flow<CashSessionEntity?>

  @Query("SELECT * FROM cash_sessions WHERE status = 'open' ORDER BY openedAt DESC LIMIT 1")
  suspend fun findOpen(): CashSessionEntity?

  @Query("SELECT * FROM cash_sessions WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): CashSessionEntity?

  @Query("SELECT * FROM cash_sessions ORDER BY openedAt DESC")
  fun observeAll(): Flow<List<CashSessionEntity>>

  @Query("SELECT * FROM cash_sessions ORDER BY openedAt DESC")
  suspend fun all(): List<CashSessionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: CashSessionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<CashSessionEntity>)

  @Query("DELETE FROM cash_sessions")
  suspend fun clear()
}
