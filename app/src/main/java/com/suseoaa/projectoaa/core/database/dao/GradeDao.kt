package com.suseoaa.projectoaa.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.suseoaa.projectoaa.core.database.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    // 插入数据，如果主键冲突则替换 (用于更新成绩)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    // 实时查询：返回 Flow，当数据库变化时 UI 会自动更新
    @Query("""
        SELECT * FROM grades 
        WHERE studentId = :studentId 
          AND xnm = :xnm 
          AND xqm = :xqm
    """)
    fun getGradesFlow(studentId: String, xnm: String, xqm: String): Flow<List<GradeEntity>>

    // 删除指定学期的旧数据
    @Query("DELETE FROM grades WHERE studentId = :studentId AND xnm = :xnm AND xqm = :xqm")
    suspend fun deleteGrades(studentId: String, xnm: String, xqm: String)

    // 事务操作：先删后存，确保数据纯净 (防止服务器删除了某门课，本地却还留着)
    @Transaction
    suspend fun updateGrades(studentId: String, xnm: String, xqm: String, newGrades: List<GradeEntity>) {
        deleteGrades(studentId, xnm, xqm)
        insertGrades(newGrades)
    }
}