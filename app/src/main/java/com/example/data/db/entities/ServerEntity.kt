package com.example.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val minecraftVersion: String,
    val serverType: String, // ServerSoftware name
    val directory: String,
    val javaVersion: Int,
    val minRamMb: Int,
    val maxRamMb: Int,
    val port: Int,
    val motd: String,
    val difficulty: String,
    val gamemode: String,
    val onlineMode: Boolean,
    val maxPlayers: Int,
    val pvp: Boolean,
    val allowFlight: Boolean,
    val viewDistance: Int,
    val simulationDistance: Int,
    val spawnProtection: Int,
    val whiteList: Boolean,
    val status: String,
    val autoRestart: Boolean,
    val customJvmArgs: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
