package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.api.SchoolApiService
import com.suseoaa.projectoaa.data.model.MajorItem
import com.suseoaa.projectoaa.data.model.ProfessionInfoResponse
import com.suseoaa.projectoaa.data.model.TeachingPlanResponse
import com.suseoaa.projectoaa.database.CourseDatabase
import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.pow
import kotlin.math.round

/**
 * GPA 仓库 - 处理成绩数据和培养计划
 */
class GpaRepository(
    private val api: SchoolApiService,
    private val gradeRepository: SchoolGradeRepository,
    private val localCourseRepository: LocalCourseRepository,
    private val authRepository: SchoolAuthRepository,
    private val tokenManager: TokenManager,
    private val json: Json,
    private val database: CourseDatabase
) {
    // 内存缓存：课程号/课程名 -> 是否学位课
    private val degreeCourseCache = mutableMapOf<String, Boolean>()

    /**
     * 获取 GPA 数据（成绩 + 学位课标记）
     */
    suspend fun getGpaData(studentId: String): Result<List<GpaCourseWrapper>> =
        withContext(Dispatchers.IO) {
            try {
                // 1. 获取本地所有成绩
                val allGrades = gradeRepository.observeAllGrades(studentId).first()

                if (allGrades.isEmpty()) {
                    return@withContext Result.failure(Exception("暂无成绩数据，请先在成绩查询页面同步成绩"))
                }

                // 2. 按课程名去重，保留最高分
                val uniqueGrades = allGrades
                    .groupBy { it.courseName }
                    .mapValues { entry ->
                        entry.value.maxByOrNull { parseScore(it.score) } ?: entry.value.first()
                    }
                    .values
                    .toList()
                    .sortedWith(compareBy({ it.xnm }, { it.xqm }))

                // 3. 加载学位课信息（优先从数据库，然后从网络）
                if (degreeCourseCache.isEmpty()) {
                    loadDegreeCourseCache(studentId)
                }

                // 4. 合并数据
                val result = uniqueGrades.map { entity ->
                    val scoreStr = entity.score.trim()
                    // "缓考" 完全排除（未完成考试）
                    val isExcluded = scoreStr.contains("缓考")
                    // "合格", "通过", "免修" 等成绩标记为仅通过类（用于显示），但仍然参与绩点计算
                    val isPassOnly = listOf("合格", "通过", "免修").any { scoreStr.contains(it) }

                    // 优先用课程号匹配，其次用课程名
                    val isDegree = if (isExcluded) false else {
                        degreeCourseCache[entity.courseId]
                            ?: degreeCourseCache[entity.courseName]
                            ?: false
                    }

                    val scoreVal = if (isPassOnly) 60.0 else parseScore(entity.score)  // 合格/通过默认60分

                    GpaCourseWrapper(
                        originalEntity = entity,
                        isDegreeCourse = isDegree,
                        simulatedScore = if (isExcluded) null else scoreVal,
                        isExcluded = isExcluded,
                        isPassOnly = isPassOnly,
                        originalScoreText = scoreStr
                    )
                }.filter { !it.isExcluded }

                Result.success(result)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    /**
     * 加载学位课程缓存
     * 优先从数据库加载，如果数据库为空则从网络获取并保存
     */
    private suspend fun loadDegreeCourseCache(studentId: String) {
        // 1. 先从数据库加载
        val dbData = database.degreeCourseQueries.selectByStudent(studentId).executeAsList()
        if (dbData.isNotEmpty()) {
            dbData.forEach { item ->
                degreeCourseCache[item.courseKey] = item.isDegree == 1L
            }
            return
        }

        // 2. 数据库为空，从网络获取
        try {
            fetchAndSaveTeachingPlan(studentId)
        } catch (e: Exception) {
            // 获取培养计划失败不影响基本功能
            e.printStackTrace()
        }
    }

    /**
     * 获取并保存培养计划到数据库
     */
    private suspend fun fetchAndSaveTeachingPlan(studentId: String) {
        // 获取用户信息
        val userInfo = tokenManager.userInfoFlow.first()
        val jgId = userInfo["jg_id"]
        val njdmId = userInfo["njdm_id"]
        var zyhId = userInfo["zyh_id"]

        if (jgId.isNullOrEmpty() || njdmId.isNullOrEmpty()) {
            throw Exception("缺少学院或年级信息，请先同步成绩")
        }

        // 步骤 0: 如果专业ID缺失，主动查询专业列表
        if (zyhId.isNullOrEmpty()) {
            val majorsRes = api.getMajorList(jgId = jgId)
            if (majorsRes.status.value == 200) {
                val body = majorsRes.bodyAsText()
                val majors = json.decodeFromString<List<MajorItem>>(body)
                zyhId = majors.firstOrNull()?.majorId
            }
        }

        if (zyhId.isNullOrEmpty()) {
            throw Exception("无法获取专业ID")
        }

        // 步骤 1: 获取培养计划 ID
        val infoRes = api.getProfessionInfo(jgId, njdmId, zyhId)
        if (infoRes.status.value != 200) {
            throw Exception("获取培养计划信息失败")
        }

        val infoBody = infoRes.bodyAsText()
        val professionInfo = json.decodeFromString<ProfessionInfoResponse>(infoBody)
        val planId = professionInfo.items?.firstOrNull()?.planId
            ?: throw Exception("未找到该专业的培养计划")

        // 步骤 2: 获取课程列表
        val planRes = api.getTeachingPlan(planId)
        if (planRes.status.value != 200) {
            throw Exception("获取培养计划课程列表失败")
        }

        val planBody = planRes.bodyAsText()
        val teachingPlan = json.decodeFromString<TeachingPlanResponse>(planBody)
        val items = teachingPlan.items ?: emptyList()

        if (items.isEmpty()) {
            throw Exception("服务端返回的课程列表为空")
        }

        // 步骤 3: 保存到数据库并更新内存缓存
        database.transaction {
            // 先清除旧数据
            database.degreeCourseQueries.deleteByStudent(studentId)

            items.forEach { item ->
                val isDegree = item.degreeCourseFlag == "是"
                val isDegreeInt = if (isDegree) 1L else 0L

                // 保存课程号
                if (!item.courseNumber.isNullOrEmpty()) {
                    database.degreeCourseQueries.insertOrReplace(
                        studentId = studentId,
                        courseKey = item.courseNumber,
                        isDegree = isDegreeInt
                    )
                    degreeCourseCache[item.courseNumber] = isDegree
                }

                // 保存课程名
                if (!item.courseName.isNullOrEmpty()) {
                    database.degreeCourseQueries.insertOrReplace(
                        studentId = studentId,
                        courseKey = item.courseName,
                        isDegree = isDegreeInt
                    )
                    degreeCourseCache[item.courseName] = isDegree
                }
            }
        }
    }

    /**
     * 解析成绩为分数
     */
    private fun parseScore(score: String): Double {
        val trimmed = score.trim()
        return trimmed.toDoubleOrNull() ?: when {
            trimmed.contains("优") || trimmed.contains("A") -> 95.0
            trimmed.contains("良") || trimmed.contains("B") -> 85.0
            trimmed.contains("中") || trimmed.contains("C") -> 75.0
            trimmed.contains("及格") || trimmed.contains("D") -> 65.0
            trimmed.contains("不及格") || trimmed.contains("F") -> 0.0
            else -> 0.0
        }
    }

    /**
     * 清除缓存（用于切换账号时）
     */
    fun clearCache() {
        degreeCourseCache.clear()
    }
}

/**
 * 绩点课程包装类
 */
data class GpaCourseWrapper(
    val originalEntity: GradeEntity,
    val isDegreeCourse: Boolean,
    val simulatedScore: Double?,
    val simulatedGpa: Double? = null,  // 模拟绩点（用户修改后的值）
    val isExcluded: Boolean = false,
    val isPassOnly: Boolean = false,   // 仅通过类成绩（合格/通过/免修），用于显示标记
    val originalScoreText: String = ""  // 原始成绩文本（用于显示优/良/中/差）
) {
    val credit: Double
        get() = originalEntity.credit.toDoubleOrNull() ?: 0.0

    val scoreValue: Double
        get() = simulatedScore ?: 0.0

    /**
     * 是否是等级制成绩（优/良/中/差等，不包括合格/通过/免修）
     */
    val isGradeLevel: Boolean
        get() = originalScoreText.isNotEmpty() &&
                originalScoreText.toDoubleOrNull() == null &&
                !isPassOnly &&
                simulatedGpa == null

    /**
     * 获取绩点值
     * 所有成绩都参与绩点计算
     * 优先级：模拟绩点 > 数据库绩点 > 计算绩点
     */
    val gpaValue: Double
        get() = simulatedGpa
            ?: originalEntity.gpa.toDoubleOrNull()
            ?: calculateGpa(scoreValue)

    /**
     * 显示成绩：如果是等级制成绩或仅通过类成绩且未被修改，显示原始等级文本
     */
    val displayScore: String
        get() = if (isGradeLevel || (isPassOnly && simulatedGpa == null)) originalScoreText else scoreValue.format(
            1
        )

    val displayGpa: String
        get() = gpaValue.format(2)

    /**
     * 计算绩点（当数据库没有存储绩点时使用）
     * 与 Android 版本保持一致：
     * - 95分及以上: 4.5
     * - 60分以下: 0.0
     * - 其他: 1.0 + ((score - 60) / 5).toInt() * 0.5
     */
    private fun calculateGpa(score: Double): Double {
        return when {
            score >= 95.0 -> 4.5
            score < 60.0 -> 0.0
            else -> {
                val base = 1.0
                val steps = ((score - 60) / 5).toInt()
                base + steps * 0.5
            }
        }
    }
}

// KMP 兼容的格式化函数（四舍五入）
private fun Double.format(decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(this * factor) / factor
    val str = rounded.toString()
    val parts = str.split(".")
    return if (parts.size == 1) {
        "$str.${"0".repeat(decimals)}"
    } else {
        val intPart = parts[0]
        val decimalPart = parts[1]
        if (decimalPart.length >= decimals) {
            "$intPart.${decimalPart.take(decimals)}"
        } else {
            "$intPart.$decimalPart${"0".repeat(decimals - decimalPart.length)}"
        }
    }
}
