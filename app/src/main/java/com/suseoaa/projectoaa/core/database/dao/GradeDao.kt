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

    // 当传入具体年份（如 "2024"）时，LIKE "2024" 等同于 = "2024"，不会影响原有成绩查询功能
    @Query(
        """
        SELECT * FROM grades 
        WHERE studentId = :studentId 
          AND xnm LIKE :xnm 
          AND xqm LIKE :xqm
    """
    )
    fun getGradesFlow(studentId: String, xnm: String, xqm: String): Flow<List<GradeEntity>>

    // 删除指定学期的旧数据 (此处保持 = 即可，因为删除通常是针对特定学期)
    @Query("DELETE FROM grades WHERE studentId = :studentId AND xnm = :xnm AND xqm = :xqm")
    suspend fun deleteGrades(studentId: String, xnm: String, xqm: String)

    // 事务操作：先删后存
    @Transaction
    suspend fun updateGrades(
        studentId: String,
        xnm: String,
        xqm: String,
        newGrades: List<GradeEntity>
    ) {
        deleteGrades(studentId, xnm, xqm)
        insertGrades(newGrades)
    }

    @Query(
        """
        UPDATE grades 
        SET regularScore = :regular, finalScore = :finalScore 
        WHERE studentId = :studentId AND courseId = :courseId AND xnm = :xnm AND xqm = :xqm
    """
    )
    suspend fun updateGradeDetail(
        studentId: String,
        courseId: String,
        xnm: String,
        xqm: String,
        regular: String,
        finalScore: String
    )
}