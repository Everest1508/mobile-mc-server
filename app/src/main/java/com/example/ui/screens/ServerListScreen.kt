package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entities.ServerEntity
import com.example.data.model.ServerStatus
import com.example.ui.components.MonospacePill
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.ServerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    viewModel: ServerViewModel,
    onOpenServer: (String) -> Unit,
    onCreateServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val servers by viewModel.servers.collectAsState()
    val statuses by viewModel.serverStatuses.collectAsState()
    val memInfo = remember { viewModel.getDeviceMemoryInfo() }

    // Aggregate running servers RAM
    val runningRamMb = servers.filter {
        val s = statuses[it.id] ?: ServerStatus.fromString(it.status)
        s == ServerStatus.RUNNING || s == ServerStatus.STARTING
    }.sumOf { it.maxRamMb }

    var serverToDelete by remember { mutableStateOf<ServerEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SleekBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateServer,
                icon = { Icon(Icons.Filled.Add, contentDescription = "Create", modifier = Modifier.size(20.dp)) },
                text = {
                    Text(
                        "New Server",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                containerColor = SleekPrimary,
                contentColor = SleekOnPrimary,
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("create_server_fab")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            item {
                // Header Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(28.dp)),
                    color = SleekCard
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "MobileCraft Server",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(SleekMint)
                                    )
                                    Text(
                                        text = "HOSTING ENGINE READY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            fontSize = 10.sp
                                        ),
                                        color = SleekMint
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SleekSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Dns,
                                        contentDescription = null,
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${servers.size} ${if (servers.size == 1) "Server" else "Servers"}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Device Memory Status Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE SERVER RAM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 10.sp
                                ),
                                color = TextSecondary
                            )
                            Text(
                                text = "${runningRamMb} MB / ${memInfo.totalMb} MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val progress = if (memInfo.totalMb > 0) runningRamMb.toFloat() / memInfo.totalMb.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (progress > 0.75f) SleekCoral else SleekPrimary,
                            trackColor = SleekOutline
                        )

                        if (progress > 0.75f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = SleekAmber, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "High RAM load across running servers! Consider stopping unused servers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekAmber
                                )
                            }
                        }
                    }
                }
            }

            if (servers.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .border(1.dp, SleekBorder, RoundedCornerShape(28.dp)),
                        color = SleekCard
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SleekPrimaryContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = SleekOnPrimaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Text(
                                text = "No Servers Configured",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextWhite
                            )

                            Text(
                                text = "Create a Minecraft Java Edition server in seconds. Support for Vanilla, Paper, Fabric, NeoForge, and Forge.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = onCreateServer,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SleekPrimary,
                                    contentColor = SleekOnPrimary
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("empty_create_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create First Server", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "MY SERVERS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                items(servers, key = { it.id }) { server ->
                    val status = statuses[server.id] ?: ServerStatus.fromString(server.status)
                    val isOnline = status == ServerStatus.RUNNING

                    ServerCard(
                        server = server,
                        status = status,
                        onOpen = { onOpenServer(server.id) },
                        onToggleState = {
                            if (isOnline) {
                                viewModel.stopServer(server.id)
                            } else {
                                viewModel.startServer(server.id)
                            }
                        },
                        onDelete = { serverToDelete = server },
                        modifier = Modifier.testTag("server_card_${server.id}")
                    )
                }
            }
        }
    }

    if (serverToDelete != null) {
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text("Delete Server?", color = TextWhite, fontWeight = FontWeight.Medium) },
            text = {
                Text(
                    "Are you sure you want to delete '${serverToDelete?.name}'? This will permanently erase the server files, worlds, and configurations.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        serverToDelete?.let { viewModel.deleteServer(it) }
                        serverToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleekCoral,
                        contentColor = SleekOnCoral
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { serverToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = SleekCard,
            shape = RoundedCornerShape(28.dp),
            titleContentColor = TextWhite,
            textContentColor = TextSecondary
        )
    }
}

@Composable
fun ServerCard(
    server: ServerEntity,
    status: ServerStatus,
    onOpen: () -> Unit,
    onToggleState: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline = status == ServerStatus.RUNNING
    val isStarting = status == ServerStatus.STARTING || status == ServerStatus.INSTALLING
    val isStopping = status == ServerStatus.STOPPING

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(
                1.dp,
                if (isOnline) SleekMint.copy(alpha = 0.4f) else SleekBorder,
                RoundedCornerShape(28.dp)
            )
            .clickable { onOpen() },
        color = SleekCard
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.2).sp
                        ),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = server.motd,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                MonospacePill(text = "${server.serverType} ${server.minecraftVersion}")
                MonospacePill(text = "Port ${server.port}")
                MonospacePill(text = "${server.maxRamMb} MB")
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = SleekBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Server",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpen,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SleekOutline)
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp), tint = SleekPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manage", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onToggleState,
                        enabled = !isStarting && !isStopping,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOnline) SleekCoral else SleekMint,
                            contentColor = if (isOnline) SleekOnCoral else SleekOnMint
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        if (isStarting || isStopping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = if (isOnline) SleekOnCoral else SleekOnMint
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isStarting) "Starting..." else "Stopping...",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Icon(
                                imageVector = if (isOnline) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isOnline) "Stop" else "Start",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
