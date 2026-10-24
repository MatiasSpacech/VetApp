package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
  @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY sortOrder, name COLLATE NOCASE")
  fun observeActive(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): CategoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: CategoryEntity)

  @Query("SELECT * FROM categories ORDER BY createdAt ASC")
  suspend fun all(): List<CategoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<CategoryEntity>)

  @Query("DELETE FROM categories")
  suspend fun clear()
}
