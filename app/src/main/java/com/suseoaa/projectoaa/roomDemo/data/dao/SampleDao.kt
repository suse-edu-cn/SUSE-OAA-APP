package com.suseoaa.projectoaa.roomDemo.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.suseoaa.projectoaa.roomDemo.data.entity.SampleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room数据库测试用的DAO
 * MVVM架构 - Model层（数据访问对象）
 * 提供完整的CRUD操作
 */
@Dao
interface SampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: SampleEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(samples: List<SampleEntity>)
    
    @Update
    suspend fun update(sample: SampleEntity)
    
    @Delete
    suspend fun delete(sample: SampleEntity)
    
    @Query("SELECT * FROM sample_data WHERE id = :id")
    suspend fun getById(id: Int): SampleEntity?
    
    @Query("SELECT * FROM sample_data ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SampleEntity>>
    
    @Query("SELECT * FROM sample_data WHERE name LIKE '%' || :query || '%'")
    fun searchByName(query: String): Flow<List<SampleEntity>>
    
    @Query("DELETE FROM sample_data")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM sample_data")
    suspend fun getCount(): Int
}
