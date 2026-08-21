package com.example.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backups")
data class BackupEntity(
    @PrimaryKey val id: String,
    val serverId: String,
    val name: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val worldName: String,
    val includesConfigs: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
