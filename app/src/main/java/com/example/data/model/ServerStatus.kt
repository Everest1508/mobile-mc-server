package com.example.data.model

enum class ServerStatus(val label: String) {
    STOPPED("Stopped"),
    INSTALLING("Installing"),
    STARTING("Starting"),
    RUNNING("Online"),
    STOPPING("Stopping"),
    CRASHED("Crashed");

    companion object {
        fun fromString(value: String): ServerStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: STOPPED
        }
    }
}
