package com.example.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "java_runtimes")
data class JavaRuntimeEntity(
    @PrimaryKey val id: String,
    val versionNumber: Int,
    val name: String,
    val installedPath: String,
    val isInstalled: Boolean,
    val sizeBytes: Long,
    val arch: String,
    val downloadedAt: Long = System.currentTimeMillis()
)
