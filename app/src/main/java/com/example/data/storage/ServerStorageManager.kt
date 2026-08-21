package com.example.data.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.data.model.ModPluginItem
import com.example.data.model.ServerProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ServerStorageManager(private val context: Context) {

    val rootServersDir: File
        get() {
            val dir = File(context.filesDir, "servers")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    val backupsDir: File
        get() {
            val dir = File(context.filesDir, "backups")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun getServerDirectory(serverId: String): File {
        val dir = File(rootServersDir, serverId)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun initializeServerStructure(serverId: String, properties: ServerProperties): File = withContext(Dispatchers.IO) {
        val serverDir = getServerDirectory(serverId)
        
        // Essential subdirectories
        val worldDir = File(serverDir, "world")
        val modsDir = File(serverDir, "mods")
        val pluginsDir = File(serverDir, "plugins")
        val logsDir = File(serverDir, "logs")
        val configDir = File(serverDir, "config")
        
        if (!worldDir.exists()) worldDir.mkdirs()
        if (!modsDir.exists()) modsDir.mkdirs()
        if (!pluginsDir.exists()) pluginsDir.mkdirs()
        if (!logsDir.exists()) logsDir.mkdirs()
        if (!configDir.exists()) configDir.mkdirs()

        // EULA.txt -> eula=true
        val eulaFile = File(serverDir, "eula.txt")
        if (!eulaFile.exists()) {
            eulaFile.writeText("# By changing the setting below to TRUE you are indicating your agreement to the Mojang EULA (https://aka.ms/MinecraftEULA).\neula=true\n")
        }

        // server.properties
        val propsFile = File(serverDir, "server.properties")
        if (!propsFile.exists()) {
            propsFile.writeText(properties.toPropertiesFileContent())
        }

        // Default JSON files
        val opsFile = File(serverDir, "ops.json")
        if (!opsFile.exists()) opsFile.writeText("[]")

        val whitelistFile = File(serverDir, "whitelist.json")
        if (!whitelistFile.exists()) whitelistFile.writeText("[]")

        val bannedPlayersFile = File(serverDir, "banned-players.json")
        if (!bannedPlayersFile.exists()) bannedPlayersFile.writeText("[]")

        val bannedIpsFile = File(serverDir, "banned-ips.json")
        if (!bannedIpsFile.exists()) bannedIpsFile.writeText("[]")

        serverDir
    }

    suspend fun loadServerProperties(serverId: String): ServerProperties = withContext(Dispatchers.IO) {
        val file = File(getServerDirectory(serverId), "server.properties")
        if (file.exists()) {
            ServerProperties.parse(file.readText())
        } else {
            ServerProperties()
        }
    }

    suspend fun saveServerProperties(serverId: String, properties: ServerProperties) = withContext(Dispatchers.IO) {
        val file = File(getServerDirectory(serverId), "server.properties")
        file.writeText(properties.toPropertiesFileContent())
    }

    suspend fun listServerFiles(serverId: String, relativePath: String = ""): List<FileItem> = withContext(Dispatchers.IO) {
        val serverDir = getServerDirectory(serverId)
        val targetDir = if (relativePath.isEmpty()) serverDir else File(serverDir, relativePath)

        // Prevent path traversal
        if (!targetDir.canonicalPath.startsWith(serverDir.canonicalPath)) {
            throw SecurityException("Access denied: Path traversal detected")
        }

        if (!targetDir.exists() || !targetDir.isDirectory) return@withContext emptyList()

        val files = targetDir.listFiles() ?: arrayOf()
        files.map { file ->
            val rel = file.relativeTo(serverDir).path
            FileItem(
                name = file.name,
                relativePath = rel,
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isDirectory) calculateFolderSize(file) else file.length(),
                lastModified = file.lastModified()
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun readTextFile(serverId: String, relativePath: String): String = withContext(Dispatchers.IO) {
        val serverDir = getServerDirectory(serverId)
        val targetFile = File(serverDir, relativePath)
        if (!targetFile.canonicalPath.startsWith(serverDir.canonicalPath)) {
            throw SecurityException("Access denied: Path traversal detected")
        }
        if (!targetFile.exists()) throw NoSuchFileException(targetFile)
        targetFile.readText()
    }

    suspend fun writeTextFile(serverId: String, relativePath: String, content: String) = withContext(Dispatchers.IO) {
        val serverDir = getServerDirectory(serverId)
        val targetFile = File(serverDir, relativePath)
        if (!targetFile.canonicalPath.startsWith(serverDir.canonicalPath)) {
            throw SecurityException("Access denied: Path traversal detected")
        }
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(content)
    }

    suspend fun deleteFileOrDirectory(serverId: String, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val serverDir = getServerDirectory(serverId)
        val target = File(serverDir, relativePath)
        if (!target.canonicalPath.startsWith(serverDir.canonicalPath)) {
            throw SecurityException("Access denied: Path traversal detected")
        }
        if (target == serverDir) {
            throw IllegalArgumentException("Cannot delete server root directly")
        }
        target.deleteRecursively()
    }

    suspend fun createDirectory(serverId: String, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val serverDir = getServerDirectory(serverId)
        val target = File(serverDir, relativePath)
        if (!target.canonicalPath.startsWith(serverDir.canonicalPath)) {
            throw SecurityException("Access denied: Path traversal detected")
        }
        target.mkdirs()
    }

    suspend fun listModsAndPlugins(serverId: String, isPlugin: Boolean): List<ModPluginItem> = withContext(Dispatchers.IO) {
        val serverDir = getServerDirectory(serverId)
        val folder = File(serverDir, if (isPlugin) "plugins" else "mods")
        if (!folder.exists()) folder.mkdirs()

        val files = folder.listFiles() ?: return@withContext emptyList()
        files.filter { it.isFile && (it.name.endsWith(".jar") || it.name.endsWith(".jar.disabled")) }
            .map { file ->
                val isEnabled = file.name.endsWith(".jar")
                val cleanName = if (isEnabled) file.name.removeSuffix(".jar") else file.name.removeSuffix(".jar.disabled")
                ModPluginItem(
                    fileName = file.name,
                    name = cleanName,
                    version = "1.0",
                    isEnabled = isEnabled,
                    isPlugin = isPlugin,
                    sizeBytes = file.length(),
                    description = if (isPlugin) "Bukkit/Paper Plugin" else "Fabric/Forge Mod Jar"
                )
            }.sortedBy { it.name.lowercase() }
    }

    suspend fun toggleModOrPlugin(serverId: String, fileName: String, isPlugin: Boolean, enable: Boolean): Boolean = withContext(Dispatchers.IO) {
        val folder = File(getServerDirectory(serverId), if (isPlugin) "plugins" else "mods")
        val currentFile = File(folder, fileName)
        if (!currentFile.exists()) return@withContext false

        val newFile = if (enable) {
            if (fileName.endsWith(".jar.disabled")) {
                File(folder, fileName.removeSuffix(".disabled"))
            } else currentFile
        } else {
            if (fileName.endsWith(".jar")) {
                File(folder, "$fileName.disabled")
            } else currentFile
        }

        if (currentFile != newFile) {
            currentFile.renameTo(newFile)
        } else true
    }

    suspend fun createWorldBackup(serverId: String, backupName: String, includeConfigs: Boolean): File = withContext(Dispatchers.IO) {
        val serverDir = getServerDirectory(serverId)
        val timestamp = System.currentTimeMillis()
        val cleanName = backupName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val zipFile = File(backupsDir, "${serverId}_${cleanName}_$timestamp.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val pathsToInclude = mutableListOf("world", "server.properties", "eula.txt", "ops.json", "whitelist.json")
            if (includeConfigs) {
                pathsToInclude.addAll(listOf("config", "mods", "plugins"))
            }

            for (relPath in pathsToInclude) {
                val fileOrDir = File(serverDir, relPath)
                if (fileOrDir.exists()) {
                    addFileToZip(fileOrDir, relPath, zos)
                }
            }
        }
        zipFile
    }

    suspend fun restoreBackup(serverId: String, backupFile: File) = withContext(Dispatchers.IO) {
        val serverDir = getServerDirectory(serverId)
        ZipInputStream(FileInputStream(backupFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val outFile = File(serverDir, entry.name)
                // Check traversal
                if (!outFile.canonicalPath.startsWith(serverDir.canonicalPath)) {
                    throw SecurityException("Security violation in ZIP: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun addFileToZip(fileToZip: File, entryName: String, zos: ZipOutputStream) {
        if (fileToZip.isDirectory) {
            val children = fileToZip.listFiles() ?: return
            for (child in children) {
                addFileToZip(child, "$entryName/${child.name}", zos)
            }
        } else {
            FileInputStream(fileToZip).use { fis ->
                val zipEntry = ZipEntry(entryName)
                zos.putNextEntry(zipEntry)
                fis.copyTo(zos)
                zos.closeEntry()
            }
        }
    }

    fun calculateFolderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateFolderSize(file) else file.length()
        }
        return size
    }

    fun getStorageTelemetry(): StorageTelemetry {
        val statFs = StatFs(context.filesDir.path)
        val totalBytes = statFs.totalBytes
        val availableBytes = statFs.availableBytes
        val usedBytes = totalBytes - availableBytes
        val appServerBytes = calculateFolderSize(rootServersDir)

        return StorageTelemetry(
            totalBytes = totalBytes,
            availableBytes = availableBytes,
            usedBytes = usedBytes,
            appServerBytes = appServerBytes
        )
    }

    suspend fun deleteServerDirectory(serverId: String) = withContext(Dispatchers.IO) {
        val dir = File(rootServersDir, serverId)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }
}

data class FileItem(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)

data class StorageTelemetry(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val appServerBytes: Long
) {
    val availableGb: Float get() = (availableBytes / (1024f * 1024f * 1024f))
    val totalGb: Float get() = (totalBytes / (1024f * 1024f * 1024f))
    val usedGb: Float get() = (usedBytes / (1024f * 1024f * 1024f))
}
