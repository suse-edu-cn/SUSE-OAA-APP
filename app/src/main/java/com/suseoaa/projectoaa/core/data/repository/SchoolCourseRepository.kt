package com.suseoaa.projectoaa.core.data.repository


import com.suseoaa.projectoaa.core.network.model.course.CourseResponseJson
import com.suseoaa.projectoaa.core.network.school.SchoolApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolCourseRepository @Inject constructor(
    private val api: SchoolApiService,
    private val json: Json,
    // 注入 AuthRepo 用于自动重试
    private val authRepository: SchoolAuthRepository
) {

    /**
     * 获取原始课表数据 (带自动重试)
     * 自动处理 Session 过期的情况
     */
    suspend fun getCourseSchedule(year: String, semester: String): Result<CourseResponseJson> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.querySchedule(year = year, semester = semester)

                if (response.isSuccessful) {
                    val jsonString = response.body()?.string() ?: ""

                    // 检查是否是被重定向到了登录页
                    if (isLoginRequired(jsonString)) {
                        return@withContext Result.failure(SessionExpiredException())
                    }

                    try {
                        val data = json.decodeFromString<CourseResponseJson>(jsonString)
                        Result.success(data)
                    } catch (e: Exception) {
                        Result.failure(Exception("JSON 解析失败: ${e.message}"))
                    }
                } else {
                    if (response.code() == 302 || response.code() == 901) {
                        Result.failure(SessionExpiredException())
                    } else {
                        Result.failure(Exception("请求失败: ${response.code()}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 获取校历（解析起始周）
     */
    suspend fun fetchSemesterStart(): String? = withContext(Dispatchers.IO) {
        try {
            val response = api.getCalendar()
            if (!response.isSuccessful) return@withContext null

            val html = response.body()?.string() ?: ""
            if (isLoginRequired(html)) return@withContext null

            // 正则提取日期：例如 "2025-02-17 至"
            val regex = Regex("""(\d{4}-\d{2}-\d{2})\s*至""")
            regex.find(html)?.groupValues?.get(1)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 辅助方法

    private fun isLoginRequired(content: String): Boolean {
        return content.contains("用户登录") || content.contains("/xtgl/login_slogin.html")
    }

    class SessionExpiredException : Exception("Session Expired")
}