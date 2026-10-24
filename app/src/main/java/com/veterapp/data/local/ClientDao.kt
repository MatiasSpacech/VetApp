package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
  @Query("SELECT * FROM clients WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
  fun observeActive(): Flow<List<ClientEntity>>

  @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
  fun observeById(id: String): Flow<ClientEntity?>

  @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): ClientEntity?

  @Query("SELECT * FROM clients WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
  suspend fun active(): List<ClientEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(client: ClientEntity)

  @Query("SELECT * FROM clients ORDER BY createdAt ASC")
  suspend fun all(): List<ClientEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(clients: List<ClientEntity>)

  @Query("DELETE FROM clients")
  suspend fun clear()
}
