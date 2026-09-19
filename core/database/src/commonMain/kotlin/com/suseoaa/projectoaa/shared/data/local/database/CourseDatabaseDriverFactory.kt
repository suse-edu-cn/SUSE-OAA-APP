package com.suseoaa.projectoaa.shared.data.local.database

import app.cash.sqldelight.db.SqlDriver

expect class CourseDatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
