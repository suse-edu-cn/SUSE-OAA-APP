package com.suseoaa.projectoaa.core.data.repository

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
    private val json: Json,
    // 注入 AuthRepo 用于自动重试
    private val authRepository: SchoolAuthRepository
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
                        // 使用通用重试逻辑
                        executeWithAutoRetry(account) {
                            fetchAndSaveSingleTerm(account, year.toString(), semester)
                        }.onSuccess { successCount++ }
                        delay(300) // 避免频繁请求
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

    // 通用辅助方法
    private fun isLoginRequired(html: String): Boolean =
        html.contains("用户登录") || html.contains("/xtgl/login_slogin.html")

    private suspend fun <T> executeWithAutoRetry(
        account: CourseAccountEntity,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: SessionExpiredException) {
            // Session 过期，尝试自动重新登录
            val loginResult = authRepository.login(account.studentId, account.password)
            if (loginResult.isSuccess) {
                try {
                    // 重试
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