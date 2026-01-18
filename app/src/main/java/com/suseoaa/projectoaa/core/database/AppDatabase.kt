package com.suseoaa.projectoaa.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.suseoaa.projectoaa.core.database.dao.AcademicDao
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.dao.GradeDao
import com.suseoaa.projectoaa.core.database.entity.ClassTimeEntity
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.CourseEntity
import com.suseoaa.projectoaa.core.database.entity.ExamCacheEntity
import com.suseoaa.projectoaa.core.database.entity.GradeEntity
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
    version = 12,
    exportSchema = false
)
abstract class CourseDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun gradeDao(): GradeDao
    abstract fun academicDao(): AcademicDao

    companion object {
        @Volatile
        private var INSTANCE: CourseDatabase? = null

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE grades ADD COLUMN jxbId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE grades ADD COLUMN regularScore TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE grades ADD COLUMN finalScore TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 添加 regularRatio 列
                db.execSQL("ALTER TABLE grades ADD COLUMN regularRatio TEXT NOT NULL DEFAULT ''")
                // 添加 finalRatio 列
                db.execSQL("ALTER TABLE grades ADD COLUMN finalRatio TEXT NOT NULL DEFAULT ''")
            }
        }

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
                // 注册所有迁移脚本
                .addMigrations(MIGRATION_10_11, MIGRATION_11_12)
                .fallbackToDestructiveMigration(false)
                .build()
        }
    }
}