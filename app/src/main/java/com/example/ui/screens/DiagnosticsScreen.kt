package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entities.JavaRuntimeEntity
import com.example.ui.components.MonospacePill
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.ServerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: ServerViewModel,
    modifier: Modifier = Modifier
) {
    val memInfo = remember { viewModel.getDeviceMemoryInfo() }
    val storage = remember { viewModel.storageManager.getStorageTelemetry() }
    val runtimes by viewModel.installedRuntimes.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SleekBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Diagnostics & System",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.5).sp
                        ),
                        color = TextWhite
                    )
                    Text(
                        text = "Device hardware & OpenJDK runtime environments",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekPrimary
                    )
                }
            }

            // Hardware Telemetry Grid
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val ramProgress = if (memInfo.totalMb > 0) (memInfo.totalMb - memInfo.availableMb).toFloat() / memInfo.totalMb.toFloat() else 0f
                    StatCard(
                        title = "Device RAM",
                        value = "${memInfo.totalMb}",
                        subtitle = "MB",
                        icon = Icons.Filled.Memory,
                        accentColor = SleekPrimary,
                        progress = ramProgress,
                        modifier = Modifier.weight(1f)
                    )

                    val usedStorageGb = (storage.totalGb - storage.availableGb).coerceAtLeast(0.1f)
                    val storageProgress = if (storage.totalGb > 0) usedStorageGb / storage.totalGb else 0f
                    StatCard(
                        title = "Storage",
                        value = String.format("%.1f", storage.totalGb),
                        subtitle = "GB",
                        icon = Icons.Filled.Storage,
                        accentColor = SleekPrimary,
                        progress = storageProgress,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Architecture & OS Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(28.dp)),
                    color = SleekCard
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Hardware & Environment",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextWhite
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("CPU Architecture (ABI):", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            MonospacePill(text = viewModel.runtimeManager.supportedAbi)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Android Version:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", style = MaterialTheme.typography.bodySmall, color = TextWhite, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Device Model:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("${Build.MANUFACTURER} ${Build.MODEL}", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Foreground Service:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Active with WakeLock", style = MaterialTheme.typography.bodySmall, color = SleekMint, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Java Runtimes Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(28.dp)),
                    color = SleekCard
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Installed Java Runtimes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextWhite
                            )
                            Icon(Icons.Filled.Code, contentDescription = null, tint = SleekPrimary)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val versions = listOf(8, 17, 21)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            versions.forEach { ver ->
                                val installed = runtimes.any { it.versionNumber == ver && it.isInstalled }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp)),
                                    color = SleekSurface
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
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (installed) Icons.Filled.CheckCircle else Icons.Outlined.Download,
                                                contentDescription = null,
                                                tint = if (installed) SleekMint else TextMuted
                                            )
                                            Column {
                                                Text(
                                                    text = "OpenJDK $ver (${viewModel.runtimeManager.supportedAbi})",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = TextWhite
                                                )
                                                Text(
                                                    text = if (ver == 21) "Used by MC 1.20.5 - 1.21+" else if (ver == 17) "Used by MC 1.18 - 1.20.4" else "Used by MC 1.12 - 1.16.5",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }

                                        if (!installed) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        viewModel.runtimeManager.getOrInstallRuntime(ver) { _, _ -> }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimaryContainer, contentColor = SleekOnPrimaryContainer),
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Text("Install", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            MonospacePill(text = "Ready")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // OEM Background Optimization Warning
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, SleekAmber.copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
                    color = SleekCard
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.BatteryAlert, contentDescription = null, tint = SleekAmber)
                            Text(
                                text = "Keep Server Active in Background",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SleekAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "To prevent Android battery optimizers (Samsung Sleeping Apps, Xiaomi MIUI Battery Saver, or Pixel Adaptive Battery) from suspending your Minecraft server process while screen is locked:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Set MobileCraft Battery Usage to 'Unrestricted' in Android App Settings.\n• Keep phone connected to a charger for prolonged multiplayer sessions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
