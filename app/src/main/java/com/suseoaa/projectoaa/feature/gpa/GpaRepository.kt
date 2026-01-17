package com.suseoaa.projectoaa.feature.gpa

import com.suseoaa.projectoaa.core.database.dao.GradeDao
import com.suseoaa.projectoaa.core.database.entity.GradeEntity
import com.suseoaa.projectoaa.core.network.school.SchoolApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class GpaCourseWrapper(
    val originalEntity: GradeEntity,
    val isDegreeCourse: Boolean, // 来自培养计划
    var simulatedScore: Double,  // 用于模拟计算
    var simulatedGpa: Double     // 用于模拟计算
)

class GpaRepository @Inject constructor(
    private val api: SchoolApiService,
    private val gradeDao: GradeDao
) {
    // 内存缓存：课程号 -> 是否学位课
    private val degreeCourseCache = mutableMapOf<String, Boolean>()

    suspend fun getGpaData(
        studentId: String,
        jgId: String,
        njdmId: String,
        zyhId: String? // 允许为空，内部自动补全
    ): Result<List<GpaCourseWrapper>> = withContext(Dispatchers.IO) {
        try {
            // 1. 获取本地所有学期的成绩
            val allGrades = gradeDao.getGradesFlow(studentId, "%", "%").first()

            // === 核心修复：成绩去重 ===
            // 逻辑：按课程名称分组，取分数最高的一次。
            // 这样如果是补考（两学期都有同一门课），会保留通过的那次（或者分高的那次），避免重复计算学分。
            val uniqueGrades = allGrades
                .groupBy { it.courseName } // 按照课程名分组
                .mapValues { entry ->
                    // 在每一组中，找到解析分数最高的那个记录
                    entry.value.maxByOrNull { entity ->
                        parseScore(entity.score)
                    }!!
                }
                .values
                .toList()
                // 可选：按学年学期重新排序，让列表整齐
                .sortedWith(compareBy({ it.xnm }, { it.xqm }))

            // 2. 如果缓存为空，尝试联网获取培养计划
            if (degreeCourseCache.isEmpty()) {
                fetchAndCachePlan(jgId, njdmId, zyhId)
            }

            // 3. 合并数据 (使用去重后的 uniqueGrades)
            val result = uniqueGrades.map { entity ->
                // 优先用课程号匹配，其次用课程名
                val isDegree = degreeCourseCache[entity.courseId]
                    ?: degreeCourseCache[entity.courseName]
                    ?: false

                val scoreVal = parseScore(entity.score)

                GpaCourseWrapper(
                    originalEntity = entity,
                    isDegreeCourse = isDegree,
                    simulatedScore = scoreVal,
                    simulatedGpa = 0.0 // 初始 GPA 在 ViewModel 中统一计算
                )
            }
            Result.success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun fetchAndCachePlan(jgId: String, njdmId: String, providedZyhId: String?) {
        var finalZyhId = providedZyhId

        // 步骤 0: 如果专业ID缺失，主动查询专业列表
        if (finalZyhId.isNullOrEmpty()) {
            val majorsRes = api.getMajorList(jgId = jgId)
            if (majorsRes.isSuccessful) {
                finalZyhId = majorsRes.body()?.firstOrNull()?.majorId
            }
        }

        if (finalZyhId.isNullOrEmpty()) {
            throw Exception("无法获取专业ID (zyh_id)，请尝试刷新成绩或重新登录")
        }

        // 步骤 1: 获取培养计划 ID
        val infoRes = api.getProfessionInfo(jgId, njdmId, finalZyhId)
        val planId = infoRes.body()?.items?.firstOrNull()?.planId
            ?: throw Exception("未找到该专业的培养计划 (Plan ID)")

        // 步骤 2: 获取课程列表
        val planRes = api.getTeachingPlan(planId)
        val items = planRes.body()?.items ?: emptyList()

        if (items.isEmpty()) {
            throw Exception("服务端返回的课程列表为空 (PlanID: $planId)")
        }

        // 步骤 3: 缓存学位课标记
        items.forEach { item ->
            val isDegree = item.degreeCourseFlag == "是"
            // 同时缓存课程号和课程名，提高匹配率
            if (!item.courseNumber.isNullOrEmpty()) {
                degreeCourseCache[item.courseNumber] = isDegree
            }
            if (!item.courseName.isNullOrEmpty()) {
                degreeCourseCache[item.courseName] = isDegree
            }
        }
    }

    // 辅助方法：解析分数为 Double，用于比较大小
    private fun parseScore(score: String): Double {
        return when (score) {
            "优" -> 95.0
            "良" -> 85.0
            "中" -> 75.0
            "及格" -> 65.0
            "不及格" -> 0.0
            else -> score.toDoubleOrNull() ?: 0.0
        }
    }
}