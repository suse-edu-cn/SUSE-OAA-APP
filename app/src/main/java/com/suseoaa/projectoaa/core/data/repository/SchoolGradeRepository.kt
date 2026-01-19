package com.suseoaa.projectoaa.core.data.repository

import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.dao.GradeDao
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.GradeEntity
import com.suseoaa.projectoaa.core.network.model.academic.studentGrade.StudentGradeResponse
import com.suseoaa.projectoaa.core.network.school.SchoolApiService
import com.suseoaa.projectoaa.core.util.HtmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolGradeRepository @Inject constructor(
    private val api: SchoolApiService,
    private val gradeDao: GradeDao,
    private val courseDao: CourseDao,
    private val json: Json,
    private val authRepository: SchoolAuthRepository,
    private val tokenManager: TokenManager
) {

    fun observeGrades(studentId: String, xnm: String, xqm: String): Flow<List<GradeEntity>> {
        return gradeDao.getGradesFlow(studentId, xnm, xqm)
    }

    suspend fun fetchAllHistoryGrades(account: CourseAccountEntity): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
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

    // 增加并发获取详情逻辑
    private suspend fun fetchAndSaveSingleTerm(
        account: CourseAccountEntity,
        year: String,
        semester: String
    ) = coroutineScope {
        val response = api.getStudentGrade(year = year, semester = semester)
        if (response.isSuccessful) {
            val bodyString = response.body()?.string() ?: ""
            if (isLoginRequired(bodyString)) throw SessionExpiredException()

            val gradeResponse = json.decodeFromString<StudentGradeResponse>(bodyString)

            // 1. 更新专业信息
            gradeResponse.items.firstOrNull()?.let { firstItem ->
                val jgId = firstItem.jgId
                val zyhId = firstItem.zyhId
                val njdmId = firstItem.njdmId
                if (!jgId.isNullOrEmpty() && !zyhId.isNullOrEmpty() && !njdmId.isNullOrEmpty()) {
                    courseDao.updateStudentMajorInfo(
                        studentId = account.studentId,
                        jgId = jgId,
                        zyhId = zyhId,
                        njdmId = njdmId
                    )
                    tokenManager.saveUserInfo(jgId, zyhId, njdmId)
                }
            }

            // 2. 初步构建 Entity 列表 (此时还没有详情)
            val tempEntities = gradeResponse.items.map { item ->
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
            }

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
                            if (detailResp.isSuccessful) {
                                val html = detailResp.body()?.string() ?: ""
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
            gradeDao.updateGrades(account.studentId, year, semester, finalEntities)
        } else {
            if (response.code() == 901) throw SessionExpiredException()
            throw Exception("HTTP Error: ${response.code()}")
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