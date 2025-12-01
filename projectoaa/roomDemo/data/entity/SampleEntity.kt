package com.suseoaa.projectoaa.roomDemo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room数据库测试用的示例实体
 * MVVM架构 - Model层（数据实体）
 */
@Entity(tableName = "sample_data")
data class SampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
