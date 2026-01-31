package com.suseoaa.projectoaa.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.suseoaa.projectoaa.database.CourseDatabase

actual class CourseDatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = NativeSqliteDriver(CourseDatabase.Schema, "course.db")
        
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
        
        return driver
    }
}
