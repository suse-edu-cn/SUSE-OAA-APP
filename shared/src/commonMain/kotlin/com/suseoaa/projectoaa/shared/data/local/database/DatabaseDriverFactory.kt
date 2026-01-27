package com.suseoaa.projectoaa.shared.data.local.database

import app.cash.sqldelight.db.SqlDriver

/**
 * 数据库驱动工厂接口
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
