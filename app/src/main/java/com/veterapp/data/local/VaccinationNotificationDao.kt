package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VaccinationNotificationDao {
  @Query("SELECT * FROM vaccination_notifications ORDER BY notifiedAt ASC")
  suspend fun all(): List<VaccinationNotificationEntity>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(item: VaccinationNotificationEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAll(items: List<VaccinationNotificationEntity>)

  @Query("DELETE FROM vaccination_notifications WHERE vaccinationId = :vaccinationId")
  suspend fun deleteForVaccination(vaccinationId: String)

  @Query("DELETE FROM vaccination_notifications")
  suspend fun clear()
}
