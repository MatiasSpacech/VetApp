package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleItemDao {
  @Query("SELECT * FROM sale_items WHERE saleId = :saleId ORDER BY createdAt ASC, id ASC")
  fun observeForSale(saleId: String): Flow<List<SaleItemEntity>>

  @Query("SELECT * FROM sale_items WHERE saleId = :saleId ORDER BY createdAt ASC, id ASC")
  suspend fun forSale(saleId: String): List<SaleItemEntity>

  @Query("SELECT * FROM sale_items WHERE saleId IN (:saleIds) ORDER BY createdAt ASC")
  suspend fun forSales(saleIds: List<String>): List<SaleItemEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<SaleItemEntity>)

  @Query("UPDATE sale_items SET pendingSync = 0 WHERE saleId = :saleId")
  suspend fun markSyncedForSale(saleId: String)

  @Query("SELECT * FROM sale_items ORDER BY createdAt ASC")
  suspend fun all(): List<SaleItemEntity>

  @Query("DELETE FROM sale_items")
  suspend fun clear()
}
