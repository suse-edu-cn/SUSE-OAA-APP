package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamApiItem
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamCacheEntity
import com.suseoaa.projectoaa.shared.domain.model.message.MessageCacheEntity
import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.Flow

/**
 * SchoolInfoRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface SchoolInfoRepository {

    fun observeExams(studentId: String): Flow<List<ExamCacheEntity>>

    suspend fun refreshAcademicExamInfo(account: CourseAccountEntity): Result<String>

    suspend fun fetchExamsByTerm(
        account: CourseAccountEntity,
        year: String,
        semester: String
        ): Result<List<ExamApiItem>>

    suspend fun addCustomExam(exam: ExamCacheEntity): Unit

    suspend fun updateExam(exam: ExamCacheEntity): Unit

    suspend fun deleteExam(examId: Long): Unit

    suspend fun getCustomExamsBySemester(
        studentId: String,
        xnm: String,
        xqm: String
        ): List<ExamCacheEntity>

    fun observeExamsBySemester(
        studentId: String,
        xnm: String,
        xqm: String
        ): Flow<List<ExamCacheEntity>>

    fun observeMessages(studentId: String): Flow<List<MessageCacheEntity>>

    suspend fun refreshAcademicMessageInfo(account: CourseAccountEntity): Result<String>

    suspend fun updateMessageSummary(id: Long, summary: String): Unit

    suspend fun getAcademicCourseInfo(account: CourseAccountEntity): Result<List<String>>
}
