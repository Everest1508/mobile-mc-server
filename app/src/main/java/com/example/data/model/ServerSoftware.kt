package com.example.data.model

enum class ServerSoftware(val displayName: String, val description: String, val supportsMods: Boolean, val supportsPlugins: Boolean) {
    VANILLA("Vanilla", "Official Mojang server software. Pure Minecraft experience.", false, false),
    PAPER("Paper", "High-performance server fork with Bukkit & Spigot plugin support.", false, true),
    FABRIC("Fabric", "Lightweight, modular modding toolchain with fast updates.", true, false),
    NEOFORGE("NeoForge", "Modern community-driven fork of Forge with vast mod ecosystem.", true, false),
    FORGE("Forge", "Traditional Minecraft modding platform with thousands of mods.", true, false);

    companion object {
        fun fromString(name: String): ServerSoftware {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: VANILLA
        }
    }
}
