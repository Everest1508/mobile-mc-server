package com.example.data.model

data class MinecraftVersion(
    val version: String,
    val releaseType: String = "release",
    val requiredJavaVersion: Int = 21,
    val supportedSoftware: List<ServerSoftware> = listOf(
        ServerSoftware.VANILLA,
        ServerSoftware.PAPER,
        ServerSoftware.FABRIC,
        ServerSoftware.NEOFORGE,
        ServerSoftware.FORGE
    )
) {
    companion object {
        val KNOWN_VERSIONS = listOf(
            MinecraftVersion("1.21.4", "release", 21, listOf(ServerSoftware.VANILLA, ServerSoftware.PAPER, ServerSoftware.FABRIC, ServerSoftware.NEOFORGE)),
            MinecraftVersion("1.21.1", "release", 21, listOf(ServerSoftware.VANILLA, ServerSoftware.PAPER, ServerSoftware.FABRIC, ServerSoftware.NEOFORGE, ServerSoftware.FORGE)),
            MinecraftVersion("1.20.4", "release", 17, listOf(ServerSoftware.VANILLA, ServerSoftware.PAPER, ServerSoftware.FABRIC, ServerSoftware.NEOFORGE, ServerSoftware.FORGE)),
            MinecraftVersion("1.20.1", "release", 17, listOf(ServerSoftware.VANILLA, ServerSoftware.PAPER, ServerSoftware.FABRIC, ServerSoftware.FORGE)),
            MinecraftVersion("1.19.4", "release", 17, listOf(ServerSoftware.VANILLA, ServerSoftware.PAPER, ServerSoftware.FABRIC, ServerSoftware.FORGE)),
            MinecraftVersion("1.18.2", "release", 17, listOf(ServerSoftware.VANILLA, ServerSoftware.PAPER, ServerSoftware.FABRIC, ServerSoftware.FORGE)),
            MinecraftVersion("1.16.5", "release", 8, listOf(ServerSoftware.VANILLA, ServerSoftware.PAPER, ServerSoftware.FABRIC, ServerSoftware.FORGE)),
            MinecraftVersion("1.12.2", "release", 8, listOf(ServerSoftware.VANILLA, ServerSoftware.PAPER, ServerSoftware.FORGE))
        )

        fun getRequiredJava(versionStr: String): Int {
            return when {
                versionStr.startsWith("1.21") || versionStr.startsWith("1.20.5") || versionStr.startsWith("1.20.6") -> 21
                versionStr.startsWith("1.18") || versionStr.startsWith("1.19") || versionStr.startsWith("1.20") -> 17
                versionStr.startsWith("1.17") -> 16
                else -> 8
            }
        }
    }
}
