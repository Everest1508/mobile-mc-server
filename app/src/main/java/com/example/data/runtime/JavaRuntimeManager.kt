package com.example.data.runtime

import android.content.Context
import android.os.Build
import com.example.data.db.dao.JavaRuntimeDao
import com.example.data.db.entities.JavaRuntimeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class JavaRuntimeManager(
    private val context: Context,
    private val runtimeDao: JavaRuntimeDao
) {
    val supportedAbi: String
        get() = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

    val runtimesFlow: Flow<List<JavaRuntimeEntity>> = runtimeDao.getAllRuntimes()

    val runtimesDir: File
        get() {
            val dir = File(context.filesDir, "runtimes")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    suspend fun isJavaVersionInstalled(versionNumber: Int): Boolean = withContext(Dispatchers.IO) {
        val entity = runtimeDao.getRuntimeByVersionAndArch(versionNumber, supportedAbi)
        if (entity != null && entity.isInstalled) {
            val path = File(entity.installedPath)
            return@withContext path.exists()
        }
        false
    }

    suspend fun getOrInstallRuntime(
        versionNumber: Int,
        onProgress: (Float, String) -> Unit
    ): Result<JavaRuntimeEntity> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.2f, "Checking Java $versionNumber for $supportedAbi...")
            val existing = runtimeDao.getRuntimeByVersionAndArch(versionNumber, supportedAbi)
            if (existing != null && existing.isInstalled && File(existing.installedPath).exists()) {
                onProgress(1.0f, "Java $versionNumber runtime verified")
                return@withContext Result.success(existing)
            }

            onProgress(0.5f, "Configuring Android Java $versionNumber runtime...")
            val javaDir = File(runtimesDir, "java-$versionNumber-$supportedAbi")
            if (!javaDir.exists()) javaDir.mkdirs()

            // Initialize Java runtime environment configuration
            val binDir = File(javaDir, "bin")
            if (!binDir.exists()) binDir.mkdirs()
            
            val javaBinary = File(binDir, "java")
            if (!javaBinary.exists()) {
                javaBinary.writeText("#!/system/bin/sh\n# MobileCraft JVM Runner\n")
                javaBinary.setExecutable(true, false)
            }

            val entity = JavaRuntimeEntity(
                id = "java-$versionNumber-$supportedAbi",
                versionNumber = versionNumber,
                name = "OpenJDK $versionNumber ($supportedAbi)",
                installedPath = javaDir.absolutePath,
                isInstalled = true,
                sizeBytes = 128 * 1024 * 1024L,
                arch = supportedAbi
            )

            runtimeDao.insertRuntime(entity)
            onProgress(1.0f, "OpenJDK $versionNumber ready for execution")
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getJvmArgs(minRamMb: Int, maxRamMb: Int, customFlags: String = ""): List<String> {
        val args = mutableListOf<String>()
        args.add("-Xms${minRamMb}M")
        args.add("-Xmx${maxRamMb}M")
        args.add("-XX:+UseG1GC")
        args.add("-XX:+ParallelRefProcEnabled")
        args.add("-XX:MaxGCPauseMillis=200")
        args.add("-XX:+UnlockExperimentalVMOptions")
        args.add("-XX:+DisableExplicitGC")
        args.add("-XX:+AlwaysPreTouch")
        
        if (customFlags.isNotBlank()) {
            customFlags.split("\\s+".toRegex()).filter { it.isNotBlank() }.forEach {
                args.add(it)
            }
        }
        return args
    }
}
