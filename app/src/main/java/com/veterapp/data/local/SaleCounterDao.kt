package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SaleCounterDao {
  @Query("SELECT * FROM sale_counters WHERE clinicId = :clinicId AND day = :day LIMIT 1")
  suspend fun find(clinicId: String, day: String): SaleCounterEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: SaleCounterEntity)

  @Query("SELECT * FROM sale_counters")
  suspend fun all(): List<SaleCounterEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<SaleCounterEntity>)

  @Query("DELETE FROM sale_counters")
  suspend fun clear()
}
