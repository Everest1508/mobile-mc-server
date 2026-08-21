package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.manager.NetworkManager
import com.example.data.process.ServerProcessManager
import com.example.data.provider.MinecraftDownloadManager
import com.example.data.runtime.JavaRuntimeManager
import com.example.data.storage.ServerStorageManager

class MobileCraftApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val storageManager by lazy { ServerStorageManager(this) }
    val runtimeManager by lazy { JavaRuntimeManager(this, database.javaRuntimeDao()) }
    val downloadManager by lazy { MinecraftDownloadManager() }
    val networkManager by lazy { NetworkManager(this) }
    val processManager by lazy {
        ServerProcessManager(this, database.serverDao(), storageManager, runtimeManager)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
