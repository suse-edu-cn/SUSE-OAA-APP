package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.database.ValueCalculatorItem
import kotlinx.coroutines.flow.Flow

/**
 * ValueCalculatorRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface ValueCalculatorRepository {

    fun getAllItems(): Flow<List<ValueCalculatorItem>>

    suspend fun insertItem(
        itemName: String,
        price: Double,
        purchaseDateMillis: Long,
        createdAtMillis: Long
        ): Unit

    suspend fun deleteItem(id: Long): Unit
}
