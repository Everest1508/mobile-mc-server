package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MobileCraftApp
import kotlinx.coroutines.*

class MinecraftServerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var activeServerId: String? = null
    private var activeServerName: String = "Minecraft Server"

    companion object {
        const val CHANNEL_ID = "minecraft_server_service_channel"
        const val NOTIFICATION_ID = 25565
        const val ACTION_START = "ACTION_START_SERVER_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_SERVER_SERVICE"
        const val EXTRA_SERVER_ID = "EXTRA_SERVER_ID"
        const val EXTRA_SERVER_NAME = "EXTRA_SERVER_NAME"

        fun startService(context: Context, serverId: String, serverName: String) {
            val intent = Intent(context, MinecraftServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SERVER_ID, serverId)
                putExtra(EXTRA_SERVER_NAME, serverName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MinecraftServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                activeServerId?.let { id ->
                    val app = application as? MobileCraftApp
                    app?.let {
                        serviceScope.launch(Dispatchers.IO) {
                            it.processManager.stopServer(id)
                        }
                    }
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                activeServerId = intent.getStringExtra(EXTRA_SERVER_ID)
                activeServerName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Minecraft Server"
                startForeground(NOTIFICATION_ID, buildNotification("🟢 Server Online", "Running on local network • Port 25565"))
                observeServerStats()
            }
        }
        return START_STICKY
    }

    private fun observeServerStats() {
        val serverId = activeServerId ?: return
        val app = application as? MobileCraftApp ?: return

        serviceScope.launch {
            app.processManager.getStatsFlow(serverId).collect { stats ->
                val notif = buildNotification(
                    title = "🟢 $activeServerName Online",
                    content = "Players: ${stats.activePlayersCount} | RAM: ${stats.ramUsedMb}MB / ${stats.ramMaxMb}MB | CPU: ${stats.cpuUsagePercent.toInt()}%"
                )
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notif)
            }
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MinecraftServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP SERVER", stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "OPEN APP", openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Minecraft Server Host",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live running status and statistics for active Minecraft servers"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileCraft:ServerRunningLock").apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 hours max safety
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MobileCraft:WifiServerLock").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseLocks()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
