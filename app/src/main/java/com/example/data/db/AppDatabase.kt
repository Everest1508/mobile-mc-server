package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.db.dao.BackupDao
import com.example.data.db.dao.JavaRuntimeDao
import com.example.data.db.dao.ServerDao
import com.example.data.db.entities.BackupEntity
import com.example.data.db.entities.JavaRuntimeEntity
import com.example.data.db.entities.ServerEntity

@Database(
    entities = [ServerEntity::class, BackupEntity::class, JavaRuntimeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun backupDao(): BackupDao
    abstract fun javaRuntimeDao(): JavaRuntimeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mobilecraft_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
