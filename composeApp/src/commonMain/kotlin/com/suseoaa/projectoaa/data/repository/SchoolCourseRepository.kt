package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.data.api.SchoolApiService
import com.suseoaa.projectoaa.data.model.CourseResponseJson
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json

class SchoolCourseRepository(
    private val api: SchoolApiService,
    private val json: Json
) {
    suspend fun getCourseSchedule(year: String, semester: String): Result<CourseResponseJson> {
        return try {
            val response = api.querySchedule(year = year, semester = semester)

            if (response.status.value == 200) {
                val jsonString = response.bodyAsText()

                if (isLoginRequired(jsonString)) {
                    return Result.failure(SessionExpiredException())
                }

                try {
                    val data = json.decodeFromString<CourseResponseJson>(jsonString)
                    Result.success(data)
                } catch (e: Exception) {
                    Result.failure(Exception("JSON 解析失败: ${e.message}"))
                }
            } else {
                if (response.status.value == 302 || response.status.value == 901) {
                    Result.failure(SessionExpiredException())
                } else {
                    Result.failure(Exception("请求失败: ${response.status.value}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSemesterStart(): String? {
        return try {
            val response = api.getCalendar()
            if (response.status.value != 200) return null

            val html = response.bodyAsText()
            if (isLoginRequired(html)) return null

            val regex = Regex("""(\d{4}-\d{2}-\d{2})\s*至""")
            regex.find(html)?.groupValues?.get(1)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isLoginRequired(content: String): Boolean {
        return content.contains("用户登录") || content.contains("/xtgl/login_slogin.html")
    }

    class SessionExpiredException : Exception("Session Expired")
}
