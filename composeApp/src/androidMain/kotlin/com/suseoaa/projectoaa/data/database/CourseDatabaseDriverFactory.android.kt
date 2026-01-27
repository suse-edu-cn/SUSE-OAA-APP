package com.suseoaa.projectoaa.data.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.suseoaa.projectoaa.database.CourseDatabase

actual class CourseDatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(CourseDatabase.Schema, context, "course.db")
    }
}
