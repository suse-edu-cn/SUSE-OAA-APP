package com.suseoaa.projectoaa.core.data.repository

import android.util.Log
import com.suseoaa.projectoaa.core.database.dao.GradeDao
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.GradeEntity
import com.suseoaa.projectoaa.core.network.model.academic.studentGrade.StudentGradeResponse
import com.suseoaa.projectoaa.core.network.model.course.CourseResponseJson
import com.suseoaa.projectoaa.core.network.school.SchoolApiService
import com.suseoaa.projectoaa.core.network.school.SchoolCookieJar
import com.suseoaa.projectoaa.core.util.HtmlParser.htmlParse
import com.suseoaa.projectoaa.core.utils.RSAEncryptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolRepository @Inject constructor(
    private val api: SchoolApiService,
    private val cookieJar: SchoolCookieJar,
    private val json: Json,
    private val gradeDao: GradeDao
) {
    //    通用自动重试请求执行器
    private suspend fun <T> executeWithAutoRetry(
        account: CourseAccountEntity,
        block: suspend () -> Result<T>
    ): Result<T> {
        val firstResult = block()
        if (firstResult.isFailure) {
            val error = firstResult.exceptionOrNull()
            if (isSessionExpired(error)) {
                val loginResult = login(account.studentId, account.password)
                return if (loginResult.isSuccess) {
                    block()
                } else {
                    Result.failure(Exception("自动重登失败，请检查密码"))
                }
            }
        }
        return firstResult
    }

    private fun isSessionExpired(e: Throwable?): Boolean {
        return e is SessionExpiredException
    }

    class SessionExpiredException : Exception("Session Expired")

    suspend fun login(username: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                cookieJar.clear()
                val csrfHtml = api.getCSRFToken().string()
                val csrfToken = extractCSRFToken(csrfHtml)
                    ?: return@withContext Result.failure(Exception("无法获取 CSRF Token"))

                val rsaKey = api.getRSAKey()
                val encryptedPwd = RSAEncryptor.encrypt(password, rsaKey.modulus, rsaKey.exponent)

                val timestamp = System.currentTimeMillis().toString()
                val response = api.login(timestamp, username, encryptedPwd, csrfToken)

                if (response.code() == 302) {
                    val location = response.headers()["Location"]
                    if (location != null) {
                        val targetUrl =
                            if (location.startsWith("/")) "https://jwgl.suse.edu.cn$location" else location
                        try {
                            api.visitUrl(targetUrl)
                            delay(500)
                            Result.success("登录成功")
                        } catch (e: Exception) {
                            Result.success("登录成功 (重定向异常: ${e.message})")
                        }
                    } else {
                        Result.success("登录成功 (无跳转)")
                    }
                } else {
                    val body = response.errorBody()?.string() ?: response.body()?.string() ?: ""
                    val msg =
                        if (body.contains("用户名或密码不正确")) "用户名或密码错误" else "登录失败，状态码: ${response.code()}"
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getCourseSchedule(year: String, semester: String): Result<CourseResponseJson> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.querySchedule(year = year, semester = semester)
                if (response.isSuccessful) {
                    val jsonString = response.body()?.string() ?: ""
                    try {
                        val data = json.decodeFromString<CourseResponseJson>(jsonString)
                        Result.success(data)
                    } catch (e: Exception) {
                        Result.failure(Exception("JSON 解析失败: ${e.message}"))
                    }
                } else {
                    Result.failure(Exception("请求失败: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun fetchSemesterStart(): String? = withContext(Dispatchers.IO) {
        try {
            val response = api.getCalendar()
            if (!response.isSuccessful) return@withContext null
            val html = response.body()?.string() ?: ""
            val regex = Regex("""(\d{4}-\d{2}-\d{2})\s*至""")
            regex.find(html)?.groupValues?.get(1)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractCSRFToken(html: String): String? {
        val patterns = listOf(
            Regex("""<input\s+type="hidden"\s+id="csrftoken"\s+name="csrftoken"\s+value="([^"]+)"\s*/>"""),
            Regex("""name="csrftoken"\s+value="([^"]+)""""),
            Regex("""id="csrftoken".*?value="([^"]+)"""")
        )
        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    suspend fun getGrades(
        account: CourseAccountEntity,
        year: String,
        semester: String
    ): Result<List<StudentGradeResponse.Item>> {
        return executeWithAutoRetry(account) {
            try {
                val response = api.getStudentGrade(
                    year = year,
                    semester = semester,
                    showCount = 100
                )

                if (response.isSuccessful) {
                    val bodyString = response.body()?.string() ?: ""
                    if (bodyString.contains("用户登录") || bodyString.contains("/xtgl/login_slogin.html")) {
                        Result.failure(SessionExpiredException())
                    } else {
                        try {
                            val gradeResponse =
                                json.decodeFromString<StudentGradeResponse>(bodyString)
                            Result.success(gradeResponse.items)
                        } catch (e: Exception) {
                            Result.failure(Exception("解析成绩失败: ${e.message}"))
                        }
                    }
                } else {
                    if (response.code() == 901) {
                        Result.failure(SessionExpiredException())
                    } else {
                        Result.failure(Exception("HTTP请求失败: ${response.code()}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 批量同步该用户的所有历史成绩
     */
    suspend fun fetchAllHistoryGrades(account: CourseAccountEntity): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val startYear = account.njdmId.toIntOrNull() ?: (currentYear - 4)
                val endYear = currentYear + 1

                var successCount = 0
                var requestCount = 0

                for (year in startYear..endYear) {
                    val semesters = listOf("3", "12")
                    for (semester in semesters) {
                        requestCount++
                        val result = fetchAndSaveGrades(account, year.toString(), semester)
                        if (result.isSuccess) {
                            successCount++
                        }
                        delay(300)
                    }
                }
                Result.success("同步完成：请求 $requestCount 次，更新 $successCount 个学期")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun fetchAndSaveGrades(
        account: CourseAccountEntity,
        year: String,
        semester: String
    ): Result<Unit> {
        return executeWithAutoRetry(account) {
            try {
                val response = api.getStudentGrade(year = year, semester = semester)

                if (response.isSuccessful) {
                    val bodyString = response.body()?.string() ?: ""
                    if (bodyString.contains("用户登录") || bodyString.contains("/xtgl/login_slogin.html")) {
                        Result.failure(SessionExpiredException())
                    } else {
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

                        // 无论列表是否为空，都执行更新（先删后存），确保本地数据与服务器一致
                        gradeDao.updateGrades(account.studentId, year, semester, entities)

                        Result.success(Unit)
                    }
                } else {
                    if (response.code() == 901) Result.failure(SessionExpiredException())
                    else Result.failure(Exception("HTTP Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun observeGrades(studentId: String, xnm: String, xqm: String): Flow<List<GradeEntity>> {
        return gradeDao.getGradesFlow(studentId, xnm, xqm)
    }


    //    获取教务系统页面的课表更新信息
    suspend fun getAcademicCourseInfo(
        account: CourseAccountEntity
    ): Result<List<String>> {
        return executeWithAutoRetry(account) {
            try {
                val response = api.getAcademicCourseInfo()

                // 1. 检查 Session 失效
                if (response.code() == 901 || response.code() == 302) {
                    return@executeWithAutoRetry Result.failure(SessionExpiredException())
                }

                if (response.isSuccessful) {
                    val html = response.body()?.string() ?: ""

                    // 2. 检查 HTML 是否是登录页
                    if (html.contains("用户登录") || html.contains("/xtgl/login_slogin.html")) {
                        return@executeWithAutoRetry Result.failure(SessionExpiredException())
                    }

//                    val doc = Jsoup.parse(html)
//                    // 找到所有列表项
//                    val messages = doc.select("div#kbDiv a.list-group-item")
//                        .map { element ->
//                            // 提取 span class="title" 里面的文字
//                            // .trim() 用于去除前后的换行符和空格
//                            element.select("span.title").text().trim()
//                        }
//                        // 过滤掉空行
//                        .filter { it.isNotEmpty() }

                    Result.success(htmlParse(html))
                } else {
                    Result.failure(Exception("请求失败，状态码: ${response.code()}"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}