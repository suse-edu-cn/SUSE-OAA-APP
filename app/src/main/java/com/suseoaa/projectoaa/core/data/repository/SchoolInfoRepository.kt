package com.suseoaa.projectoaa.core.data.repository

import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.network.school.SchoolApiService
import com.suseoaa.projectoaa.core.util.HtmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolInfoRepository @Inject constructor(
    private val api: SchoolApiService,
    private val authRepository: SchoolAuthRepository
) {
    // 考试信息 - 使用三段式拼接
    suspend fun getAcademicExamInfo(account: CourseAccountEntity): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val (xnm, xqm) = getCurrentTerm()

                val request = suspend { api.getExamList(xnm, xqm) }
                var response = request()

                // 自动重试逻辑
                if (!response.isSuccessful || response.raw().header("Content-Type")
                        ?.contains("html") == true
                ) {
                    authRepository.login(account.studentId, account.password)
                    response = request()
                }

                if (response.isSuccessful) {
                    val examResponse = response.body()
                    val items = examResponse?.items ?: emptyList()

                    val resultList = items.map { item ->
                        val name = item.kcmc ?: "未知课程"
                        val time = item.kssj ?: "时间待定"

                        // 拼接地点：教室 + (校区)
                        // e.g. "LA5-322(临港校区)"
                        var location = item.cdmc ?: "地点待定"
                        if (!item.cdxqmc.isNullOrBlank()) {
                            location += "(${item.cdxqmc})"
                        }

                        // 使用 "###" 分隔三个字段：课程名###时间###地点
                        // 这样 ViewModel 就不需要用正则去猜了，直接 split 即可
                        "$name###$time###$location"
                    }
                    Result.success(resultList)
                } else {
                    Result.failure(Exception("获取考试信息失败: ${response.code()}"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    suspend fun getAcademicMessageInfo(account: CourseAccountEntity): Result<List<String>> =
        fetchAndParseHtml(account) { api.getAcademicMessageInfo() }

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
                val finalHtml = bodyString
                val result = withContext(Dispatchers.Default) {
                    HtmlParser.htmlParse(finalHtml)
                }
                Result.success(result)
            } else {
                Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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