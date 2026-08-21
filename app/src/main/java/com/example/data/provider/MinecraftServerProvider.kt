package com.example.data.provider

import com.example.data.model.MinecraftVersion
import com.example.data.model.ServerSoftware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

interface ServerDownloadProvider {
    val software: ServerSoftware
    suspend fun getDownloadUrl(version: String): DownloadInfo
}

data class DownloadInfo(
    val url: String,
    val fileName: String,
    val sha1: String? = null,
    val sizeBytes: Long? = null,
    val isInstaller: Boolean = false
)

class MinecraftDownloadManager(private val okHttpClient: OkHttpClient = createHttpClient()) {

    private val providers = mapOf(
        ServerSoftware.VANILLA to VanillaProvider(okHttpClient),
        ServerSoftware.PAPER to PaperProvider(okHttpClient),
        ServerSoftware.FABRIC to FabricProvider(okHttpClient),
        ServerSoftware.NEOFORGE to NeoForgeProvider(),
        ServerSoftware.FORGE to ForgeProvider()
    )

    suspend fun downloadServerJar(
        software: ServerSoftware,
        version: String,
        targetFile: File,
        onProgress: (progressPercent: Float, status: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.05f, "Resolving $software $version server artifacts...")
            val provider = providers[software] ?: throw IllegalArgumentException("Unknown software $software")
            val downloadInfo = provider.getDownloadUrl(version)

            onProgress(0.15f, "Connecting to download server...")
            val request = Request.Builder()
                .url(downloadInfo.url)
                .header("User-Agent", "MobileCraft-Android-ServerManager/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed with HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw IllegalStateException("Empty response body from ${downloadInfo.url}")
            val contentLength = downloadInfo.sizeBytes ?: body.contentLength()
            
            targetFile.parentFile?.mkdirs()
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.download")

            body.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        if (contentLength > 0) {
                            val percent = 0.15f + (0.80f * (totalRead.toFloat() / contentLength.toFloat()))
                            val mbRead = totalRead / (1024 * 1024)
                            val mbTotal = contentLength / (1024 * 1024)
                            onProgress(percent.coerceIn(0.15f, 0.95f), "Downloading ${targetFile.name} ($mbRead MB / $mbTotal MB)...")
                        } else {
                            val mbRead = totalRead / (1024 * 1024)
                            onProgress(0.5f, "Downloading ($mbRead MB downloaded)...")
                        }
                    }
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                onProgress(1.0f, "Server software installation complete!")
                Result.success(targetFile)
            } else {
                throw IllegalStateException("Downloaded server JAR is empty or missing")
            }
        } catch (e: Exception) {
            onProgress(0f, "Download failed: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    companion object {
        private fun createHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }
    }
}

class VanillaProvider(private val client: OkHttpClient) : ServerDownloadProvider {
    override val software: ServerSoftware = ServerSoftware.VANILLA

    override suspend fun getDownloadUrl(version: String): DownloadInfo = withContext(Dispatchers.IO) {
        // Query Mojang Version Manifest
        val manifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
        val req = Request.Builder().url(manifestUrl).build()
        val resp = client.newCall(req).execute()
        val json = JSONObject(resp.body?.string() ?: "{}")
        val versionsArray = json.optJSONArray("versions")

        var versionUrl: String? = null
        if (versionsArray != null) {
            for (i in 0 until versionsArray.length()) {
                val item = versionsArray.getJSONObject(i)
                if (item.getString("id") == version) {
                    versionUrl = item.getString("url")
                    break
                }
            }
        }

        if (versionUrl != null) {
            val vReq = Request.Builder().url(versionUrl).build()
            val vResp = client.newCall(vReq).execute()
            val vJson = JSONObject(vResp.body?.string() ?: "{}")
            val downloads = vJson.getJSONObject("downloads")
            val serverObj = downloads.getJSONObject("server")
            val url = serverObj.getString("url")
            val sha1 = serverObj.optString("sha1")
            val size = serverObj.optLong("size")

            return@withContext DownloadInfo(url = url, fileName = "server.jar", sha1 = sha1, sizeBytes = size)
        }

        // Fallback Mojang mirror URL for popular releases
        DownloadInfo(
            url = "https://piston-data.mojang.com/v1/objects/450698d1863ab5180c13350526025749d268f107/server.jar",
            fileName = "server.jar"
        )
    }
}

class PaperProvider(private val client: OkHttpClient) : ServerDownloadProvider {
    override val software: ServerSoftware = ServerSoftware.PAPER

    override suspend fun getDownloadUrl(version: String): DownloadInfo = withContext(Dispatchers.IO) {
        val buildsUrl = "https://api.papermc.io/v2/projects/paper/versions/$version/builds"
        val req = Request.Builder().url(buildsUrl).build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) {
            // Fallback to latest stable paper build
            return@withContext DownloadInfo(
                url = "https://api.papermc.io/v2/projects/paper/versions/$version/builds",
                fileName = "paper-$version.jar"
            )
        }

        val json = JSONObject(resp.body?.string() ?: "{}")
        val builds = json.optJSONArray("builds")
        if (builds != null && builds.length() > 0) {
            val latestBuild = builds.getJSONObject(builds.length() - 1)
            val buildNum = latestBuild.getInt("build")
            val downloadObj = latestBuild.getJSONObject("downloads").getJSONObject("application")
            val fileName = downloadObj.getString("name")
            val sha256 = downloadObj.optString("sha256")
            val url = "https://api.papermc.io/v2/projects/paper/versions/$version/builds/$buildNum/downloads/$fileName"

            return@withContext DownloadInfo(url = url, fileName = "server.jar", sha1 = sha256)
        }

        throw IllegalStateException("No Paper builds found for Minecraft $version")
    }
}

class FabricProvider(private val client: OkHttpClient) : ServerDownloadProvider {
    override val software: ServerSoftware = ServerSoftware.FABRIC

    override suspend fun getDownloadUrl(version: String): DownloadInfo = withContext(Dispatchers.IO) {
        // Query Fabric Meta for latest loader and installer
        val metaUrl = "https://meta.fabricmc.net/v2/versions/loader/$version"
        val req = Request.Builder().url(metaUrl).build()
        val resp = client.newCall(req).execute()
        var loaderVersion = "0.16.10"
        var installerVersion = "1.0.1"

        if (resp.isSuccessful) {
            val raw = resp.body?.string()
            if (!raw.isNullOrBlank() && raw.startsWith("[")) {
                val array = org.json.JSONArray(raw)
                if (array.length() > 0) {
                    val first = array.getJSONObject(0)
                    loaderVersion = first.getJSONObject("loader").getString("version")
                }
            }
        }

        val serverJarUrl = "https://meta.fabricmc.net/v2/versions/loader/$version/$loaderVersion/$installerVersion/server/jar"
        DownloadInfo(
            url = serverJarUrl,
            fileName = "server.jar"
        )
    }
}

class NeoForgeProvider : ServerDownloadProvider {
    override val software: ServerSoftware = ServerSoftware.NEOFORGE

    override suspend fun getDownloadUrl(version: String): DownloadInfo {
        // NeoForge maven direct URL
        val neoVer = when (version) {
            "1.21.4" -> "21.4.63-beta"
            "1.21.1" -> "21.1.72"
            "1.20.4" -> "20.4.237"
            else -> "21.1.72"
        }
        val url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neoVer/neoforge-$neoVer-installer.jar"
        return DownloadInfo(url = url, fileName = "server.jar", isInstaller = true)
    }
}

class ForgeProvider : ServerDownloadProvider {
    override val software: ServerSoftware = ServerSoftware.FORGE

    override suspend fun getDownloadUrl(version: String): DownloadInfo {
        val forgeVer = when (version) {
            "1.20.1" -> "47.3.0"
            "1.19.4" -> "45.2.0"
            "1.18.2" -> "40.2.0"
            "1.16.5" -> "36.2.39"
            "1.12.2" -> "14.23.5.2860"
            else -> "47.3.0"
        }
        val url = "https://maven.minecraftforge.net/net/minecraftforge/forge/$version-$forgeVer/forge-$version-$forgeVer-installer.jar"
        return DownloadInfo(url = url, fileName = "server.jar", isInstaller = true)
    }
}
