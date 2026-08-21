package com.example.data.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.text.format.Formatter
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkManager(private val context: Context) {

    fun getLocalIpAddress(): String {
        try {
            // First check Wi-Fi
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val wifiIp = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (wifiIp != 0) {
                @Suppress("DEPRECATION")
                val formatted = Formatter.formatIpAddress(wifiIp)
                if (formatted != "0.0.0.0" && formatted.isNotEmpty()) {
                    return formatted
                }
            }

            // Fallback to active Network Interfaces
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                val addresses = nif.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
