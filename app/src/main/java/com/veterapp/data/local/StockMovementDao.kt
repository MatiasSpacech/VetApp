package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
  @Query(
    "SELECT * FROM stock_movements WHERE itemId = :itemId " +
      "ORDER BY createdAt DESC, id DESC",
  )
  fun observeForItem(itemId: String): Flow<List<StockMovementEntity>>

  @Query("SELECT * FROM stock_movements WHERE itemId = :itemId ORDER BY createdAt ASC, id ASC")
  suspend fun allForItem(itemId: String): List<StockMovementEntity>

  @Query("SELECT COALESCE(SUM(quantityDelta), 0) FROM stock_movements WHERE itemId = :itemId")
  suspend fun sumForItem(itemId: String): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: StockMovementEntity)

  @Query("UPDATE stock_movements SET pendingSync = 0 WHERE id = :id")
  suspend fun markSynced(id: String)

  @Query("UPDATE stock_movements SET pendingSync = 0 WHERE saleId = :saleId")
  suspend fun markSyncedForSale(saleId: String)

  @Query("SELECT * FROM stock_movements ORDER BY createdAt ASC")
  suspend fun all(): List<StockMovementEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<StockMovementEntity>)

  @Query("DELETE FROM stock_movements")
  suspend fun clear()
}
