package com.suseoaa.projectoaa.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.suseoaa.projectoaa.data.api.SchoolApiService
import com.suseoaa.projectoaa.data.model.CourseAccountEntity
import com.suseoaa.projectoaa.database.CourseDatabase
import com.suseoaa.projectoaa.database.ExamCache
import com.suseoaa.projectoaa.database.MessageCache
import com.suseoaa.projectoaa.presentation.academic.ExamResponse
import com.suseoaa.projectoaa.util.HtmlParser
import com.suseoaa.projectoaa.util.getCurrentTerm
import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/**
 * 考试缓存实体类 (用于UI层)
 */
data class ExamCacheEntity(
    val id: Long = 0,
    val studentId: String,
    val courseName: String,
    val time: String,
    val location: String
)

/**
 * 消息缓存实体类 (用于UI层)
 */
data class MessageCacheEntity(
    val id: Long = 0,
    val studentId: String,
    val content: String,
    val date: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * 教务系统信息仓库 - 处理考试信息和调课通知
 */
class SchoolInfoRepository(
    private val api: SchoolApiService,
    private val database: CourseDatabase,
    private val json: Json,
    private val authRepository: SchoolAuthRepository
) {

    // ==================== 考试信息 (缓存+网络) ====================

    /**
     * 观察考试信息流
     */
    fun observeExams(studentId: String): Flow<List<ExamCacheEntity>> {
        return database.examCacheQueries.selectByStudent(studentId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toEntity() } }
    }

    /**
     * 刷新考试信息
     */
    suspend fun refreshAcademicExamInfo(account: CourseAccountEntity): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val (xnm, xqm) = getCurrentTerm()
                
                // 1. 发起请求
                var response = api.getExamList(xnm, xqm)

                // 2. 自动重试（检查是否需要登录）
                val contentType = response.headers["Content-Type"] ?: ""
                if (!response.status.value.let { it == 200 } || contentType.contains("html")) {
                    authRepository.login(account.studentId, account.password)
                    response = api.getExamList(xnm, xqm)
                }

                // 3. 处理响应并写入数据库
                if (response.status.value == 200) {
                    val bodyText = response.bodyAsText()
                    val examResponse = json.decodeFromString<ExamResponse>(bodyText)
                    val items = examResponse.items ?: emptyList()

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
                    updateExams(account.studentId, entities)
                    Result.success("刷新成功")
                } else {
                    Result.failure(Exception("获取考试信息失败: ${response.status.value}"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    /**
     * 更新考试缓存
     */
    private suspend fun updateExams(studentId: String, exams: List<ExamCacheEntity>) =
        withContext(Dispatchers.IO) {
            database.transaction {
                database.examCacheQueries.deleteByStudent(studentId)
                exams.forEach { exam ->
                    database.examCacheQueries.insert(
                        studentId = exam.studentId,
                        courseName = exam.courseName,
                        time = exam.time,
                        location = exam.location
                    )
                }
            }
        }

    // ==================== 调课通知 (缓存+网络) ====================

    /**
     * 观察调课通知流
     */
    fun observeMessages(studentId: String): Flow<List<MessageCacheEntity>> {
        return database.messageCacheQueries.selectByStudent(studentId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toEntity() } }
    }

    /**
     * 刷新调课通知
     */
    suspend fun refreshAcademicMessageInfo(account: CourseAccountEntity): Result<String> =
        fetchAndSaveMessages(account)

    private suspend fun fetchAndSaveMessages(account: CourseAccountEntity): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                var response = api.getAcademicMessageInfo()
                var bodyString = response.bodyAsText()

                if (response.status.value == 901 || response.status.value == 302 || isLoginRequired(bodyString)) {
                    val loginResult = authRepository.login(account.studentId, account.password)
                    if (loginResult.isFailure) return@withContext Result.failure(Exception("自动登录失败"))
                    response = api.getAcademicMessageInfo()
                    bodyString = response.bodyAsText()
                }

                if (response.status.value == 200) {
                    val rawList = HtmlParser.htmlParse(bodyString)
                    val entities = rawList.map { content ->
                        MessageCacheEntity(
                            studentId = account.studentId,
                            content = content
                        )
                    }
                    updateMessages(account.studentId, entities)
                    Result.success("刷新成功")
                } else {
                    Result.failure(Exception("请求失败: ${response.status.value}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 更新消息缓存
     */
    private suspend fun updateMessages(studentId: String, messages: List<MessageCacheEntity>) =
        withContext(Dispatchers.IO) {
            database.transaction {
                database.messageCacheQueries.deleteByStudent(studentId)
                messages.forEach { msg ->
                    database.messageCacheQueries.insert(
                        studentId = msg.studentId,
                        content = msg.content,
                        date = msg.date
                    )
                }
            }
        }

    // ==================== 课程信息获取 ====================

    /**
     * 获取课程更新信息 (用于 GetCourseInfoViewModel)
     */
    suspend fun getAcademicCourseInfo(account: CourseAccountEntity): Result<List<String>> =
        fetchAndParseHtml(account) { api.getAcademicCourseInfo() }

    private suspend fun fetchAndParseHtml(
        account: CourseAccountEntity,
        request: suspend () -> HttpResponse
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            var response = request()
            var bodyString = response.bodyAsText()

            if (response.status.value == 901 || response.status.value == 302 || isLoginRequired(bodyString)) {
                val loginResult = authRepository.login(account.studentId, account.password)
                if (loginResult.isFailure) return@withContext Result.failure(Exception("自动登录失败"))

                response = request()
                bodyString = response.bodyAsText()
            }

            if (response.status.value == 200) {
                val result = HtmlParser.htmlParse(bodyString)
                Result.success(result)
            } else {
                Result.failure(Exception("请求失败: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 辅助方法 ====================

    private fun isLoginRequired(html: String?): Boolean =
        html != null && (html.contains("用户登录") || html.contains("/xtgl/login_slogin.html"))
}

/**
 * 扩展函数：将 SQLDelight 生成的 ExamCache 转换为 ExamCacheEntity
 */
private fun ExamCache.toEntity() = ExamCacheEntity(
    id = id,
    studentId = studentId,
    courseName = courseName,
    time = time,
    location = location
)

/**
 * 扩展函数：将 SQLDelight 生成的 MessageCache 转换为 MessageCacheEntity
 */
private fun MessageCache.toEntity() = MessageCacheEntity(
    id = id,
    studentId = studentId,
    content = content,
    date = date
)
