package com.suseoaa.projectoaa.courseList.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.suseoaa.projectoaa.courseList.data.dao.CourseDao
import com.suseoaa.projectoaa.courseList.data.entity.ClassTimeEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseAccountEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseEntity

// 升级数据库版本
@Database(
    entities = [CourseEntity::class, ClassTimeEntity::class, CourseAccountEntity::class],
    version = 6,
    exportSchema = false
)
abstract class CourseDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao

    companion object {
        @Volatile
        private var INSTANCE: CourseDatabase? = null

        fun getInstance(context: Context): CourseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): CourseDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CourseDatabase::class.java,
                "course_schedule.db"
            )
                .fallbackToDestructiveMigration(true) // 允许破坏性迁移
                .build()
        }
    }
}