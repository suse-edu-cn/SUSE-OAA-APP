package com.suseoaa.projectoaa.data.database

import app.cash.sqldelight.db.SqlDriver

expect class CourseDatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
