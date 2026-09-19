package com.suseoaa.projectoaa.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.suseoaa.projectoaa.shared.database.CourseDatabase
import com.suseoaa.projectoaa.shared.database.ValueCalculatorItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import com.suseoaa.projectoaa.shared.domain.repository.ValueCalculatorRepository

class ValueCalculatorRepositoryImpl(
    private val database: CourseDatabase
) : ValueCalculatorRepository {
    private val queries = database.valueCalculatorItemQueries

    override fun getAllItems(): Flow<List<ValueCalculatorItem>> {
        return queries.getAllItems()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    override suspend fun insertItem(
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

    override suspend fun deleteItem(id: Long) {
        queries.deleteItem(id)
    }
}
