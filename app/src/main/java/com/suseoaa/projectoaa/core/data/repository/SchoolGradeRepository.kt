package com.suseoaa.projectoaa.core.data.repository

import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.dao.GradeDao
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.GradeEntity
import com.suseoaa.projectoaa.core.network.model.academic.studentGrade.StudentGradeResponse
import com.suseoaa.projectoaa.core.network.school.SchoolApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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

    private suspend fun fetchAndSaveSingleTerm(
        account: CourseAccountEntity,
        year: String,
        semester: String
    ) {
        val response = api.getStudentGrade(year = year, semester = semester)
        if (response.isSuccessful) {
            val bodyString = response.body()?.string() ?: ""
            if (isLoginRequired(bodyString)) throw SessionExpiredException()

            val gradeResponse = json.decodeFromString<StudentGradeResponse>(bodyString)

            // 每次拉取成绩时，尝试提取专业信息并更新到当前账户的数据库记录中
            gradeResponse.items.firstOrNull()?.let { firstItem ->
                val jgId = firstItem.jgId
                val zyhId = firstItem.zyhId
                val njdmId = firstItem.njdmId

                // 只要信息完整，就更新到数据库的用户表中
                if (!jgId.isNullOrEmpty() && !zyhId.isNullOrEmpty() && !njdmId.isNullOrEmpty()) {
                    courseDao.updateStudentMajorInfo(
                        studentId = account.studentId,
                        jgId = jgId,
                        zyhId = zyhId,
                        njdmId = njdmId
                    )
                    // 为了兼容旧代码，顺便存一份到 TokenManager，但主要依赖数据库
                    tokenManager.saveUserInfo(jgId, zyhId, njdmId)
                }
            }

            val entities = gradeResponse.items.map { item ->
                GradeEntity(
                    studentId = account.studentId,
                    xnm = item.xnm ?: year,
                    xqm = item.xqm ?: semester,
                    courseId = item.kchId ?: item.kch ?: "unknown_${item.hashCode()}",
                    courseName = item.kcmc ?: "未知课程",
                    score = item.cj ?: "-",
                    credit = item.xf ?: "0",
                    gpa = item.jd ?: "0",
                    courseType = item.kcxzmc ?: "",
                    examType = item.khfsmc ?: "",
                    teacher = item.jsxm ?: item.cjbdczr ?: "",
                    examNature = item.ksxz ?: ""
                )
            }
            gradeDao.updateGrades(account.studentId, year, semester, entities)
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