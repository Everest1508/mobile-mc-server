package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entities.BackupEntity
import com.example.data.db.entities.ServerEntity
import com.example.data.model.*
import com.example.ui.components.MonospacePill
import com.example.ui.components.SleekNavTile
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.ServerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDashboardScreen(
    serverId: String,
    viewModel: ServerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val servers by viewModel.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    val statuses by viewModel.serverStatuses.collectAsState()
    val status = statuses[serverId] ?: (server?.status?.let { ServerStatus.fromString(it) } ?: ServerStatus.STOPPED)

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Console", "Files", "Mods", "Backups", "Players", "Settings")

    LaunchedEffect(serverId) {
        viewModel.selectServer(serverId)
    }

    if (server == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SleekBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SleekPrimary)
        }
        return
    }

    val isOnline = status == ServerStatus.RUNNING

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SleekBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekBg)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedTab != 0) {
                                    selectedTab = 0
                                } else {
                                    onBack()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SleekSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = server.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = TextWhite
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) SleekMint else if (status == ServerStatus.STARTING || status == ServerStatus.INSTALLING) SleekAmber else TextMuted)
                                )
                                Text(
                                    text = status.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isOnline) SleekMint else if (status == ServerStatus.STARTING || status == ServerStatus.INSTALLING) SleekAmber else TextMuted
                                )
                                Text(
                                    text = "• ${server.serverType} ${server.minecraftVersion}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    if (selectedTab != 0) {
                        TextButton(
                            onClick = { selectedTab = 0 },
                            colors = ButtonDefaults.textButtonColors(contentColor = SleekPrimary)
                        ) {
                            Text("Overview", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.refreshFiles(serverId) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SleekSurfaceVariant)
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Sleek Tab Bar if navigating deeper
                if (selectedTab != 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SleekBg,
                        contentColor = SleekPrimary,
                        edgePadding = 0.dp,
                        divider = { HorizontalDivider(color = SleekBorder) }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selectedTab == index) SleekPrimary else TextMuted
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> SleekOverviewTab(
                    server = server,
                    status = status,
                    viewModel = viewModel,
                    onNavigateToTab = { targetTab -> selectedTab = targetTab }
                )
                1 -> ConsoleTab(serverId = serverId, status = status, viewModel = viewModel)
                2 -> FilesTab(serverId = serverId, viewModel = viewModel)
                3 -> ModsPluginsTab(serverId = serverId, server = server, viewModel = viewModel)
                4 -> BackupsTab(serverId = serverId, isRunning = status == ServerStatus.RUNNING, viewModel = viewModel)
                5 -> PlayersTab(serverId = serverId, status = status, viewModel = viewModel)
                6 -> SettingsTab(server = server, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SleekOverviewTab(
    server: ServerEntity,
    status: ServerStatus,
    viewModel: ServerViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val stats by viewModel.processManager.getStatsFlow(server.id).collectAsState()
    val storage = remember { viewModel.storageManager.getStorageTelemetry() }
    val isOnline = status == ServerStatus.RUNNING
    val isStarting = status == ServerStatus.STARTING || status == ServerStatus.INSTALLING
    val isStopping = status == ServerStatus.STOPPING

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        // 2x2 Telemetry Grid (Sleek Metric Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Players Card
                    val playersProgress = if (server.maxPlayers > 0) stats.activePlayersCount.toFloat() / server.maxPlayers else 0f
                    StatCard(
                        title = "Players",
                        value = "${stats.activePlayersCount}",
                        subtitle = "/ ${server.maxPlayers}",
                        icon = Icons.Filled.Group,
                        accentColor = SleekPrimary,
                        progress = playersProgress,
                        modifier = Modifier.weight(1f)
                    )

                    // CPU Usage Card
                    val cpuProgress = if (isOnline) stats.cpuUsagePercent / 100f else 0f
                    StatCard(
                        title = "CPU Usage",
                        value = if (isOnline) "${stats.cpuUsagePercent.toInt()}" else "0",
                        subtitle = "%",
                        icon = Icons.Filled.DeveloperBoard,
                        accentColor = if (stats.cpuUsagePercent > 80) SleekCoral else SleekPrimary,
                        progress = cpuProgress,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // RAM Memory Card
                    val ramGb = if (isOnline) stats.ramUsedMb / 1024f else 0f
                    val maxRamGb = server.maxRamMb / 1024f
                    val ramProgress = if (server.maxRamMb > 0 && isOnline) stats.ramUsedMb.toFloat() / server.maxRamMb.toFloat() else 0f
                    StatCard(
                        title = "RAM Memory",
                        value = if (isOnline) String.format(Locale.getDefault(), "%.1f", ramGb) else "0.0",
                        subtitle = "/ ${String.format(Locale.getDefault(), "%.1f", maxRamGb)}GB",
                        icon = Icons.Filled.Memory,
                        accentColor = SleekPrimary,
                        progress = ramProgress,
                        modifier = Modifier.weight(1f)
                    )

                    // Storage Card
                    val usedStorageGb = (storage.totalGb - storage.availableGb).coerceAtLeast(0.1f)
                    val storageProgress = if (storage.totalGb > 0) usedStorageGb / storage.totalGb else 0f
                    StatCard(
                        title = "Storage",
                        value = String.format(Locale.getDefault(), "%.1f", usedStorageGb),
                        subtitle = "GB",
                        icon = Icons.Filled.Storage,
                        accentColor = SleekPrimary,
                        progress = storageProgress,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Current Uptime Hero Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp)),
                color = SleekPrimary,
                contentColor = SleekOnPrimary
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "CURRENT UPTIME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            color = SleekOnPrimary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isOnline) formatUptime(stats.uptimeSeconds) else "00:00:00",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.5).sp,
                                fontSize = 24.sp
                            ),
                            color = SleekOnPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SleekOnPrimary,
                        contentColor = SleekPrimary
                    ) {
                        Box(
                            modifier = Modifier.padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QueryStats,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3x2 Navigation Grid of Tiles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SleekNavTile(
                        title = "Console",
                        icon = Icons.Filled.Terminal,
                        onClick = { onNavigateToTab(1) },
                        modifier = Modifier.weight(1f)
                    )
                    SleekNavTile(
                        title = "Files",
                        icon = Icons.Filled.FolderOpen,
                        onClick = { onNavigateToTab(2) },
                        modifier = Modifier.weight(1f)
                    )
                    SleekNavTile(
                        title = "Mods",
                        icon = Icons.Filled.Extension,
                        onClick = { onNavigateToTab(3) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SleekNavTile(
                        title = "Backups",
                        icon = Icons.Filled.History,
                        onClick = { onNavigateToTab(4) },
                        modifier = Modifier.weight(1f)
                    )
                    SleekNavTile(
                        title = "Players",
                        icon = Icons.Filled.Group,
                        onClick = { onNavigateToTab(5) },
                        modifier = Modifier.weight(1f)
                    )
                    SleekNavTile(
                        title = "Settings",
                        icon = Icons.Filled.Settings,
                        onClick = { onNavigateToTab(6) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Sleek Action Footer Buttons
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Action Button (Stop or Start)
                Button(
                    onClick = {
                        if (isOnline) viewModel.stopServer(server.id) else viewModel.startServer(server.id)
                    },
                    enabled = !isStarting && !isStopping,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOnline) SleekCoral else SleekMint,
                        contentColor = if (isOnline) SleekOnCoral else SleekOnMint
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("dashboard_start_stop_button")
                ) {
                    if (isStarting || isStopping) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = if (isOnline) SleekOnCoral else SleekOnMint
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isStarting) "STARTING..." else "STOPPING...",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    } else {
                        Icon(
                            imageVector = if (isOnline) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOnline) "STOP SERVER" else "START SERVER",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                // Restart / Refresh Action Button
                IconButton(
                    onClick = { viewModel.restartServer(server.id) },
                    enabled = isOnline,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SleekOutline)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Restart",
                        tint = if (isOnline) TextPrimary else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConsoleTab(
    serverId: String,
    status: ServerStatus,
    viewModel: ServerViewModel
) {
    val logs by viewModel.processManager.getLogsFlow(serverId).collectAsState()
    var commandInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var autoScroll by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, searchQuery) {
        if (searchQuery.isBlank()) logs
        else logs.filter { it.raw.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        // Console Control Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SleekSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter logs...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = customTextFieldColors(),
                    shape = RoundedCornerShape(50)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { autoScroll = !autoScroll },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (autoScroll) Icons.Filled.VerticalAlignBottom else Icons.Filled.Pause,
                        contentDescription = "Auto Scroll",
                        tint = if (autoScroll) SleekPrimary else TextMuted
                    )
                }

                IconButton(
                    onClick = { viewModel.clearLogs(serverId) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear Console", tint = TextMuted)
                }
            }
        }

        // Quick Command Suggestion Chips
        val quickCmds = listOf("say Hello", "list", "save-all", "op Player", "time set day", "weather clear")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SleekSurfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCmds.forEach { cmd ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { viewModel.sendCommand(serverId, cmd) },
                    color = SleekCard
                ) {
                    Text(
                        text = cmd,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Logs Output Window
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                item {
                    Text(
                        text = if (status == ServerStatus.RUNNING) "Waiting for server output..." else "Server is offline. Press START to view terminal logs.",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                items(filteredLogs) { entry ->
                    val color = when (entry.level) {
                        LogLevel.INFO -> TerminalInfo
                        LogLevel.WARN -> TerminalWarn
                        LogLevel.ERROR -> TerminalError
                        LogLevel.DEBUG -> TextMuted
                    }
                    Text(
                        text = entry.raw,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = color
                    )
                }
            }
        }

        // Interactive Command Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SleekSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ">",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SleekPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    placeholder = { Text("Type Minecraft command (e.g. list, say, op)...", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (commandInput.isNotBlank()) {
                            viewModel.sendCommand(serverId, commandInput)
                            commandInput = ""
                        }
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("console_command_input"),
                    colors = customTextFieldColors(),
                    shape = RoundedCornerShape(50)
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (commandInput.isNotBlank()) {
                            viewModel.sendCommand(serverId, commandInput)
                            commandInput = ""
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SleekPrimary)
                        .testTag("console_send_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = SleekOnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlayersTab(
    serverId: String,
    status: ServerStatus,
    viewModel: ServerViewModel
) {
    val players by viewModel.processManager.getPlayersFlow(serverId).collectAsState()
    var newPlayerName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            // Whitelist / Add Player Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(24.dp)),
                color = SleekCard
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Whitelist & Player Management",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPlayerName,
                            onValueChange = { newPlayerName = it },
                            placeholder = { Text("Player Minecraft Username") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = customTextFieldColors(),
                            shape = RoundedCornerShape(50)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newPlayerName.isNotBlank()) {
                                    viewModel.sendCommand(serverId, "whitelist add $newPlayerName")
                                    newPlayerName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SleekPrimary,
                                contentColor = SleekOnPrimary
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Whitelist", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "ACTIVE CONNECTED PLAYERS (${players.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                ),
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (players.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(24.dp)),
                    color = SleekCard
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.PersonOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                        Text(
                            text = "No Players Online",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Text(
                            text = "When players join your server, they will appear here in real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(players) { player ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(20.dp)),
                    color = SleekCard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SleekPrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = player.name.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = SleekOnPrimaryContainer
                                )
                            }
                            Column {
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextWhite
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (player.isOp) {
                                        Surface(shape = RoundedCornerShape(50), color = SleekAmberContainer) {
                                            Text(
                                                "OP",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = SleekAmber
                                            )
                                        }
                                    }
                                    Text("${player.pingMs}ms ping", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    if (player.isOp) viewModel.sendCommand(serverId, "deop ${player.name}")
                                    else viewModel.sendCommand(serverId, "op ${player.name}")
                                }
                            ) {
                                Icon(
                                    imageVector = if (player.isOp) Icons.Filled.Shield else Icons.Outlined.Shield,
                                    contentDescription = "Toggle OP",
                                    tint = if (player.isOp) SleekAmber else TextMuted
                                )
                            }

                            IconButton(
                                onClick = { viewModel.sendCommand(serverId, "kick ${player.name}") }
                            ) {
                                Icon(Icons.Outlined.Logout, contentDescription = "Kick", tint = SleekCoral)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModsPluginsTab(
    serverId: String,
    server: ServerEntity,
    viewModel: ServerViewModel
) {
    val mods by viewModel.currentModsList.collectAsState()
    val plugins by viewModel.currentPluginsList.collectAsState()
    val isPaper = server.serverType.equals("PAPER", ignoreCase = true)
    val isModded = server.serverType.equals("FABRIC", ignoreCase = true) ||
            server.serverType.equals("FORGE", ignoreCase = true) ||
            server.serverType.equals("NEOFORGE", ignoreCase = true)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(24.dp)),
                color = SleekCard
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isPaper) "Paper Plugins Manager" else if (isModded) "${server.serverType} Mods Manager" else "Server Addons",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPaper)
                            "Place your Bukkit/Spigot/Paper .jar plugins in the server's /plugins/ folder or upload via File Manager."
                        else if (isModded)
                            "Place compatible ${server.minecraftVersion} mod .jar files in the /mods/ folder to activate them."
                        else
                            "Vanilla server does not support custom mods or plugins. Upgrade to Paper or Fabric to install extensions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        val activeList = if (isPaper) plugins else mods
        if (activeList.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(24.dp)),
                    color = SleekCard
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.ExtensionOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                        Text("No Jars Installed", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text("Use the Files tab to upload or manage JAR archives in /${if (isPaper) "plugins" else "mods"}/.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        } else {
            items(activeList) { item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(20.dp)),
                    color = SleekCard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isPlugin) Icons.Filled.Extension else Icons.Filled.Widgets,
                                contentDescription = null,
                                tint = if (item.isEnabled) SleekPrimary else TextMuted
                            )
                            Column {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (item.isEnabled) TextWhite else TextMuted
                                )
                                Text(
                                    text = "${item.sizeBytes / 1024} KB • ${if (item.isEnabled) "Enabled" else "Disabled"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = item.isEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.toggleModOrPlugin(serverId, item, checked)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = SleekPrimary)
                            )
                            IconButton(onClick = { viewModel.deleteModOrPlugin(serverId, item) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = SleekCoral)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesTab(
    serverId: String,
    viewModel: ServerViewModel
) {
    val currentFolder by viewModel.currentFileFolder.collectAsState()
    val files by viewModel.currentFileList.collectAsState()
    var editingFile by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }

    if (editingFile != null) {
        val (relPath, initialContent) = editingFile!!
        var textContent by remember { mutableStateOf(initialContent) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(relPath, style = MaterialTheme.typography.titleMedium, color = TextWhite) },
                    navigationIcon = {
                        IconButton(onClick = { editingFile = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                viewModel.createTextFile(serverId, relPath, textContent)
                                editingFile = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = SleekOnPrimary),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekSurface)
                )
            },
            containerColor = TerminalBackground
        ) { editPad ->
            OutlinedTextField(
                value = textContent,
                onValueChange = { textContent = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(editPad)
                    .padding(8.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextPrimary),
                colors = customTextFieldColors()
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Navigation Breadcrumb & Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SleekSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentFolder.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val parent = currentFolder.substringBeforeLast('/', "")
                                viewModel.navigateFolder(serverId, parent)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = "/${currentFolder}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        color = SleekPrimary,
                        maxLines = 1
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showCreateFolderDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.CreateNewFolder, contentDescription = "New Folder", tint = TextPrimary)
                    }
                    IconButton(onClick = { showCreateFileDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.NoteAdd, contentDescription = "New File", tint = TextPrimary)
                    }
                    IconButton(onClick = { viewModel.refreshFiles(serverId) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Files List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (files.isEmpty()) {
                item {
                    Text(
                        text = "Directory is empty",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(files) { file ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                if (file.isDirectory) {
                                    viewModel.navigateFolder(serverId, file.relativePath)
                                } else {
                                    kotlinx.coroutines.MainScope().launch {
                                        try {
                                            val text = viewModel.storageManager.readTextFile(serverId, file.relativePath)
                                            editingFile = Pair(file.relativePath, text)
                                        } catch (_: Exception) {}
                                    }
                                }
                            },
                        color = SleekCard
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = if (file.isDirectory) SleekAmber else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = TextWhite,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (file.isDirectory) "Folder" else "${file.sizeBytes / 1024} KB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteFile(serverId, file.relativePath) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = SleekCoral.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder", color = TextWhite) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("Folder name") },
                    singleLine = true,
                    colors = customTextFieldColors(),
                    shape = RoundedCornerShape(50)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createFolder(serverId, folderName)
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = SleekOnPrimary),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = SleekCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showCreateFileDialog) {
        var fileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Create File", color = TextWhite) },
            text = {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    placeholder = { Text("e.g. motd.txt, config.yml") },
                    singleLine = true,
                    colors = customTextFieldColors(),
                    shape = RoundedCornerShape(50)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileName.isNotBlank()) {
                            viewModel.createTextFile(serverId, fileName, "")
                            showCreateFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = SleekOnPrimary),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = SleekCard,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SettingsTab(
    server: ServerEntity,
    viewModel: ServerViewModel
) {
    val props by viewModel.currentProperties.collectAsState()
    var motd by remember(props) { mutableStateOf(props.motd) }
    var port by remember(props) { mutableStateOf(props.serverPort.toString()) }
    var maxPlayers by remember(props) { mutableStateOf(props.maxPlayers.toString()) }
    var difficulty by remember(props) { mutableStateOf(props.difficulty) }
    var gamemode by remember(props) { mutableStateOf(props.gamemode) }
    var onlineMode by remember(props) { mutableStateOf(props.onlineMode) }
    var pvp by remember(props) { mutableStateOf(props.pvp) }
    var allowFlight by remember(props) { mutableStateOf(props.allowFlight) }
    var whiteList by remember(props) { mutableStateOf(props.whiteList) }
    var viewDistance by remember(props) { mutableStateOf(props.viewDistance.toFloat()) }
    var minRam by remember(server) { mutableIntStateOf(server.minRamMb) }
    var maxRam by remember(server) { mutableIntStateOf(server.maxRamMb) }
    var autoRestart by remember(server) { mutableStateOf(server.autoRestart) }
    var customArgs by remember(server) { mutableStateOf(server.customJvmArgs) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            WizardCard(title = "server.properties Visual Editor", subtitle = "Changes will be applied and preserved in the configuration file.") {
                OutlinedTextField(
                    value = motd,
                    onValueChange = { motd = it },
                    label = { Text("MOTD (Message of the Day)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors(),
                    shape = RoundedCornerShape(20.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = customTextFieldColors(),
                        shape = RoundedCornerShape(20.dp)
                    )
                    OutlinedTextField(
                        value = maxPlayers,
                        onValueChange = { maxPlayers = it },
                        label = { Text("Max Players") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = customTextFieldColors(),
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SwitchRow(title = "Online Mode (Mojang Auth)", subtitle = "Verify Minecraft account tokens", checked = onlineMode, onCheckedChange = { onlineMode = it })
                SwitchRow(title = "Enable PvP", subtitle = "Player vs Player combat", checked = pvp, onCheckedChange = { pvp = it })
                SwitchRow(title = "Allow Flight", subtitle = "Allow survival players to fly", checked = allowFlight, onCheckedChange = { allowFlight = it })
                SwitchRow(title = "Whitelist Enforced", subtitle = "Only whitelisted players can join", checked = whiteList, onCheckedChange = { whiteList = it })

                Spacer(modifier = Modifier.height(8.dp))
                Text("View Distance: ${viewDistance.toInt()} chunks", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                Slider(
                    value = viewDistance,
                    onValueChange = { viewDistance = it },
                    valueRange = 4f..16f,
                    steps = 12,
                    colors = SliderDefaults.colors(thumbColor = SleekPrimary, activeTrackColor = SleekPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val updated = props.copy(
                            motd = motd,
                            serverPort = port.toIntOrNull() ?: 25565,
                            maxPlayers = maxPlayers.toIntOrNull() ?: 10,
                            difficulty = difficulty,
                            gamemode = gamemode,
                            onlineMode = onlineMode,
                            pvp = pvp,
                            allowFlight = allowFlight,
                            whiteList = whiteList,
                            viewDistance = viewDistance.toInt()
                        )
                        viewModel.updateProperties(server.id, updated)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = SleekOnPrimary),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Save Server Properties", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            WizardCard(title = "JVM & Process Tuning", subtitle = "Adjust heap size allocation and flags for Java runtime execution.") {
                Text("Max Heap RAM: $maxRam MB", style = MaterialTheme.typography.labelMedium, color = SleekPrimary)
                Slider(
                    value = maxRam.toFloat(),
                    onValueChange = { maxRam = ((it.toInt() / 256) * 256).coerceAtLeast(512) },
                    valueRange = 512f..8192f,
                    steps = 29,
                    colors = SliderDefaults.colors(thumbColor = SleekPrimary, activeTrackColor = SleekPrimary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customArgs,
                    onValueChange = { customArgs = it },
                    label = { Text("Custom JVM Arguments") },
                    placeholder = { Text("e.g. -XX:+UseG1GC") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors(),
                    shape = RoundedCornerShape(20.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                SwitchRow(
                    title = "Auto-Restart on Crash",
                    subtitle = "Automatic crash recovery with backoff",
                    checked = autoRestart,
                    onCheckedChange = { autoRestart = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.updateJvmAndAutoRestart(server.id, minRam, maxRam, autoRestart, customArgs)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = SleekOnPrimary),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Save JVM & Process Settings", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BackupsTab(
    serverId: String,
    isRunning: Boolean,
    viewModel: ServerViewModel
) {
    val backups by viewModel.allBackups.collectAsState()
    val serverBackups = backups.filter { it.serverId == serverId }
    var showCreateDialog by remember { mutableStateOf(false) }
    var backupToRestore by remember { mutableStateOf<BackupEntity?>(null) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(24.dp)),
                color = SleekCard
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "World Backups & Snapshots",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create compressed ZIP archives of worlds, Nether, The End, and configurations safely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = SleekOnPrimary),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Backup Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "SAVED BACKUPS (${serverBackups.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                ),
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (serverBackups.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(24.dp)),
                    color = SleekCard
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                        Text("No Backups Created", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text("Create a snapshot before making major changes or installing mods.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        } else {
            items(serverBackups) { backup ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(20.dp)),
                    color = SleekCard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Archive, contentDescription = null, tint = SleekPrimary)
                            Column {
                                Text(
                                    text = backup.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextWhite
                                )
                                Text(
                                    text = "${dateFormat.format(Date(backup.createdAt))} • ${backup.fileSizeBytes / (1024 * 1024)} MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { backupToRestore = backup },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SleekOutline)),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Restore")
                            }

                            IconButton(onClick = { viewModel.deleteBackup(backup) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = SleekCoral)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var backupName by remember { mutableStateOf("Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}") }
        var includeConfigs by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create World Backup", color = TextWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = backupName,
                        onValueChange = { backupName = it },
                        label = { Text("Backup Label") },
                        singleLine = true,
                        colors = customTextFieldColors(),
                        shape = RoundedCornerShape(20.dp)
                    )
                    SwitchRow(
                        title = "Include Configs & Mods",
                        subtitle = "Package config/ and mods/ folders",
                        checked = includeConfigs,
                        onCheckedChange = { includeConfigs = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createBackup(serverId, backupName, includeConfigs)
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = SleekOnPrimary),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Start Backup", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = SleekCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (backupToRestore != null) {
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = { Text("Restore Backup?", color = TextWhite) },
            text = {
                Text(
                    "Restoring '${backupToRestore?.name}' will overwrite existing world files. ${if (isRunning) "Please stop the server first." else ""}",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        backupToRestore?.let { viewModel.restoreBackup(it) }
                        backupToRestore = null
                    },
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = SleekCoral, contentColor = SleekOnCoral),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Overwrite & Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = SleekCard,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

fun formatUptime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
}
