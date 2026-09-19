package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.teachingplan.*
import io.ktor.client.call.*
import io.ktor.client.statement.*

/**
 * TeachingPlanRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface TeachingPlanRepository {

    suspend fun getCollegeList(): Result<List<CollegeOption>>

    suspend fun getMajorList(collegeId: String): Result<List<MajorOption>>

    suspend fun getTeachingPlanInfo(
        collegeId: String,
        gradeId: String,
        majorId: String
        ): Result<TeachingPlanInfo?>

    suspend fun getStudyRequirementCourses(
        requirementNodeId: String
        ): Result<List<StudyRequirementCourse>>

    suspend fun getCourseInfoList(
        planId: String,
        suggestedYear: String = "",
        suggestedSemester: String = "",
        courseCode: String = "",
        studyType: String = "",
        showCount: Int = 1000,
    ): Result<CourseInfoListResponse>

    suspend fun getCourseInfoListWithAuth(
        studentId: String,
        password: String,
        planId: String,
        suggestedYear: String = "",
        suggestedSemester: String = "",
        courseCode: String = "",
        studyType: String): Result<CourseInfoListResponse>

    suspend fun getStudentPlanId(
        studentId: String,
        password: String,
        collegeId: String,
        gradeId: String,
        majorId: String
        ): Result<String>

    fun generateGradeList(): List<String>

    fun groupCoursesByType(courses: List<CourseInfoItem>): Map<String, List<CourseInfoItem>>

    fun groupCoursesBySemester(courses: List<CourseInfoItem>): Map<String, List<CourseInfoItem>>

    fun calculateTotalCredits(courses: List<CourseInfoItem>): Double
}
