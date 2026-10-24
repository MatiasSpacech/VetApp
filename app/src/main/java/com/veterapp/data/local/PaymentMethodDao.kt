package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
  @Query(
    "SELECT * FROM payment_methods WHERE deletedAt IS NULL AND active = 1 " +
      "ORDER BY sortOrder, name COLLATE NOCASE",
  )
  fun observeActive(): Flow<List<PaymentMethodEntity>>

  @Query("SELECT * FROM payment_methods WHERE deletedAt IS NULL ORDER BY sortOrder, name COLLATE NOCASE")
  fun observeAll(): Flow<List<PaymentMethodEntity>>

  @Query("SELECT * FROM payment_methods WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): PaymentMethodEntity?

  @Query("SELECT * FROM payment_methods WHERE deletedAt IS NULL AND affectsCash = 1 LIMIT 1")
  suspend fun findCashMethod(): PaymentMethodEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: PaymentMethodEntity)

  @Query("SELECT * FROM payment_methods ORDER BY createdAt ASC")
  suspend fun all(): List<PaymentMethodEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<PaymentMethodEntity>)

  @Query("DELETE FROM payment_methods")
  suspend fun clear()
}
