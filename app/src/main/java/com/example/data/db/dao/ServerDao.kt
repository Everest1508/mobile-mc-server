package com.example.data.db.dao

import androidx.room.*
import com.example.data.db.entities.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY updatedAt DESC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServerById(id: String): ServerEntity?

    @Query("SELECT * FROM servers WHERE id = :id")
    fun observeServerById(id: String): Flow<ServerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity)

    @Update
    suspend fun updateServer(server: ServerEntity)

    @Query("UPDATE servers SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteServer(server: ServerEntity)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteServerById(id: String)
}
