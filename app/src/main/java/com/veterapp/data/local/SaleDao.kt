package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class SaleWithClient(
  @Embedded val sale: SaleEntity,
  val clientPhone: String?,
)

@Dao
interface SaleDao {
  @Query(
    "SELECT s.*, c.phone AS clientPhone FROM sales s " +
      "LEFT JOIN clients c ON c.id = s.clientId " +
      "WHERE s.soldAt >= :from AND s.soldAt < :to " +
      "ORDER BY s.soldAt DESC, s.number DESC",
  )
  fun observeBetween(from: Long, to: Long): Flow<List<SaleWithClient>>

  @Query(
    "SELECT s.*, c.phone AS clientPhone FROM sales s " +
      "LEFT JOIN clients c ON c.id = s.clientId WHERE s.id = :id LIMIT 1",
  )
  suspend fun findByIdWithClient(id: String): SaleWithClient?

  @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): SaleEntity?

  @Query("SELECT COUNT(*) FROM sales WHERE number = :number")
  suspend fun countByNumber(number: String): Int

  @Query("SELECT * FROM sales WHERE status = 'completed' AND soldAt >= :from AND soldAt < :to")
  suspend fun completedBetween(from: Long, to: Long): List<SaleEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: SaleEntity)

  @Query("UPDATE sales SET pendingSync = 0 WHERE id = :id")
  suspend fun markSynced(id: String)

  @Query("SELECT * FROM sales ORDER BY createdAt ASC")
  suspend fun all(): List<SaleEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<SaleEntity>)

  @Query("DELETE FROM sales")
  suspend fun clear()
}
