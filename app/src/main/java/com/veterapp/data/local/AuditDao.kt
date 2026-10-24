package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {
  @Query("SELECT * FROM audit_log ORDER BY createdAt DESC, id DESC LIMIT :limit")
  fun observeRecent(limit: Int): Flow<List<AuditEntity>>

  @Query("SELECT * FROM audit_log ORDER BY createdAt ASC, id ASC")
  suspend fun all(): List<AuditEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: AuditEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<AuditEntity>)

  @Query("DELETE FROM audit_log")
  suspend fun clear()
}
