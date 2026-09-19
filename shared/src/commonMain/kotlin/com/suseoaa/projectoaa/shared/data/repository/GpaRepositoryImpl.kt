package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.SchoolApiService
import com.suseoaa.projectoaa.shared.domain.model.gpa.MajorItem
import com.suseoaa.projectoaa.shared.domain.model.gpa.ProfessionInfoResponse
import com.suseoaa.projectoaa.shared.domain.model.gpa.TeachingPlanResponse
import com.suseoaa.projectoaa.shared.database.CourseDatabase
import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.pow
import kotlin.math.round
import com.suseoaa.projectoaa.shared.util.getCurrentTerm
import com.suseoaa.projectoaa.shared.domain.model.gpa.GpaCourseWrapper
import com.suseoaa.projectoaa.shared.domain.model.grade.GradeEntity
import com.suseoaa.projectoaa.shared.data.local.store.UserProfileStore
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import com.suseoaa.projectoaa.shared.domain.repository.GpaRepository
import com.suseoaa.projectoaa.shared.domain.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolGradeRepository

/**
 * GPA 仓库 - 处理成绩数据和培养计划
 */
class GpaRepositoryImpl(
    private val api: SchoolApiService,
    private val gradeRepository: SchoolGradeRepository,
    private val localCourseRepository: LocalCourseRepository,
    private val authRepository: SchoolAuthRepository,
    private val userProfileStore: UserProfileStore,
    private val json: Json,
    private val database: CourseDatabase
) : GpaRepository {
    // 内存缓存：课程号/课程名 -> 是否学位课
    private val degreeCourseCache = mutableMapOf<String, Boolean>()

    /**
     * 获取 GPA 数据（成绩 + 学位课标记）
     */
    override suspend fun getGpaData(studentId: String): Result<List<GpaCourseWrapper>> =
        withContext(Dispatchers.IO) {
            try {
                // 1. 获取本地所有成绩
                val allGrades = gradeRepository.observeAllGrades(studentId).first()

                // 2. 获取本地排课课程（用于提取本学期及以后学期未出分课程进行提前模拟）
                val allScheduleCourses = localCourseRepository.getAllCoursesByStudent(studentId).first()

                val (currentXnm, currentXqm) = getCurrentTerm()
                val currentTermValue = currentXnm.toIntOrNull()?.times(100)?.plus(currentXqm.toIntOrNull() ?: 0) ?: 0

                val simulatedGrades = allScheduleCourses.filter { courseWithTimes ->
                    val c = courseWithTimes.course
                    val termValue = c.xnm.toIntOrNull()?.times(100)?.plus(c.xqm.toIntOrNull() ?: 0) ?: 0
                    // 只包含当前或未来学期，且不是自定义课程的
                    termValue >= currentTermValue && !c.isCustom
                }.map { courseWithTimes ->
                    val c = courseWithTimes.course
                    GradeEntity(
                        studentId = studentId,
                        xnm = c.xnm,
                        xqm = c.xqm,
                        courseId = c.remoteCourseId.ifEmpty { c.courseName },
                        jxbId = "",
                        courseName = c.courseName,
                        score = "", // 空成绩
                        credit = c.credit.ifEmpty { "0" },
                        gpa = "0",
                        courseType = c.nature,
                        examType = c.assessment,
                        teacher = courseWithTimes.times.firstOrNull()?.teacher ?: "",
                        examNature = ""
                    )
                }

                // 过滤掉已有真实成绩的课，防止覆盖
                val existingCourseNames = allGrades.map { it.courseName }.toSet()
                val newSimulatedGrades = simulatedGrades.filter { it.courseName !in existingCourseNames }

                val combinedGrades = allGrades + newSimulatedGrades

                if (combinedGrades.isEmpty()) {
                    return@withContext appFailure(AppError.Business(userMessage = "暂无数据，请先同步成绩或课表"))
                }

                // 3. 按课程名去重，保留最高分
                val uniqueGrades = combinedGrades
                    .groupBy { it.courseName }
                    .mapValues { entry ->
                        entry.value.maxByOrNull { parseScore(it.score) } ?: entry.value.first()
                    }
                    .values
                    .toList()
                    .sortedWith(compareBy({ it.xnm }, { it.xqm }))

                // 4. 加载学位课信息（优先从数据库，然后从网络）
                if (degreeCourseCache.isEmpty()) {
                    loadDegreeCourseCache(studentId)
                }

                // 5. 合并数据
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

                    // 对于空成绩（提前模拟），scoreVal 设为空，用户需要手动修改
                    val scoreVal = if (scoreStr.isEmpty()) null else if (isPassOnly) 60.0 else parseScore(entity.score)

                    GpaCourseWrapper(
                        originalEntity = entity,
                        isDegreeCourse = isDegree,
                        simulatedScore = if (isExcluded) null else scoreVal,
                        isExcluded = isExcluded,
                        isPassOnly = isPassOnly,
                        originalScoreText = scoreStr,
                        isIncludedInCalculation = true
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
        val userInfo = userProfileStore.userInfoFlow.first()
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
    override fun clearCache() {
        degreeCourseCache.clear()
    }
}

