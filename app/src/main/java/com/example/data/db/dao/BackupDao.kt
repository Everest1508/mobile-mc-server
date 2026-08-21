package com.example.data.db.dao

import androidx.room.*
import com.example.data.db.entities.BackupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {
    @Query("SELECT * FROM backups WHERE serverId = :serverId ORDER BY createdAt DESC")
    fun getBackupsForServer(serverId: String): Flow<List<BackupEntity>>

    @Query("SELECT * FROM backups ORDER BY createdAt DESC")
    fun getAllBackups(): Flow<List<BackupEntity>>

    @Query("SELECT * FROM backups WHERE id = :id")
    suspend fun getBackupById(id: String): BackupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupEntity)

    @Delete
    suspend fun deleteBackup(backup: BackupEntity)

    @Query("DELETE FROM backups WHERE id = :id")
    suspend fun deleteBackupById(id: String)
}
