package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.data.api.SchoolApiService
import com.suseoaa.projectoaa.data.model.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * 学业情况仓库 - 处理学业情况查询相关功能
 */
class AcademicStatusRepository(
    private val api: SchoolApiService,
    private val json: Json
) {

    /**
     * 预定义的课程类别列表
     * 这些是从教务系统学业情况页面中提取的固定类别
     */
    private val predefinedCategories = listOf(
        "素质教育通识必修" to "SZJYTSBX",
        "素质教育实践必修" to "SZJYSJBX",
        "学科基础必修" to "XKJCBX",
        "学科基础选修" to "XKJCXX",
        "专业基础必修" to "ZYJCBX",
        "专业核心必修" to "ZYHXBX",
        "专业选修" to "ZYXX",
        "素质实践选修" to "SZSJXX",
        "集中实践必修" to "JZSJBX",
        "素质教育通识选修" to "SZJYTSXX",
        "素质教育通识限选" to "SZJYTSXXX",
        "复合培养选修" to "FHPYXX"
    )

    /**
     * 获取学业情况页面，解析课程类别
     * @param studentId 学号
     */
    suspend fun getAcademicStatusCategories(studentId: String): Result<List<AcademicStatusCategory>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getAcademicStatusPage(studentId)
                if (response.status.value == 200) {
                    val bodyText = response.bodyAsText()
                    // 从HTML中解析课程类别
                    val categories = parseCategoriesFromHtml(bodyText)
                    Result.success(categories)
                } else {
                    Result.failure(Exception("获取学业情况失败: ${response.status.value}"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    /**
     * 从HTML中解析课程类别
     */
    private fun parseCategoriesFromHtml(html: String): List<AcademicStatusCategory> {
        val categories = mutableListOf<AcademicStatusCategory>()

        // 使用正则匹配 xfyqjd_id 和对应的类别名称
        val pattern = Regex("xfyqjd_id='([A-F0-9]+)'\\s+data-content='([^']+)'")
        val matches = pattern.findAll(html)

        val seenIds = mutableSetOf<String>()
        for (match in matches) {
            val categoryId = match.groupValues[1]
            val categoryName = match.groupValues[2]

            // 去重
            if (categoryId !in seenIds && categoryName.isNotEmpty()) {
                seenIds.add(categoryId)
                categories.add(
                    AcademicStatusCategory(
                        categoryId = categoryId,
                        categoryName = categoryName
                    )
                )
            }
        }

        // 按类别名称排序
        return categories.sortedBy { category ->
            when {
                category.categoryName.contains("通识必修") -> 0
                category.categoryName.contains("实践必修") -> 1
                category.categoryName.contains("基础必修") -> 2
                category.categoryName.contains("核心必修") -> 3
                category.categoryName.contains("专业选修") -> 4
                category.categoryName.contains("选修") -> 5
                category.categoryName.contains("实践") -> 6
                else -> 7
            }
        }
    }

    /**
     * 获取指定类别下的课程列表
     * @param categoryId 类别ID
     * @param studentId 学号
     */
    suspend fun getCategoryCourses(
        categoryId: String,
        studentId: String
    ): Result<List<AcademicStatusCourseItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAcademicStatusCourses(categoryId, studentId)
            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val courses = try {
                    json.decodeFromString<List<AcademicStatusCourseItem>>(bodyText)
                } catch (e: Exception) {
                    // 可能返回空数组或错误格式
                    emptyList()
                }
                Result.success(courses)
            } else {
                Result.failure(Exception("获取课程列表失败: ${response.status.value}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 获取非计划内课程
     */
    suspend fun getNonPlanCourses(
        categoryId: String,
        studentId: String
    ): Result<List<AcademicStatusCourseItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAcademicStatusNonPlanCourses(categoryId, studentId)
            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val courses = try {
                    json.decodeFromString<List<AcademicStatusCourseItem>>(bodyText)
                } catch (e: Exception) {
                    emptyList()
                }
                Result.success(courses)
            } else {
                Result.failure(Exception("获取非计划课程失败: ${response.status.value}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 计算类别统计信息
     */
    fun calculateCategoryStats(courses: List<AcademicStatusCourseItem>): AcademicStatusCategory {
        var totalCredits = 0.0
        var earnedCredits = 0.0
        var passedCount = 0
        var failedCount = 0
        var studyingCount = 0
        var notStudiedCount = 0

        for (course in courses) {
            val credits = course.credits.toDoubleOrNull() ?: 0.0
            totalCredits += credits

            when (course.studyStatus) {
                StudyStatusUtils.PASSED -> {
                    passedCount++
                    earnedCredits += credits
                }

                StudyStatusUtils.FAILED -> {
                    failedCount++
                }

                StudyStatusUtils.STUDYING -> {
                    studyingCount++
                }

                StudyStatusUtils.NOT_STUDIED -> {
                    notStudiedCount++
                }
            }
        }

        return AcademicStatusCategory(
            categoryId = "",
            categoryName = "",
            courses = courses,
            totalCredits = totalCredits,
            earnedCredits = earnedCredits,
            passedCount = passedCount,
            failedCount = failedCount,
            studyingCount = studyingCount,
            notStudiedCount = notStudiedCount,
            isLoaded = true
        )
    }
}
