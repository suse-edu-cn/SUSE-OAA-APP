package com.suseoaa.projectoaa.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.suseoaa.projectoaa.shared.database.CourseDatabase
import com.suseoaa.projectoaa.shared.database.ValueCalculatorItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

class ValueCalculatorRepository(
    private val database: CourseDatabase
) {
    private val queries = database.valueCalculatorItemQueries

    fun getAllItems(): Flow<List<ValueCalculatorItem>> {
        return queries.getAllItems()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun insertItem(
        itemName: String,
        price: Double,
        purchaseDateMillis: Long,
        createdAtMillis: Long
    ) {
        queries.insertItem(
            itemName = itemName,
            price = price,
            purchaseDateMillis = purchaseDateMillis,
            createdAtMillis = createdAtMillis
        )
    }

    suspend fun deleteItem(id: Long) {
        queries.deleteItem(id)
    }
}
