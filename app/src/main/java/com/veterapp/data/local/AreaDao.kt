package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AreaDao {
  @Query("SELECT * FROM areas WHERE deletedAt IS NULL AND active = 1 ORDER BY sortOrder, name COLLATE NOCASE")
  fun observeActive(): Flow<List<AreaEntity>>

  @Query("SELECT * FROM areas WHERE deletedAt IS NULL ORDER BY sortOrder, name COLLATE NOCASE")
  fun observeAll(): Flow<List<AreaEntity>>

  @Query("SELECT * FROM areas WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): AreaEntity?

  @Query("SELECT * FROM areas WHERE deletedAt IS NULL AND lower(trim(name)) = lower(trim(:name)) LIMIT 1")
  suspend fun findByName(name: String): AreaEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: AreaEntity)

  @Query("SELECT * FROM areas ORDER BY createdAt ASC")
  suspend fun all(): List<AreaEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<AreaEntity>)

  @Query("DELETE FROM areas")
  suspend fun clear()
}
