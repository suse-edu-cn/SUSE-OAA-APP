package com.suseoaa.projectoaa.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.suseoaa.projectoaa.core.database.dao.AcademicDao
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.dao.GradeDao
import com.suseoaa.projectoaa.core.database.entity.ClassTimeEntity
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.CourseEntity
import com.suseoaa.projectoaa.core.database.entity.GradeEntity
import com.suseoaa.projectoaa.core.database.entity.ExamCacheEntity
import com.suseoaa.projectoaa.core.database.entity.MessageCacheEntity

@Database(
    entities = [
        CourseEntity::class,
        ClassTimeEntity::class,
        CourseAccountEntity::class,
        GradeEntity::class,
        ExamCacheEntity::class,
        MessageCacheEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class CourseDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun gradeDao(): GradeDao
    abstract fun academicDao(): AcademicDao

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
                .fallbackToDestructiveMigration(false)
                .build()
        }
    }
}