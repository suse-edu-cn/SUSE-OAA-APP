package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity
import com.suseoaa.projectoaa.shared.domain.model.grade.GradeEntity
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.Flow

/**
 * SchoolGradeRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface SchoolGradeRepository {

    fun observeGrades(studentId: String, xnm: String, xqm: String): Flow<List<GradeEntity>>

    fun observeAllGrades(studentId: String): Flow<List<GradeEntity>>

    suspend fun fetchAllHistoryGrades(account: CourseAccountEntity): Result<String>

    suspend fun fetchCurrentTermGrades(account: CourseAccountEntity, year: String, semester: String): Result<String>
}
