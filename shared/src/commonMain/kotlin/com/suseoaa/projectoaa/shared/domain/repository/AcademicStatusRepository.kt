package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.teachingplan.*
import io.ktor.client.statement.*

/**
 * AcademicStatusRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface AcademicStatusRepository {

    suspend fun getAcademicStatusCategories(studentId: String): Result<Pair<AcademicPlanOverview, List<AcademicStatusCategory>>>

    suspend fun getCategoryCourses(
        categoryId: String,
        studentId: String
        ): Result<List<AcademicStatusCourseItem>>

    suspend fun getOtherCourses(
        studentId: String,
        htmlContent: String
        ): Result<List<AcademicStatusCourseItem>>

    suspend fun getNonPlanCourses(
        categoryId: String,
        studentId: String
        ): Result<List<AcademicStatusCourseItem>>

    fun calculateCategoryStats(courses: List<AcademicStatusCourseItem>): AcademicStatusCategory

    fun calculateWeightedGpa(allCourses: List<AcademicStatusCourseItem>): Double
}
