package com.suseoaa.projectoaa.roomDemo.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.suseoaa.projectoaa.roomDemo.data.dao.SampleDao
import com.suseoaa.projectoaa.roomDemo.data.entity.SampleEntity

/**
 * Room数据库测试数据库
 * MVVM架构 - Model层（数据库定义）
 */
@Database(
    entities = [SampleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SampleDatabase : RoomDatabase() {
    abstract fun sampleDao(): SampleDao
    
    companion object {
        @Volatile
        private var INSTANCE: SampleDatabase? = null
        
        fun getInstance(context: Context): SampleDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): SampleDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SampleDatabase::class.java,
                "sample_db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
