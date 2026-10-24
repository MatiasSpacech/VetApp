package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
  @Query("SELECT * FROM payments WHERE saleId = :saleId ORDER BY createdAt ASC, id ASC")
  fun observeForSale(saleId: String): Flow<List<PaymentEntity>>

  @Query("SELECT * FROM payments WHERE saleId = :saleId ORDER BY createdAt ASC, id ASC")
  suspend fun forSale(saleId: String): List<PaymentEntity>

  @Query("SELECT * FROM payments WHERE saleId IN (:saleIds) ORDER BY createdAt ASC")
  suspend fun forSales(saleIds: List<String>): List<PaymentEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<PaymentEntity>)

  @Query("UPDATE payments SET pendingSync = 0 WHERE saleId = :saleId")
  suspend fun markSyncedForSale(saleId: String)

  @Query("SELECT * FROM payments ORDER BY createdAt ASC")
  suspend fun all(): List<PaymentEntity>

  @Query("DELETE FROM payments")
  suspend fun clear()
}
