package com.suseoaa.projectoaa.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.suseoaa.projectoaa.database.CourseDatabase

actual class CourseDatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(CourseDatabase.Schema, "course.db")
    }
}
