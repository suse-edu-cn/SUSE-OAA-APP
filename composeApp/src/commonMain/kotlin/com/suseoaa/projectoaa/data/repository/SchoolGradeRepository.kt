package com.suseoaa.projectoaa.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.api.SchoolApiService
import com.suseoaa.projectoaa.data.model.CourseAccountEntity
import com.suseoaa.projectoaa.database.CourseDatabase
import com.suseoaa.projectoaa.database.Grade
import com.suseoaa.projectoaa.presentation.grades.GradeItem
import com.suseoaa.projectoaa.presentation.grades.StudentGradeResponse
import com.suseoaa.projectoaa.util.HtmlParser
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

/**
 * 成绩实体类 (用于UI层)
 */
data class GradeEntity(
    val studentId: String,
    val xnm: String,
    val xqm: String,
    val courseId: String,
    val jxbId: String = "",
    val regularScore: String = "",
    val regularRatio: String = "",
    val experimentScore: String = "",
    val experimentRatio: String = "",
    val finalScore: String = "",
    val finalRatio: String = "",
    val courseName: String,
    val score: String,
    val credit: String,
    val gpa: String,
    val courseType: String,
    val examType: String,
    val teacher: String,
    val examNature: String
)

/**
 * 成绩仓库 - 处理成绩数据的获取、存储和同步
 */
class SchoolGradeRepository(
    private val api: SchoolApiService,
    private val database: CourseDatabase,
    private val json: Json,
    private val authRepository: SchoolAuthRepository,
    private val localCourseRepository: LocalCourseRepository,
    private val tokenManager: TokenManager
) {

    /**
     * 观察指定学期的成绩
     */
    fun observeGrades(studentId: String, xnm: String, xqm: String): Flow<List<GradeEntity>> {
        return database.gradeQueries.selectByTerm(studentId, xnm, xqm)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toEntity() } }
    }

    /**
     * 观察学生所有成绩（用于GPA计算）
     */
    fun observeAllGrades(studentId: String): Flow<List<GradeEntity>> {
        return database.gradeQueries.selectAllByStudent(studentId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toEntity() } }
    }

    /**
     * 获取所有历史成绩（全量同步）
     */
    suspend fun fetchAllHistoryGrades(account: CourseAccountEntity): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val currentYear = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).year
                val startYear = account.njdmId.toIntOrNull() ?: (currentYear - 4)
                val endYear = currentYear + 1
                var successCount = 0

                for (year in startYear..endYear) {
                    listOf("3", "12").forEach { semester ->
                        executeWithAutoRetry(account) {
                            fetchAndSaveSingleTerm(account, year.toString(), semester)
                        }.onSuccess { successCount++ }
                        delay(300)
                    }
                }
                Result.success("同步完成，更新了 $successCount 个学期数据")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 获取并保存单学期成绩
     */
    private suspend fun fetchAndSaveSingleTerm(
        account: CourseAccountEntity,
        year: String,
        semester: String
    ) = coroutineScope {
        val response = api.getStudentGrade(year = year, semester = semester)

        if (response.status.value == 200) {
            val bodyString = response.bodyAsText()
            if (isLoginRequired(bodyString)) throw SessionExpiredException()

            val gradeResponse = json.decodeFromString<StudentGradeResponse>(bodyString)

            // 1. 更新专业信息
            gradeResponse.items?.firstOrNull()?.let { firstItem ->
                val jgId = firstItem.jgId
                val zyhId = firstItem.zyhId
                val njdmId = firstItem.njdmId
                if (!jgId.isNullOrEmpty() && !zyhId.isNullOrEmpty() && !njdmId.isNullOrEmpty()) {
                    localCourseRepository.updateMajorInfo(
                        studentId = account.studentId,
                        jgId = jgId,
                        zyhId = zyhId,
                        njdmId = njdmId
                    )
                    tokenManager.saveUserInfo(jgId, zyhId, njdmId)
                }
            }

            // 2. 初步构建 Entity 列表 (此时还没有详情)
            val tempEntities = gradeResponse.items?.map { item ->
                GradeEntity(
                    studentId = account.studentId,
                    xnm = item.xnm ?: year,
                    xqm = item.xqm ?: semester,
                    courseId = item.kchId ?: item.kch ?: "unknown_${item.hashCode()}",
                    jxbId = item.jxbId ?: "",
                    courseName = item.kcmc ?: "未知课程",
                    score = item.cj ?: "-",
                    credit = item.xf ?: "0",
                    gpa = item.jd ?: "0",
                    courseType = item.kcxzmc ?: "",
                    examType = item.khfsmc ?: "",
                    teacher = item.jsxm ?: item.cjbdczr ?: "",
                    examNature = item.ksxz ?: "",
                    regularScore = "",
                    finalScore = ""
                )
            } ?: emptyList()

            // 3. 并发获取每一科的详情
            val requestSemaphore = Semaphore(3)

            val finalEntities = tempEntities.map { entity ->
                async {
                    if (entity.jxbId.isBlank()) return@async entity

                    requestSemaphore.withPermit {
                        try {
                            val detailResp = api.getGradeDetail(
                                xnm = entity.xnm,
                                xqm = entity.xqm,
                                kcmc = entity.courseName,
                                jxbId = entity.jxbId
                            )
                            if (detailResp.status.value == 200) {
                                val html = detailResp.bodyAsText()
                                val detail = HtmlParser.parseGradeDetail(html)

                                // 同时保存成绩和比例
                                return@withPermit entity.copy(
                                    regularScore = detail.regular,
                                    regularRatio = detail.regularRatio,
                                    // 保存实验成绩和比例
                                    experimentScore = detail.experiment,
                                    experimentRatio = detail.experimentRatio,
                                    finalScore = detail.final,
                                    finalRatio = detail.finalRatio
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        entity
                    }
                }
            }.awaitAll()

            // 4. 一次性保存到数据库
            updateGrades(account.studentId, year, semester, finalEntities)
        } else {
            if (response.status.value == 901) throw SessionExpiredException()
            throw Exception("HTTP Error: ${response.status.value}")
        }
    }

    /**
     * 更新成绩（先删后存）
     */
    private suspend fun updateGrades(
        studentId: String,
        xnm: String,
        xqm: String,
        grades: List<GradeEntity>
    ) = withContext(Dispatchers.IO) {
        database.transaction {
            database.gradeQueries.deleteByTerm(studentId, xnm, xqm)
            grades.forEach { grade ->
                database.gradeQueries.insertOrReplace(
                    studentId = grade.studentId,
                    xnm = grade.xnm,
                    xqm = grade.xqm,
                    courseId = grade.courseId,
                    jxbId = grade.jxbId,
                    regularScore = grade.regularScore,
                    regularRatio = grade.regularRatio,
                    experimentScore = grade.experimentScore,
                    experimentRatio = grade.experimentRatio,
                    finalScore = grade.finalScore,
                    finalRatio = grade.finalRatio,
                    courseName = grade.courseName,
                    score = grade.score,
                    credit = grade.credit,
                    gpa = grade.gpa,
                    courseType = grade.courseType,
                    examType = grade.examType,
                    teacher = grade.teacher,
                    examNature = grade.examNature
                )
            }
        }
    }

    private fun isLoginRequired(html: String): Boolean =
        html.contains("用户登录") || html.contains("/xtgl/login_slogin.html")

    private suspend fun <T> executeWithAutoRetry(
        account: CourseAccountEntity,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: SessionExpiredException) {
            // 先使当前 session 失效，再重新登录
            authRepository.invalidateSession()
            val loginResult = authRepository.login(account.studentId, account.password)
            if (loginResult.isSuccess) {
                try {
                    Result.success(block())
                } catch (e2: Exception) {
                    Result.failure(e2)
                }
            } else {
                Result.failure(Exception("自动登录失败: ${loginResult.exceptionOrNull()?.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    class SessionExpiredException : Exception("Session Expired")
}

/**
 * 扩展函数：将 SQLDelight 生成的 Grade 转换为 GradeEntity
 */
private fun Grade.toEntity() = GradeEntity(
    studentId = studentId,
    xnm = xnm,
    xqm = xqm,
    courseId = courseId,
    jxbId = jxbId,
    regularScore = regularScore,
    regularRatio = regularRatio,
    experimentScore = experimentScore,
    experimentRatio = experimentRatio,
    finalScore = finalScore,
    finalRatio = finalRatio,
    courseName = courseName,
    score = score,
    credit = credit,
    gpa = gpa,
    courseType = courseType,
    examType = examType,
    teacher = teacher,
    examNature = examNature
)
