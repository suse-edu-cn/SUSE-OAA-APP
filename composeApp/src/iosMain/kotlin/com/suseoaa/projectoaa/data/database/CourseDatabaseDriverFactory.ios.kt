package com.suseoaa.projectoaa.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.suseoaa.projectoaa.database.CourseDatabase

actual class CourseDatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = NativeSqliteDriver(CourseDatabase.Schema, "course.db")

        // 确保 CheckinAccount 表存在（兼容旧版本数据库）
        try {
            driver.execute(
                null, """
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
            """.trimIndent(), 0
            )
        } catch (_: Exception) {
            // 表已存在或其他错误，忽略
        }

        // 迁移 ExamCache 表：为旧版本数据库添加新字段
        migrateExamCacheTable(driver)

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
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS idx_exam_student ON ExamCache(studentId)",
                0
            )
        } catch (_: Exception) {
        }

        try {
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS idx_exam_semester ON ExamCache(studentId, xnm, xqm)",
                0
            )
        } catch (_: Exception) {
        }
    }
}
