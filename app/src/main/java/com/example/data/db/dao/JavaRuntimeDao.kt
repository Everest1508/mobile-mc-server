package com.example.data.db.dao

import androidx.room.*
import com.example.data.db.entities.JavaRuntimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JavaRuntimeDao {
    @Query("SELECT * FROM java_runtimes ORDER BY versionNumber DESC")
    fun getAllRuntimes(): Flow<List<JavaRuntimeEntity>>

    @Query("SELECT * FROM java_runtimes WHERE versionNumber = :versionNumber AND arch = :arch")
    suspend fun getRuntimeByVersionAndArch(versionNumber: Int, arch: String): JavaRuntimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuntime(runtime: JavaRuntimeEntity)

    @Delete
    suspend fun deleteRuntime(runtime: JavaRuntimeEntity)
}
