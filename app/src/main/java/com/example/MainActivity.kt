package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ServerViewModel

sealed class AppScreen {
    data object Servers : AppScreen()
    data object Network : AppScreen()
    data object Diagnostics : AppScreen()
    data object CreateServerWizard : AppScreen()
    data class ServerDashboard(val serverId: String) : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MobileCraftApp

        setContent {
            MobileCraftTheme {
                val viewModel: ServerViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ServerViewModel(
                                context = applicationContext,
                                database = app.database,
                                storageManager = app.storageManager,
                                runtimeManager = app.runtimeManager,
                                downloadManager = app.downloadManager,
                                processManager = app.processManager,
                                networkManager = app.networkManager
                            ) as T
                        }
                    }
                )

                var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Servers) }
                val userMessage by viewModel.userMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(userMessage) {
                    userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearUserMessage()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SleekBg,
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = SleekSurfaceVariant,
                                contentColor = TextPrimary,
                                actionColor = SleekPrimary,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            )
                        }
                    },
                    bottomBar = {
                        // Only show bottom navigation on root top-level tabs
                        if (currentScreen is AppScreen.Servers || currentScreen is AppScreen.Network || currentScreen is AppScreen.Diagnostics) {
                            NavigationBar(
                                containerColor = SleekBg,
                                contentColor = SleekPrimary,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen is AppScreen.Servers,
                                    onClick = { currentScreen = AppScreen.Servers },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentScreen is AppScreen.Servers) Icons.Filled.Dns else Icons.Outlined.Dns,
                                            contentDescription = "Servers"
                                        )
                                    },
                                    label = {
                                        Text(
                                            "Servers",
                                            fontWeight = if (currentScreen is AppScreen.Servers) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SleekOnPrimaryContainer,
                                        selectedTextColor = SleekPrimary,
                                        indicatorColor = SleekPrimaryContainer,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_servers")
                                )

                                NavigationBarItem(
                                    selected = currentScreen is AppScreen.Network,
                                    onClick = { currentScreen = AppScreen.Network },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentScreen is AppScreen.Network) Icons.Filled.Lan else Icons.Outlined.Lan,
                                            contentDescription = "Network"
                                        )
                                    },
                                    label = {
                                        Text(
                                            "Network",
                                            fontWeight = if (currentScreen is AppScreen.Network) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SleekOnPrimaryContainer,
                                        selectedTextColor = SleekPrimary,
                                        indicatorColor = SleekPrimaryContainer,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_network")
                                )

                                NavigationBarItem(
                                    selected = currentScreen is AppScreen.Diagnostics,
                                    onClick = { currentScreen = AppScreen.Diagnostics },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentScreen is AppScreen.Diagnostics) Icons.Filled.Tune else Icons.Outlined.Tune,
                                            contentDescription = "System"
                                        )
                                    },
                                    label = {
                                        Text(
                                            "System",
                                            fontWeight = if (currentScreen is AppScreen.Diagnostics) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SleekOnPrimaryContainer,
                                        selectedTextColor = SleekPrimary,
                                        indicatorColor = SleekPrimaryContainer,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_diagnostics")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (val screen = currentScreen) {
                            is AppScreen.Servers -> {
                                ServerListScreen(
                                    viewModel = viewModel,
                                    onOpenServer = { serverId ->
                                        currentScreen = AppScreen.ServerDashboard(serverId)
                                    },
                                    onCreateServer = {
                                        currentScreen = AppScreen.CreateServerWizard
                                    }
                                )
                            }
                            is AppScreen.Network -> {
                                NetworkScreen(viewModel = viewModel)
                            }
                            is AppScreen.Diagnostics -> {
                                DiagnosticsScreen(viewModel = viewModel)
                            }
                            is AppScreen.CreateServerWizard -> {
                                ServerWizardScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = AppScreen.Servers },
                                    onServerCreated = { serverId ->
                                        currentScreen = AppScreen.ServerDashboard(serverId)
                                    }
                                )
                            }
                            is AppScreen.ServerDashboard -> {
                                ServerDashboardScreen(
                                    serverId = screen.serverId,
                                    viewModel = viewModel,
                                    onBack = { currentScreen = AppScreen.Servers }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
