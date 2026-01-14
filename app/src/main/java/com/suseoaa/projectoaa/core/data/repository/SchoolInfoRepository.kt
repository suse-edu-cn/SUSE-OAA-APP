package com.suseoaa.projectoaa.core.data.repository

import com.suseoaa.projectoaa.core.database.dao.AcademicDao
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.ExamCacheEntity
import com.suseoaa.projectoaa.core.database.entity.MessageCacheEntity
import com.suseoaa.projectoaa.core.network.school.SchoolApiService
import com.suseoaa.projectoaa.core.util.HtmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolInfoRepository @Inject constructor(
    private val api: SchoolApiService,
    private val authRepository: SchoolAuthRepository,
    private val academicDao: AcademicDao
) {

    // 考试信息 (缓存+网络)

    fun observeExams(studentId: String): Flow<List<ExamCacheEntity>> {
        return academicDao.getExamsFlow(studentId)
    }

    suspend fun refreshAcademicExamInfo(account: CourseAccountEntity): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val (xnm, xqm) = getCurrentTerm()
                // 1. 发起请求
                val request = suspend { api.getExamList(xnm, xqm) }
                var response = request()

                // 2. 自动重试
                if (!response.isSuccessful || response.raw().header("Content-Type")?.contains("html") == true) {
                    authRepository.login(account.studentId, account.password)
                    response = request()
                }

                // 3. 处理响应并写入数据库
                if (response.isSuccessful) {
                    val examResponse = response.body()
                    val items = examResponse?.items ?: emptyList()

                    val entities = items.map { item ->
                        val name = item.kcmc ?: "未知课程"
                        val time = item.kssj ?: "时间待定"
                        var location = item.cdmc ?: "地点待定"
                        if (!item.cdxqmc.isNullOrBlank()) {
                            location += "(${item.cdxqmc})"
                        }
                        ExamCacheEntity(
                            studentId = account.studentId,
                            courseName = name,
                            time = time,
                            location = location
                        )
                    }
                    academicDao.updateExams(account.studentId, entities)
                    Result.success("刷新成功")
                } else {
                    Result.failure(Exception("获取考试信息失败: ${response.code()}"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    //调课通知 (缓存+网络)

    fun observeMessages(studentId: String): Flow<List<MessageCacheEntity>> {
        return academicDao.getMessagesFlow(studentId)
    }

    suspend fun refreshAcademicMessageInfo(account: CourseAccountEntity): Result<String> =
        fetchAndSaveMessages(account)

    private suspend fun fetchAndSaveMessages(account: CourseAccountEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = suspend { api.getAcademicMessageInfo() }
            var response = request()
            var bodyString = response.body()?.string() ?: ""

            if (response.code() == 901 || response.code() == 302 || isLoginRequired(bodyString)) {
                val loginResult = authRepository.login(account.studentId, account.password)
                if (loginResult.isFailure) return@withContext Result.failure(Exception("自动登录失败"))
                response = request()
                bodyString = response.body()?.string() ?: ""
            }

            if (response.isSuccessful) {
                val rawList = HtmlParser.htmlParse(bodyString)
                val entities = rawList.map { content ->
                    MessageCacheEntity(
                        studentId = account.studentId,
                        content = content
                    )
                }
                academicDao.updateMessages(account.studentId, entities)
                Result.success("刷新成功")
            } else {
                Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 这里的返回值仍保持 List<String>，以兼容 GetCourseInfoViewModel
    suspend fun getAcademicCourseInfo(account: CourseAccountEntity): Result<List<String>> =
        fetchAndParseHtml(account) { api.getAcademicCourseInfo() }

    private suspend fun fetchAndParseHtml(
        account: CourseAccountEntity,
        request: suspend () -> retrofit2.Response<okhttp3.ResponseBody>
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            var response = request()
            var bodyString = response.body()?.string() ?: ""

            if (response.code() == 901 || response.code() == 302 || isLoginRequired(bodyString)) {
                val loginResult = authRepository.login(account.studentId, account.password)
                if (loginResult.isFailure) return@withContext Result.failure(Exception("自动登录失败"))

                response = request()
                bodyString = response.body()?.string() ?: ""
            }

            if (response.isSuccessful) {
                val result = withContext(Dispatchers.Default) {
                    HtmlParser.htmlParse(bodyString)
                }
                Result.success(result)
            } else {
                Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // 辅助

    private fun isLoginRequired(html: String?): Boolean =
        html != null && (html.contains("用户登录") || html.contains("/xtgl/login_slogin.html"))

    private fun getCurrentTerm(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return if (month >= 8 || month == 1) {
            val xnm = if (month == 1) (year - 1).toString() else year.toString()
            xnm to "3"
        } else {
            (year - 1).toString() to "12"
        }
    }
}