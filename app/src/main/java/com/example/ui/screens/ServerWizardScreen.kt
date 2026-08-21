package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MinecraftVersion
import com.example.data.model.ServerSoftware
import com.example.ui.components.MonospacePill
import com.example.ui.theme.*
import com.example.ui.viewmodel.ServerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerWizardScreen(
    viewModel: ServerViewModel,
    onBack: () -> Unit,
    onServerCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val wizardState by viewModel.wizardState.collectAsState()
    val memInfo = remember { viewModel.getDeviceMemoryInfo() }
    val storage = remember { viewModel.storageManager.getStorageTelemetry() }

    LaunchedEffect(Unit) {
        viewModel.resetWizard()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SleekBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Create Minecraft Server",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.3).sp
                            ),
                            color = TextWhite
                        )
                        Text(
                            text = if (wizardState.isInstalling) "Installing..." else "Step ${wizardState.currentStep} of 6",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = SleekPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (wizardState.currentStep > 1 && !wizardState.isInstalling) {
                                viewModel.updateWizard { it.copy(currentStep = it.currentStep - 1) }
                            } else {
                                onBack()
                            }
                        },
                        enabled = !wizardState.isInstalling,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SleekSurfaceVariant)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekBg)
            )
        },
        bottomBar = {
            if (!wizardState.isInstalling) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SleekBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (wizardState.currentStep > 1) {
                            OutlinedButton(
                                onClick = { viewModel.updateWizard { it.copy(currentStep = it.currentStep - 1) } },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SleekOutline))
                            ) {
                                Text("Previous")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        if (wizardState.currentStep < 6) {
                            Button(
                                onClick = { viewModel.updateWizard { it.copy(currentStep = it.currentStep + 1) } },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = SleekOnPrimary),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("wizard_next_button")
                            ) {
                                Text("Next Step", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.executeServerCreation { serverId ->
                                        onServerCreated(serverId)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekMint, contentColor = SleekOnMint),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("wizard_create_button")
                            ) {
                                Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Install & Build Server", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (wizardState.isInstalling) {
            InstallationProgressView(
                progress = wizardState.installProgress,
                statusText = wizardState.installStatusText,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                // Step Progress Indicator
                item {
                    LinearProgressIndicator(
                        progress = { wizardState.currentStep / 6f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SleekPrimary,
                        trackColor = SleekOutline
                    )
                }

                when (wizardState.currentStep) {
                    1 -> item { Step1ServerName(wizardState, viewModel) }
                    2 -> item { Step2MinecraftVersion(wizardState, viewModel) }
                    3 -> item { Step3ServerSoftware(wizardState, viewModel) }
                    4 -> item { Step4RamAllocation(wizardState, memInfo, viewModel) }
                    5 -> item { Step5StorageDirectory(wizardState, storage) }
                    6 -> item { Step6AdvancedConfig(wizardState, viewModel) }
                }
            }
        }
    }
}

@Composable
fun Step1ServerName(state: com.example.ui.viewmodel.ServerWizardState, viewModel: ServerViewModel) {
    WizardCard(
        title = "Server Identity",
        subtitle = "Choose a descriptive name and server description for your Minecraft Java server."
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { viewModel.updateWizard { s -> s.copy(name = it) } },
            label = { Text("Server Name") },
            placeholder = { Text("e.g., Survival SMP") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wizard_name_input"),
            colors = customTextFieldColors(),
            shape = RoundedCornerShape(20.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = state.motd,
            onValueChange = { viewModel.updateWizard { s -> s.copy(motd = it) } },
            label = { Text("Server Message (MOTD)") },
            placeholder = { Text("Welcome to my Minecraft Server!") },
            modifier = Modifier.fillMaxWidth(),
            colors = customTextFieldColors(),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun Step2MinecraftVersion(state: com.example.ui.viewmodel.ServerWizardState, viewModel: ServerViewModel) {
    WizardCard(
        title = "Minecraft Version",
        subtitle = "Select the Minecraft release version. MobileCraft automatically matches the required Java runtime."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MinecraftVersion.KNOWN_VERSIONS.forEach { ver ->
                val isSelected = ver.version == state.minecraftVersion
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            1.dp,
                            if (isSelected) SleekPrimary else SleekBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            viewModel.updateWizard { s ->
                                s.copy(
                                    minecraftVersion = ver.version,
                                    serverSoftware = if (ver.supportedSoftware.contains(s.serverSoftware)) s.serverSoftware else ver.supportedSoftware.first()
                                )
                            }
                        },
                    color = if (isSelected) SleekPrimaryContainer.copy(alpha = 0.5f) else SleekSurface
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
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = SleekPrimary)
                            )
                            Column {
                                Text(
                                    text = "Minecraft ${ver.version}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextWhite
                                )
                                Text(
                                    text = "Official Release • Java ${ver.requiredJavaVersion}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                        MonospacePill(text = "Java ${ver.requiredJavaVersion}")
                    }
                }
            }
        }
    }
}

@Composable
fun Step3ServerSoftware(state: com.example.ui.viewmodel.ServerWizardState, viewModel: ServerViewModel) {
    val verObj = MinecraftVersion.KNOWN_VERSIONS.find { it.version == state.minecraftVersion }
        ?: MinecraftVersion(state.minecraftVersion)

    WizardCard(
        title = "Server Software",
        subtitle = "Choose server type. Options filtered based on compatibility with Minecraft ${state.minecraftVersion}."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ServerSoftware.entries.forEach { sw ->
                val isCompatible = verObj.supportedSoftware.contains(sw)
                val isSelected = sw == state.serverSoftware && isCompatible

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            1.dp,
                            if (isSelected) SleekPrimary else if (isCompatible) SleekBorder else SleekBorder.copy(alpha = 0.3f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable(enabled = isCompatible) {
                            viewModel.updateWizard { s -> s.copy(serverSoftware = sw) }
                        },
                    color = if (isSelected) SleekPrimaryContainer.copy(alpha = 0.5f) else SleekSurface.copy(alpha = if (isCompatible) 1f else 0.4f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    enabled = isCompatible,
                                    colors = RadioButtonDefaults.colors(selectedColor = SleekPrimary)
                                )
                                Text(
                                    text = sw.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCompatible) TextWhite else TextMuted
                                )
                            }

                            if (!isCompatible) {
                                Surface(shape = RoundedCornerShape(50), color = SleekCoralContainer) {
                                    Text(
                                        text = "Incompatible with ${state.minecraftVersion}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SleekCoral
                                    )
                                }
                            } else if (sw.supportsPlugins) {
                                Surface(shape = RoundedCornerShape(50), color = SleekPrimaryContainer) {
                                    Text(
                                        text = "Bukkit/Paper Plugins",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SleekOnPrimaryContainer
                                    )
                                }
                            } else if (sw.supportsMods) {
                                Surface(shape = RoundedCornerShape(50), color = SleekAmberContainer) {
                                    Text(
                                        text = "Mods Enabled",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SleekAmber
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sw.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCompatible) TextSecondary else TextMuted.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Step4RamAllocation(
    state: com.example.ui.viewmodel.ServerWizardState,
    memInfo: com.example.ui.viewmodel.DeviceMemoryInfo,
    viewModel: ServerViewModel
) {
    WizardCard(
        title = "RAM Memory Allocation",
        subtitle = "Assign memory to the server JVM. We have detected your phone's hardware parameters."
    ) {
        // Device Memory Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = SleekSurface
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Device RAM:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("${memInfo.totalMb} MB (~${String.format("%.1f", memInfo.totalGb)} GB)", style = MaterialTheme.typography.bodySmall, color = TextWhite, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Available Free RAM:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("${memInfo.availableMb} MB", style = MaterialTheme.typography.bodySmall, color = SleekMint, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recommended Max Server RAM:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("${memInfo.recommendedMb} MB", style = MaterialTheme.typography.bodySmall, color = SleekAmber, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Allocated Max RAM: ${state.maxRamMb} MB (${String.format("%.1f", state.maxRamMb / 1024f)} GB)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = SleekPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Presets
        val presets = listOf(1024, 2048, 3072, 4096, 6144)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { mb ->
                val isSafe = mb <= memInfo.totalMb * 0.75f
                val isSelected = state.maxRamMb == mb
                OutlinedButton(
                    onClick = {
                        if (isSafe) {
                            viewModel.updateWizard { s -> s.copy(maxRamMb = mb, minRamMb = (mb / 2).coerceAtLeast(512)) }
                        }
                    },
                    enabled = isSafe,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) SleekPrimaryContainer else Color.Transparent,
                        contentColor = if (isSelected) SleekOnPrimaryContainer else TextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) SleekPrimary else SleekOutline)
                    ),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("${mb / 1024}G", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Custom Slider
        val maxAllowed = (memInfo.totalMb * 0.75f).coerceAtLeast(2048f)
        Slider(
            value = state.maxRamMb.toFloat(),
            onValueChange = {
                val rounded = (it.toInt() / 256) * 256
                viewModel.updateWizard { s -> s.copy(maxRamMb = rounded, minRamMb = (rounded / 2).coerceAtLeast(512)) }
            },
            valueRange = 512f..maxAllowed,
            steps = ((maxAllowed - 512) / 256).toInt(),
            colors = SliderDefaults.colors(
                thumbColor = SleekPrimary,
                activeTrackColor = SleekPrimary,
                inactiveTrackColor = SleekOutline
            )
        )
    }
}

@Composable
fun Step5StorageDirectory(
    state: com.example.ui.viewmodel.ServerWizardState,
    storage: com.example.data.storage.StorageTelemetry
) {
    WizardCard(
        title = "Server Storage & Directory",
        subtitle = "Server files are stored in your secure application data directory with full isolation."
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = SleekSurface
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Available Device Storage:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("${String.format("%.2f", storage.availableGb)} GB Free", style = MaterialTheme.typography.bodySmall, color = SleekMint, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Required Initial Space:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("~350 MB", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                }
                HorizontalDivider(color = SleekBorder)
                Text(
                    text = "Storage Location:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "/data/user/0/com.aistudio.mobilecraft.srvmng/files/servers/[server_id]",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun Step6AdvancedConfig(state: com.example.ui.viewmodel.ServerWizardState, viewModel: ServerViewModel) {
    WizardCard(
        title = "Server Settings & Networking",
        subtitle = "Configure network port, gamemode, difficulty, and player rules."
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.port.toString(),
                onValueChange = {
                    val p = it.toIntOrNull() ?: 25565
                    viewModel.updateWizard { s -> s.copy(port = p) }
                },
                label = { Text("Server Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = customTextFieldColors(),
                shape = RoundedCornerShape(20.dp)
            )

            OutlinedTextField(
                value = state.maxPlayers.toString(),
                onValueChange = {
                    val mp = it.toIntOrNull() ?: 10
                    viewModel.updateWizard { s -> s.copy(maxPlayers = mp) }
                },
                label = { Text("Max Players") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = customTextFieldColors(),
                shape = RoundedCornerShape(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Switches
        SwitchRow(
            title = "Online Mode (Mojang Auth)",
            subtitle = "Enable for Mojang verification, or disable for offline/LAN play without internet account checks.",
            checked = state.onlineMode,
            onCheckedChange = { viewModel.updateWizard { s -> s.copy(onlineMode = it) } }
        )

        SwitchRow(
            title = "Enable PvP",
            subtitle = "Allow player vs player combat in worlds.",
            checked = state.pvp,
            onCheckedChange = { viewModel.updateWizard { s -> s.copy(pvp = it) } }
        )

        SwitchRow(
            title = "Auto-Restart on Crash",
            subtitle = "Automatically restart the server if terminated with backoff safety.",
            checked = state.autoRestart,
            onCheckedChange = { viewModel.updateWizard { s -> s.copy(autoRestart = it) } }
        )
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextWhite)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SleekPrimary,
                checkedTrackColor = SleekPrimaryContainer,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SleekOutline
            )
        )
    }
}

@Composable
fun InstallationProgressView(
    progress: Float,
    statusText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(28.dp)),
            color = SleekCard
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(64.dp),
                    color = SleekPrimary,
                    trackColor = SleekOutline,
                    strokeWidth = 6.dp
                )

                Text(
                    text = "Building Minecraft Server",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = TextWhite
                )

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = SleekPrimary,
                    trackColor = SleekOutline
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun WizardCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(28.dp)),
        color = SleekCard
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SleekPrimary,
    unfocusedBorderColor = SleekOutline,
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedContainerColor = SleekSurfaceVariant,
    unfocusedContainerColor = SleekSurfaceVariant,
    focusedLabelColor = SleekPrimary,
    unfocusedLabelColor = TextMuted
)
