package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogItemDao {
  @Query("SELECT * FROM catalog_items WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
  fun observeAll(): Flow<List<CatalogItemEntity>>

  @Query("SELECT * FROM catalog_items WHERE deletedAt IS NULL AND active = 1 ORDER BY name COLLATE NOCASE")
  fun observeActive(): Flow<List<CatalogItemEntity>>

  @Query("SELECT * FROM catalog_items WHERE id = :id LIMIT 1")
  fun observeById(id: String): Flow<CatalogItemEntity?>

  @Query("SELECT * FROM catalog_items WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): CatalogItemEntity?

  @Query("SELECT * FROM catalog_items WHERE deletedAt IS NULL AND lower(trim(internalCode)) = lower(trim(:code)) LIMIT 1")
  suspend fun findByInternalCode(code: String): CatalogItemEntity?

  @Query("SELECT * FROM catalog_items WHERE deletedAt IS NULL AND barcode = :barcode LIMIT 1")
  suspend fun findByBarcode(barcode: String): CatalogItemEntity?

  @Query("SELECT COUNT(*) FROM catalog_items WHERE areaId = :areaId AND deletedAt IS NULL AND active = 1")
  suspend fun countActiveForArea(areaId: String): Int

  @Query("SELECT COUNT(*) FROM catalog_items WHERE categoryId = :categoryId AND deletedAt IS NULL AND active = 1")
  suspend fun countActiveForCategory(categoryId: String): Int

  @Query("SELECT COUNT(*) FROM catalog_items WHERE categoryId = :categoryId")
  suspend fun countForCategory(categoryId: String): Int

  @Query("SELECT COUNT(*) FROM catalog_items WHERE areaId = :areaId")
  suspend fun countForArea(areaId: String): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: CatalogItemEntity)

  @Query("SELECT * FROM catalog_items ORDER BY createdAt ASC")
  suspend fun all(): List<CatalogItemEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<CatalogItemEntity>)

  @Query("DELETE FROM catalog_items")
  suspend fun clear()
}
