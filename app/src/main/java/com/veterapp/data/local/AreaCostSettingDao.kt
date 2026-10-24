package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AreaCostSettingDao {
  @Query("SELECT * FROM area_cost_settings ORDER BY validFrom ASC, createdAt ASC")
  fun observeAll(): Flow<List<AreaCostSettingEntity>>

  @Query(
    "SELECT * FROM area_cost_settings WHERE areaId = :areaId AND validFrom <= :date " +
      "ORDER BY validFrom DESC, createdAt DESC LIMIT 1",
  )
  suspend fun effectiveFor(areaId: String, date: String): AreaCostSettingEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: AreaCostSettingEntity)

  @Query("UPDATE area_cost_settings SET pendingSync = 0 WHERE id = :id")
  suspend fun markSynced(id: String)

  @Query("SELECT * FROM area_cost_settings ORDER BY createdAt ASC")
  suspend fun all(): List<AreaCostSettingEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<AreaCostSettingEntity>)

  @Query("DELETE FROM area_cost_settings")
  suspend fun clear()
}
