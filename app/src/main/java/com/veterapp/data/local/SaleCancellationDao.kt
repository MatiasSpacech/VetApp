package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SaleCancellationDao {
  @Query("SELECT * FROM sale_cancellations WHERE saleId = :saleId LIMIT 1")
  suspend fun findBySale(saleId: String): SaleCancellationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: SaleCancellationEntity)

  @Query("UPDATE sale_cancellations SET pendingSync = 0 WHERE id = :id")
  suspend fun markSynced(id: String)

  @Query("SELECT * FROM sale_cancellations ORDER BY createdAt ASC")
  suspend fun all(): List<SaleCancellationEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<SaleCancellationEntity>)

  @Query("DELETE FROM sale_cancellations")
  suspend fun clear()
}
