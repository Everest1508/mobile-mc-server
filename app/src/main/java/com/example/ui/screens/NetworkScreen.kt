package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MonospacePill
import com.example.ui.theme.*
import com.example.ui.viewmodel.ServerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    viewModel: ServerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localIp = remember { viewModel.networkManager.getLocalIpAddress() }
    val isWifi = remember { viewModel.networkManager.isWifiConnected() }
    var copiedToClipboard by remember { mutableStateOf(false) }

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
                // Header
                Column {
                    Text(
                        text = "Network & Connectivity",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.5).sp
                        ),
                        color = TextWhite
                    )
                    Text(
                        text = "Connect friends locally or over the internet",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekPrimary
                    )
                }
            }

            // Local IP Address Card
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
                                text = "Local Wi-Fi Address",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextWhite
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isWifi) SleekMintContainer else SleekCoralContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isWifi) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                        contentDescription = null,
                                        tint = if (isWifi) SleekMint else SleekCoral,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isWifi) "Wi-Fi Connected" else "No Wi-Fi",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isWifi) SleekMint else SleekCoral
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = SleekSurface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "MINECRAFT DIRECT CONNECT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            fontSize = 10.sp
                                        ),
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$localIp:25565",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = SleekPrimary
                                    )
                                }

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Server IP", "$localIp:25565"))
                                        copiedToClipboard = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (copiedToClipboard) SleekMint else SleekPrimary,
                                        contentColor = if (copiedToClipboard) SleekOnMint else SleekOnPrimary
                                    ),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("copy_ip_button")
                                ) {
                                    Icon(
                                        imageVector = if (copiedToClipboard) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (copiedToClipboard) "Copied!" else "Copy IP", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Connection Guides
            item {
                Text(
                    text = "CONNECTION GUIDES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item {
                GuideCard(
                    title = "1. Local Network (LAN) Play",
                    description = "Anyone connected to the same Wi-Fi router can join immediately without port forwarding.",
                    steps = listOf(
                        "Ensure your phone and friends' computers are on the same Wi-Fi network.",
                        "Start your server in MobileCraft.",
                        "In Minecraft Java Edition, go to Multiplayer -> Direct Connect.",
                        "Enter the IP address shown above ($localIp:25565) and hit Join Server!"
                    ),
                    icon = Icons.Filled.Lan
                )
            }

            item {
                GuideCard(
                    title = "2. Tunneling (Playit.gg / Ngrok)",
                    description = "Play with friends anywhere across the globe without touching your home router settings.",
                    steps = listOf(
                        "Install the Playit.gg plugin or client tunnel to establish a secure tunnel.",
                        "Playit generates a public domain (e.g. yourserver.playit.gg).",
                        "Share this link with your friends on Discord or chat to let them connect instantly."
                    ),
                    icon = Icons.Filled.Public
                )
            }

            item {
                GuideCard(
                    title = "3. Port Forwarding (Router Admin)",
                    description = "For dedicated internet hosting with direct public IP.",
                    steps = listOf(
                        "Log in to your router gateway (typically 192.168.1.1 or 192.168.0.1).",
                        "Navigate to Port Forwarding / NAT rules.",
                        "Create a TCP & UDP rule for External Port 25565 -> Internal IP $localIp (Port 25565).",
                        "Friends can join using your public WAN IP."
                    ),
                    icon = Icons.Filled.Router
                )
            }

            item {
                GuideCard(
                    title = "4. Bedrock Crossplay (GeyserMC + Floodgate)",
                    description = "Allow friends on Mobile (iOS / Android Bedrock), Xbox, Switch, and PlayStation to join this Java server.",
                    steps = listOf(
                        "Create or use a Paper server in MobileCraft.",
                        "Upload the Geyser-Spigot.jar and Floodgate.jar into the /plugins/ folder.",
                        "Restart the server.",
                        "Bedrock players can now join on Port 19132 (UDP) while Java players join on 25565!"
                    ),
                    icon = Icons.Filled.SportsEsports
                )
            }
        }
    }
}

@Composable
fun GuideCard(
    title: String,
    description: String,
    steps: List<String>,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp)),
        color = SleekCard
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(22.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextWhite)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${idx + 1}.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = SleekPrimary
                        )
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
