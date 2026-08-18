package com.app.zpower.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.zpower.data.dao.*
import com.app.zpower.data.entity.*

@Database(
    entities = [
        ThermalArea::class,
        RoomEntity::class,
        PanelEntity::class,
        RelayEntity::class,
        ChildProcess::class,
        SubProcess::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ZPowerDatabase : RoomDatabase() {
    abstract fun thermalAreaDao(): ThermalAreaDao
    abstract fun roomDao(): RoomDao
    abstract fun panelDao(): PanelDao
    abstract fun relayDao(): RelayDao
    abstract fun childProcessDao(): ChildProcessDao
    abstract fun subProcessDao(): SubProcessDao
    abstract fun searchDao(): SearchDao

    companion object {
        @Volatile
        private var INSTANCE: ZPowerDatabase? = null

        fun getDatabase(context: Context): ZPowerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZPowerDatabase::class.java,
                    "zpower_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
