package com.suseoaa.projectoaa.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.suseoaa.projectoaa.core.database.entity.ExamCacheEntity
import com.suseoaa.projectoaa.core.database.entity.MessageCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicDao {
    // === 考试信息 ===
    @Query("SELECT * FROM cached_exams WHERE studentId = :studentId")
    fun getExamsFlow(studentId: String): Flow<List<ExamCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamCacheEntity>)

    @Query("DELETE FROM cached_exams WHERE studentId = :studentId")
    suspend fun clearExams(studentId: String)

    @Transaction
    suspend fun updateExams(studentId: String, newExams: List<ExamCacheEntity>) {
        clearExams(studentId)
        insertExams(newExams)
    }

    // === 调课通知 ===
    @Query("SELECT * FROM cached_messages WHERE studentId = :studentId ORDER BY date DESC")
    fun getMessagesFlow(studentId: String): Flow<List<MessageCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(msgs: List<MessageCacheEntity>)

    @Query("DELETE FROM cached_messages WHERE studentId = :studentId")
    suspend fun clearMessages(studentId: String)

    @Transaction
    suspend fun updateMessages(studentId: String, newMsgs: List<MessageCacheEntity>) {
        clearMessages(studentId)
        insertMessages(newMsgs)
    }
}