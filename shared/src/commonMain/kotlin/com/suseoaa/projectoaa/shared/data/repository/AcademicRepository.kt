package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.AcademicApi
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamItem
import com.suseoaa.projectoaa.shared.domain.model.grade.FilterType
import com.suseoaa.projectoaa.shared.domain.model.grade.GpaCourseWrapper
import com.suseoaa.projectoaa.shared.domain.model.grade.GpaStats
import com.suseoaa.projectoaa.shared.domain.model.grade.GradeItem
import com.suseoaa.projectoaa.shared.domain.model.grade.SortOrder
import com.suseoaa.projectoaa.shared.domain.model.message.AcademicMessageItem
import kotlin.math.roundToInt

/**
 * 教务仓库 (成绩、考试、消息)
 */
class AcademicRepository(
    private val academicApi: AcademicApi
) {
    /**
     * 获取学生成绩列表
     */
    suspend fun getGrades(xnm: String? = null, xqm: String? = null): Result<List<GradeItem>> {
        return try {
            val response = academicApi.getStudentGrades(xnm, xqm)
            Result.Success(response.items ?: emptyList())
        } catch (e: Exception) {
            Result.Error("获取成绩失败: ${e.message}", exception = e)
        }
    }

    /**
     * 获取考试信息列表
     */
    suspend fun getExams(): Result<List<ExamItem>> {
        return try {
            val response = academicApi.getExamInfo()
            Result.Success(response.items ?: emptyList())
        } catch (e: Exception) {
            Result.Error("获取考试信息失败: ${e.message}", exception = e)
        }
    }

    /**
     * 获取教务消息
     */
    suspend fun getAcademicMessages(): Result<List<AcademicMessageItem>> {
        return try {
            val response = academicApi.getAcademicMessages()
            Result.Success(response.items ?: emptyList())
        } catch (e: Exception) {
            Result.Error("获取教务消息失败: ${e.message}", exception = e)
        }
    }

    /**
     * 计算 GPA 统计
     */
    fun calculateGpaStats(courses: List<GpaCourseWrapper>): GpaStats {
        var totalCredits = 0.0
        var totalPoints = 0.0
        var degreeCredits = 0.0
        var degreePoints = 0.0

        for (course in courses) {
            val credit = course.credit
            val gpa = course.displayGpa.toDoubleOrNull() ?: 0.0
            val points = credit * gpa

            totalCredits += credit
            totalPoints += points

            if (course.isDegreeCourse) {
                degreeCredits += credit
                degreePoints += points
            }
        }

        val totalGpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
        val degreeGpa = if (degreeCredits > 0) degreePoints / degreeCredits else 0.0

        return GpaStats(
            totalGpa = totalGpa.formatDecimal(2),
            totalCredits = totalCredits.formatDecimal(1),
            degreeGpa = degreeGpa.formatDecimal(2),
            degreeCredits = degreeCredits.formatDecimal(1)
        )
    }

    /**
     * 将成绩项转换为 GPA 课程包装器
     */
    fun toGpaCourseWrappers(grades: List<GradeItem>): List<GpaCourseWrapper> {
        return grades.map { grade ->
            GpaCourseWrapper(
                originalEntity = grade,
                isDegreeCourse = grade.sfxwkc == "是"
            )
        }
    }

    /**
     * 过滤和排序课程
     */
    fun filterAndSortCourses(
        courses: List<GpaCourseWrapper>,
        filterType: FilterType,
        sortOrder: SortOrder
    ): List<GpaCourseWrapper> {
        val filtered = when (filterType) {
            FilterType.ALL -> courses
            FilterType.DEGREE_ONLY -> courses.filter { it.isDegreeCourse }
        }

        return when (sortOrder) {
            SortOrder.DESCENDING -> filtered.sortedByDescending { it.displayGpa.toDoubleOrNull() ?: 0.0 }
            SortOrder.ASCENDING -> filtered.sortedBy { it.displayGpa.toDoubleOrNull() ?: 0.0 }
        }
    }

    /**
     * 格式化浮点数为指定小数位数的字符串
     */
    private fun Double.formatDecimal(decimals: Int): String {
        var factor = 1.0
        repeat(decimals) { factor *= 10.0 }
        val rounded = kotlin.math.round(this * factor) / factor
        val parts = rounded.toString().split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else ""
        return if (decimals > 0) {
            "$intPart.${decPart.padEnd(decimals, '0').take(decimals)}"
        } else {
            intPart
        }
    }
}
