package com.suseoaa.projectoaa.shared.data.local.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.suseoaa.projectoaa.shared.database.CourseDatabase

actual class CourseDatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(
            schema = CourseDatabase.Schema,
            context = context,
            name = "course.db",
            callback = AndroidSqliteDriver.Callback(CourseDatabase.Schema)
        )
        
        // 确保 CheckinAccount 表存在（兼容旧版本数据库）
        try {
            driver.execute(null, """
                CREATE TABLE IF NOT EXISTS CheckinAccount (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    studentId TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    name TEXT NOT NULL DEFAULT '',
                    remark TEXT NOT NULL DEFAULT '',
                    lastCheckinTime TEXT,
                    lastCheckinStatus TEXT,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL
                )
            """.trimIndent(), 0)
        } catch (_: Exception) {
            // 表已存在或其他错误，忽略
        }
        
        // 迁移 ExamCache 表：为旧版本数据库添加新字段
        migrateExamCacheTable(driver)
        
        // 迁移近场签到表
        migrateNearFieldCheckinTables(driver)
        
        return driver
    }
    
    /**
     * 迁移 ExamCache 表，为旧版本数据库添加新字段
     */
    private fun migrateExamCacheTable(driver: SqlDriver) {
        // 添加新字段的迁移列表
        val alterStatements = listOf(
            "ALTER TABLE ExamCache ADD COLUMN credit TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ExamCache ADD COLUMN examType TEXT NOT NULL DEFAULT '考试'",
            "ALTER TABLE ExamCache ADD COLUMN examName TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ExamCache ADD COLUMN yearSemester TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ExamCache ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ExamCache ADD COLUMN xnm TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ExamCache ADD COLUMN xqm TEXT NOT NULL DEFAULT ''"
        )
        
        // 逐个执行 ALTER TABLE，忽略已存在字段的错误
        alterStatements.forEach { sql ->
            try {
                driver.execute(null, sql, 0)
            } catch (_: Exception) {
                // 字段已存在或其他错误，忽略
            }
        }
        
        // 确保索引存在
        try {
            driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_exam_student ON ExamCache(studentId)", 0)
        } catch (_: Exception) {}
        
        try {
            driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_exam_semester ON ExamCache(studentId, xnm, xqm)", 0)
        } catch (_: Exception) {}
    }

    /**
     * 迁移近场签到相关的表
     */
    private fun migrateNearFieldCheckinTables(driver: SqlDriver) {
        try {
            driver.execute(null, """
                CREATE TABLE IF NOT EXISTS NearFieldTask (
                    taskIdentifier TEXT PRIMARY KEY NOT NULL,
                    activityName TEXT NOT NULL,
                    hostName TEXT NOT NULL,
                    startTime INTEGER NOT NULL,
                    endTime INTEGER NOT NULL,
                    publishTimestamp INTEGER NOT NULL,
                    securityNonce TEXT NOT NULL,
                    isMyHosted INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent(), 0)
            
            driver.execute(null, """
                CREATE TABLE IF NOT EXISTS NearFieldParticipant (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    taskIdentifier TEXT NOT NULL,
                    participantName TEXT NOT NULL,
                    participantId TEXT NOT NULL,
                    checkinTime INTEGER NOT NULL,
                    FOREIGN KEY (taskIdentifier) REFERENCES NearFieldTask(taskIdentifier) ON DELETE CASCADE
                )
            """.trimIndent(), 0)

            // 迁移现有表，添加 status 字段（兼容已创建表的情况）
            try {
                driver.execute(null, "ALTER TABLE NearFieldParticipant ADD COLUMN status TEXT NOT NULL DEFAULT '正常'", 0)
            } catch (_: Exception) {}

            // 增加唯一索引防止重复签到
            try {
                driver.execute(null, "CREATE UNIQUE INDEX IF NOT EXISTS idx_participant_task_student ON NearFieldParticipant(taskIdentifier, participantId)", 0)
            } catch (_: Exception) {}
        } catch (_: Exception) {
            // 忽略创建错误
        }
    }
}
