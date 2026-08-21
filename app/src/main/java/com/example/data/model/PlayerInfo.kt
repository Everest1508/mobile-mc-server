package com.example.data.model

data class PlayerInfo(
    val name: String,
    val uuid: String = "",
    val isOnline: Boolean = false,
    val isOp: Boolean = false,
    val isWhitelisted: Boolean = false,
    val isBanned: Boolean = false,
    val pingMs: Int = 0
)

data class ModPluginItem(
    val fileName: String,
    val name: String,
    val version: String,
    val isEnabled: Boolean,
    val isPlugin: Boolean, // true for Paper plugin, false for mod (.jar)
    val sizeBytes: Long,
    val description: String = ""
)

data class ServerLogEntry(
    val timestamp: String,
    val level: LogLevel,
    val thread: String,
    val message: String,
    val raw: String
)

enum class LogLevel {
    INFO,
    WARN,
    ERROR,
    DEBUG
}

data class ServerStats(
    val cpuUsagePercent: Float = 0f,
    val ramUsedMb: Long = 0,
    val ramMaxMb: Long = 0,
    val storageUsedBytes: Long = 0,
    val uptimeSeconds: Long = 0,
    val activePlayersCount: Int = 0,
    val tps: Double = 20.0
)
