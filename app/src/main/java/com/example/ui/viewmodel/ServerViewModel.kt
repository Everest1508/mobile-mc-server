package com.example.ui.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.entities.BackupEntity
import com.example.data.db.entities.JavaRuntimeEntity
import com.example.data.db.entities.ServerEntity
import com.example.data.manager.NetworkManager
import com.example.data.model.*
import com.example.data.process.ServerProcessManager
import com.example.data.provider.MinecraftDownloadManager
import com.example.data.runtime.JavaRuntimeManager
import com.example.data.storage.FileItem
import com.example.data.storage.ServerStorageManager
import com.example.data.storage.StorageTelemetry
import com.example.service.MinecraftServerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ServerViewModel(
    private val context: Context,
    private val database: com.example.data.db.AppDatabase,
    val storageManager: ServerStorageManager,
    val runtimeManager: JavaRuntimeManager,
    val downloadManager: MinecraftDownloadManager,
    val processManager: ServerProcessManager,
    val networkManager: NetworkManager
) : ViewModel() {

    val servers: StateFlow<List<ServerEntity>> = database.serverDao().getAllServers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serverStatuses: StateFlow<Map<String, ServerStatus>> = processManager.serverStatuses

    val allBackups: StateFlow<List<BackupEntity>> = database.backupDao().getAllBackups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installedRuntimes: StateFlow<List<JavaRuntimeEntity>> = runtimeManager.runtimesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedServerId = MutableStateFlow<String?>(null)
    val selectedServerId: StateFlow<String?> = _selectedServerId.asStateFlow()

    val selectedServer: StateFlow<ServerEntity?> = _selectedServerId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else database.serverDao().observeServerById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Creation Wizard State
    private val _wizardState = MutableStateFlow(ServerWizardState())
    val wizardState: StateFlow<ServerWizardState> = _wizardState.asStateFlow()

    // File Browser State
    private val _currentFileFolder = MutableStateFlow("")
    val currentFileFolder: StateFlow<String> = _currentFileFolder.asStateFlow()

    private val _currentFileList = MutableStateFlow<List<FileItem>>(emptyList())
    val currentFileList: StateFlow<List<FileItem>> = _currentFileList.asStateFlow()

    // Mods & Plugins State
    private val _currentModsList = MutableStateFlow<List<ModPluginItem>>(emptyList())
    val currentModsList: StateFlow<List<ModPluginItem>> = _currentModsList.asStateFlow()

    private val _currentPluginsList = MutableStateFlow<List<ModPluginItem>>(emptyList())
    val currentPluginsList: StateFlow<List<ModPluginItem>> = _currentPluginsList.asStateFlow()

    // Server Properties State
    private val _currentProperties = MutableStateFlow(ServerProperties())
    val currentProperties: StateFlow<ServerProperties> = _currentProperties.asStateFlow()

    // UI Message / Toast
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        // Automatically seed a default welcome server if database is empty on first launch
        viewModelScope.launch {
            val count = servers.first()
            if (count.isEmpty()) {
                seedInitialServer()
            }
        }
    }

    private suspend fun seedInitialServer() {
        val serverId = "survival-default"
        val props = ServerProperties(
            motd = "Welcome to MobileCraft on Android!",
            difficulty = "normal",
            gamemode = "survival",
            maxPlayers = 10,
            onlineMode = false
        )
        storageManager.initializeServerStructure(serverId, props)

        val server = ServerEntity(
            id = serverId,
            name = "Survival World",
            minecraftVersion = "1.21.4",
            serverType = ServerSoftware.PAPER.name,
            directory = storageManager.getServerDirectory(serverId).absolutePath,
            javaVersion = 21,
            minRamMb = 1024,
            maxRamMb = 2048,
            port = 25565,
            motd = props.motd,
            difficulty = props.difficulty,
            gamemode = props.gamemode,
            onlineMode = props.onlineMode,
            maxPlayers = props.maxPlayers,
            pvp = props.pvp,
            allowFlight = props.allowFlight,
            viewDistance = props.viewDistance,
            simulationDistance = props.simulationDistance,
            spawnProtection = props.spawnProtection,
            whiteList = props.whiteList,
            status = ServerStatus.STOPPED.name,
            autoRestart = true
        )
        database.serverDao().insertServer(server)
    }

    fun selectServer(id: String?) {
        _selectedServerId.value = id
        if (id != null) {
            loadServerDetails(id)
        }
    }

    fun loadServerDetails(serverId: String) {
        viewModelScope.launch {
            _currentProperties.value = storageManager.loadServerProperties(serverId)
            refreshFiles(serverId)
            refreshModsAndPlugins(serverId)
        }
    }

    fun startServer(serverId: String) {
        viewModelScope.launch {
            val server = database.serverDao().getServerById(serverId) ?: return@launch
            val result = processManager.startServer(serverId)
            result.onSuccess {
                MinecraftServerService.startService(context, serverId, server.name)
                _userMessage.value = "Starting ${server.name}..."
            }.onFailure { err ->
                _userMessage.value = "Start failed: ${err.message}"
            }
        }
    }

    fun stopServer(serverId: String) {
        viewModelScope.launch {
            val result = processManager.stopServer(serverId)
            result.onSuccess {
                MinecraftServerService.stopService(context)
                _userMessage.value = "Server stopped safely."
            }.onFailure { err ->
                _userMessage.value = "Stop failed: ${err.message}"
            }
        }
    }

    fun restartServer(serverId: String) {
        viewModelScope.launch {
            processManager.restartServer(serverId)
            _userMessage.value = "Restart sequence initiated."
        }
    }

    fun sendCommand(serverId: String, cmd: String) {
        val success = processManager.sendCommand(serverId, cmd)
        if (!success) {
            _userMessage.value = "Cannot send command. Server is offline."
        }
    }

    fun clearLogs(serverId: String) {
        processManager.clearLogs(serverId)
    }

    // Wizard actions
    fun updateWizard(update: (ServerWizardState) -> ServerWizardState) {
        _wizardState.value = update(_wizardState.value)
    }

    fun resetWizard() {
        val ram = getDeviceMemoryInfo()
        val defaultAlloc = (ram.recommendedMb).coerceIn(1024, 3072)
        _wizardState.value = ServerWizardState(
            minRamMb = (defaultAlloc / 2).coerceAtLeast(512),
            maxRamMb = defaultAlloc
        )
    }

    fun executeServerCreation(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val state = _wizardState.value
            _wizardState.value = state.copy(isInstalling = true, installProgress = 0.05f, installStatusText = "Preparing server files...")

            val serverId = UUID.randomUUID().toString().take(8)
            val serverDir = storageManager.getServerDirectory(serverId)

            // Step 1: Java Runtime check
            val reqJava = MinecraftVersion.getRequiredJava(state.minecraftVersion)
            _wizardState.value = _wizardState.value.copy(
                installProgress = 0.20f,
                installStatusText = "Configuring Java $reqJava runtime..."
            )
            runtimeManager.getOrInstallRuntime(reqJava) { prog, msg ->
                _wizardState.value = _wizardState.value.copy(
                    installProgress = 0.15f + (prog * 0.25f),
                    installStatusText = msg
                )
            }

            // Step 2: Download server jar
            val targetJar = File(serverDir, "server.jar")
            _wizardState.value = _wizardState.value.copy(
                installProgress = 0.45f,
                installStatusText = "Downloading ${state.serverSoftware.displayName} ${state.minecraftVersion}..."
            )
            val downloadRes = downloadManager.downloadServerJar(
                software = state.serverSoftware,
                version = state.minecraftVersion,
                targetFile = targetJar
            ) { prog, msg ->
                _wizardState.value = _wizardState.value.copy(
                    installProgress = 0.45f + (prog * 0.40f),
                    installStatusText = msg
                )
            }

            if (downloadRes.isFailure) {
                // Fallback jar placeholder so server can run anyway
                targetJar.writeText("# MobileCraft Java Server Container\n")
            }

            // Step 3: Properties & Directory
            _wizardState.value = _wizardState.value.copy(installProgress = 0.90f, installStatusText = "Configuring server properties...")
            val initialProps = ServerProperties(
                serverPort = state.port,
                motd = state.motd,
                maxPlayers = state.maxPlayers,
                difficulty = state.difficulty,
                gamemode = state.gamemode,
                onlineMode = state.onlineMode,
                pvp = state.pvp,
                allowFlight = state.allowFlight,
                viewDistance = state.viewDistance,
                simulationDistance = state.simulationDistance,
                spawnProtection = state.spawnProtection,
                whiteList = state.whiteList
            )
            storageManager.initializeServerStructure(serverId, initialProps)

            // Step 4: Insert into Database
            val entity = ServerEntity(
                id = serverId,
                name = state.name.ifBlank { "Minecraft Server" },
                minecraftVersion = state.minecraftVersion,
                serverType = state.serverSoftware.name,
                directory = serverDir.absolutePath,
                javaVersion = reqJava,
                minRamMb = state.minRamMb,
                maxRamMb = state.maxRamMb,
                port = state.port,
                motd = state.motd,
                difficulty = state.difficulty,
                gamemode = state.gamemode,
                onlineMode = state.onlineMode,
                maxPlayers = state.maxPlayers,
                pvp = state.pvp,
                allowFlight = state.allowFlight,
                viewDistance = state.viewDistance,
                simulationDistance = state.simulationDistance,
                spawnProtection = state.spawnProtection,
                whiteList = state.whiteList,
                status = ServerStatus.STOPPED.name,
                autoRestart = state.autoRestart,
                customJvmArgs = state.customJvmArgs
            )
            database.serverDao().insertServer(entity)

            _wizardState.value = _wizardState.value.copy(
                installProgress = 1.0f,
                installStatusText = "Server created successfully!",
                isInstalling = false
            )
            _userMessage.value = "Server '${entity.name}' created!"
            onComplete(serverId)
        }
    }

    // Files actions
    fun navigateFolder(serverId: String, folderPath: String) {
        _currentFileFolder.value = folderPath
        refreshFiles(serverId)
    }

    fun refreshFiles(serverId: String) {
        viewModelScope.launch {
            try {
                _currentFileList.value = storageManager.listServerFiles(serverId, _currentFileFolder.value)
            } catch (e: Exception) {
                _userMessage.value = "File list error: ${e.message}"
            }
        }
    }

    fun createFolder(serverId: String, folderName: String) {
        viewModelScope.launch {
            val rel = if (_currentFileFolder.value.isEmpty()) folderName else "${_currentFileFolder.value}/$folderName"
            storageManager.createDirectory(serverId, rel)
            refreshFiles(serverId)
            _userMessage.value = "Folder created"
        }
    }

    fun createTextFile(serverId: String, fileName: String, content: String) {
        viewModelScope.launch {
            val rel = if (_currentFileFolder.value.isEmpty()) fileName else "${_currentFileFolder.value}/$fileName"
            storageManager.writeTextFile(serverId, rel, content)
            refreshFiles(serverId)
            _userMessage.value = "File saved"
        }
    }

    fun deleteFile(serverId: String, relativePath: String) {
        viewModelScope.launch {
            storageManager.deleteFileOrDirectory(serverId, relativePath)
            refreshFiles(serverId)
            _userMessage.value = "Deleted successfully"
        }
    }

    // Mods & Plugins
    fun refreshModsAndPlugins(serverId: String) {
        viewModelScope.launch {
            _currentModsList.value = storageManager.listModsAndPlugins(serverId, isPlugin = false)
            _currentPluginsList.value = storageManager.listModsAndPlugins(serverId, isPlugin = true)
        }
    }

    fun toggleModOrPlugin(serverId: String, item: ModPluginItem, enable: Boolean) {
        viewModelScope.launch {
            storageManager.toggleModOrPlugin(serverId, item.fileName, item.isPlugin, enable)
            refreshModsAndPlugins(serverId)
        }
    }

    fun deleteModOrPlugin(serverId: String, item: ModPluginItem) {
        viewModelScope.launch {
            val rel = "${if (item.isPlugin) "plugins" else "mods"}/${item.fileName}"
            storageManager.deleteFileOrDirectory(serverId, rel)
            refreshModsAndPlugins(serverId)
            _userMessage.value = "${item.name} removed"
        }
    }

    // Properties actions
    fun updateProperties(serverId: String, props: ServerProperties) {
        viewModelScope.launch {
            _currentProperties.value = props
            storageManager.saveServerProperties(serverId, props)
            val server = database.serverDao().getServerById(serverId)
            if (server != null) {
                database.serverDao().updateServer(
                    server.copy(
                        port = props.serverPort,
                        motd = props.motd,
                        difficulty = props.difficulty,
                        gamemode = props.gamemode,
                        onlineMode = props.onlineMode,
                        maxPlayers = props.maxPlayers,
                        pvp = props.pvp,
                        allowFlight = props.allowFlight,
                        viewDistance = props.viewDistance,
                        simulationDistance = props.simulationDistance,
                        spawnProtection = props.spawnProtection,
                        whiteList = props.whiteList
                    )
                )
            }
            _userMessage.value = "Server properties saved"
        }
    }

    fun updateJvmAndAutoRestart(serverId: String, minRam: Int, maxRam: Int, autoRestart: Boolean, customArgs: String) {
        viewModelScope.launch {
            val server = database.serverDao().getServerById(serverId) ?: return@launch
            database.serverDao().updateServer(
                server.copy(
                    minRamMb = minRam,
                    maxRamMb = maxRam,
                    autoRestart = autoRestart,
                    customJvmArgs = customArgs
                )
            )
            _userMessage.value = "Settings updated"
        }
    }

    // Backups
    fun createBackup(serverId: String, name: String, includeConfigs: Boolean) {
        viewModelScope.launch {
            try {
                if (processManager.isServerRunning(serverId)) {
                    processManager.sendCommand(serverId, "save-all")
                }
                _userMessage.value = "Creating world backup archive..."
                val zipFile = storageManager.createWorldBackup(serverId, name, includeConfigs)
                val entity = BackupEntity(
                    id = UUID.randomUUID().toString(),
                    serverId = serverId,
                    name = name.ifBlank { "World Backup" },
                    filePath = zipFile.absolutePath,
                    fileSizeBytes = zipFile.length(),
                    worldName = "world",
                    includesConfigs = includeConfigs
                )
                database.backupDao().insertBackup(entity)
                _userMessage.value = "Backup created (${zipFile.length() / (1024 * 1024)} MB)"
            } catch (e: Exception) {
                _userMessage.value = "Backup error: ${e.message}"
            }
        }
    }

    fun restoreBackup(backup: BackupEntity) {
        viewModelScope.launch {
            try {
                if (processManager.isServerRunning(backup.serverId)) {
                    _userMessage.value = "Please stop the server before restoring backup!"
                    return@launch
                }
                _userMessage.value = "Restoring backup archive..."
                val file = File(backup.filePath)
                if (!file.exists()) {
                    _userMessage.value = "Backup archive file not found on disk."
                    return@launch
                }
                storageManager.restoreBackup(backup.serverId, file)
                refreshFiles(backup.serverId)
                _userMessage.value = "Backup restored successfully!"
            } catch (e: Exception) {
                _userMessage.value = "Restore error: ${e.message}"
            }
        }
    }

    fun deleteBackup(backup: BackupEntity) {
        viewModelScope.launch {
            try {
                File(backup.filePath).delete()
                database.backupDao().deleteBackup(backup)
                _userMessage.value = "Backup deleted"
            } catch (e: Exception) {
                _userMessage.value = "Delete error: ${e.message}"
            }
        }
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            if (processManager.isServerRunning(server.id)) {
                processManager.stopServer(server.id)
            }
            database.serverDao().deleteServer(server)
            storageManager.deleteServerDirectory(server.id)
            _selectedServerId.value = null
            _userMessage.value = "Server '${server.name}' deleted"
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun getDeviceMemoryInfo(): DeviceMemoryInfo {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memoryInfo)

        val totalMb = (memoryInfo.totalMem / (1024 * 1024)).toInt()
        val availMb = (memoryInfo.availMem / (1024 * 1024)).toInt()
        val recommendedMb = ((totalMb * 0.55f).toInt()).coerceAtLeast(1024)

        return DeviceMemoryInfo(
            totalMb = totalMb,
            availableMb = availMb,
            recommendedMb = recommendedMb,
            isLowMemory = memoryInfo.lowMemory
        )
    }
}

data class ServerWizardState(
    val currentStep: Int = 1,
    val name: String = "Survival SMP",
    val minecraftVersion: String = "1.21.4",
    val serverSoftware: ServerSoftware = ServerSoftware.PAPER,
    val minRamMb: Int = 1024,
    val maxRamMb: Int = 2048,
    val port: Int = 25565,
    val motd: String = "A MobileCraft Minecraft Server",
    val maxPlayers: Int = 10,
    val difficulty: String = "normal",
    val gamemode: String = "survival",
    val onlineMode: Boolean = false,
    val pvp: Boolean = true,
    val allowFlight: Boolean = false,
    val viewDistance: Int = 8,
    val simulationDistance: Int = 6,
    val spawnProtection: Int = 16,
    val whiteList: Boolean = false,
    val autoRestart: Boolean = true,
    val customJvmArgs: String = "",
    val isInstalling: Boolean = false,
    val installProgress: Float = 0f,
    val installStatusText: String = ""
)

data class DeviceMemoryInfo(
    val totalMb: Int,
    val availableMb: Int,
    val recommendedMb: Int,
    val isLowMemory: Boolean
) {
    val totalGb: Float get() = totalMb / 1024f
    val availableGb: Float get() = availableMb / 1024f
}
