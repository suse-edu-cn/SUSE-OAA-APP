package com.suseoaa.projectoaa.roomDemo.data.repository

import com.suseoaa.projectoaa.roomDemo.data.database.SampleDatabase
import com.suseoaa.projectoaa.roomDemo.data.entity.SampleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room数据库测试仓库
 * MVVM架构 - Model层（Repository，封装数据访问逻辑）
 */
class SampleRepository(private val db: SampleDatabase) {

    fun getAllSamples(): Flow<List<SampleEntity>> = db.sampleDao().getAll()

    suspend fun getSampleById(id: Int): SampleEntity? = db.sampleDao().getById(id)

    fun searchSamples(query: String): Flow<List<SampleEntity>> = db.sampleDao().searchByName(query)

    suspend fun insertSample(sample: SampleEntity): Long = db.sampleDao().insert(sample)

    suspend fun insertSamples(samples: List<SampleEntity>) = db.sampleDao().insertAll(samples)

    suspend fun updateSample(sample: SampleEntity) = db.sampleDao().update(sample)

    suspend fun deleteSample(sample: SampleEntity) = db.sampleDao().delete(sample)

    suspend fun deleteAllSamples() = db.sampleDao().deleteAll()

    suspend fun getSampleCount(): Int = db.sampleDao().getCount()

    /**
     * 插入示例测试数据
     */
    suspend fun insertSampleData() {
        val samples = listOf(
            SampleEntity(name = "示例1", description = "这是第一个测试数据"),
            SampleEntity(name = "示例2", description = "这是第二个测试数据"),
            SampleEntity(name = "示例3", description = "这是第三个测试数据"),
            SampleEntity(name = "测试A", description = "Room数据库测试"),
            SampleEntity(name = "测试B", description = "MVVM架构演示")
        )
        insertSamples(samples)
    }
}
