package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.data.api.CheckinApiService
import com.suseoaa.projectoaa.data.model.*
import com.suseoaa.projectoaa.database.CourseDatabase
import com.suseoaa.projectoaa.util.CheckinRSAEncryptor
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * 652打卡仓库 - 处理打卡账号管理和打卡操作
 * 
 * 打卡流程：
 * 1. 获取验证码图片并由用户手动输入
 * 2. 登录 UIAS 统一认证系统
 * 3. 获取用户组编码
 * 4. 检查待签任务列表（确认有任务可签）
 * 5. 执行打卡
 */
class CheckinRepository(
    private val api: CheckinApiService,
    private val database: CourseDatabase,
    private val json: Json
) {
    private val queries get() = database.checkinAccountQueries
    
    // 缓存登录页面的 execution token
    private var cachedExecution: String? = null
    
    // ==================== 账号管理 ====================
    
    /**
     * 获取所有打卡账号
     */
    fun getAllAccounts(): List<CheckinAccountData> {
        return queries.selectAll().executeAsList().map { it.toData() }
    }
    
    /**
     * 添加打卡账号
     */
    fun addAccount(studentId: String, password: String, name: String = "", remark: String = ""): Result<Unit> {
        return try {
            val now = getCurrentTimeString()
            queries.insert(
                studentId = studentId,
                password = password,
                name = name,
                remark = remark,
                createdAt = now,
                updatedAt = now
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 更新打卡账号
     */
    fun updateAccount(id: Long, studentId: String, password: String, name: String, remark: String): Result<Unit> {
        return try {
            val now = getCurrentTimeString()
            queries.update(
                studentId = studentId,
                password = password,
                name = name,
                remark = remark,
                updatedAt = now,
                id = id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 删除打卡账号
     */
    fun deleteAccount(id: Long): Result<Unit> {
        return try {
            queries.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 检查账号是否存在
     */
    fun isAccountExists(studentId: String): Boolean {
        return queries.selectByStudentId(studentId).executeAsOneOrNull() != null
    }
    
    // ==================== 打卡操作（需要手动输入验证码） ====================
    
    // 注意: 打卡现在需要通过以下流程:
    // 1. ViewModel 调用 fetchCaptchaImage() 获取验证码
    // 2. UI 显示验证码，用户输入
    // 3. ViewModel 调用 loginWithCaptcha() 登录
    // 4. ViewModel 调用 performCheckinAfterLogin() 执行打卡
    
    // ==================== 公开方法（供 ViewModel 调用）====================
    
    /**
     * 获取验证码图片（公开方法，供 ViewModel 调用）
     * 同时会缓存 execution token
     */
    suspend fun fetchCaptchaImage(): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] 开始获取验证码...")
            
            // 1. 获取登录页面提取 execution token
            val loginPageResponse = api.getLoginPage()
            println("[Checkin] 登录页面响应状态: ${loginPageResponse.status}")
            
            if (loginPageResponse.status.value != 200) {
                return@withContext Result.failure(Exception("无法访问登录页面 (${loginPageResponse.status.value})"))
            }
            
            val pageHtml = loginPageResponse.bodyAsText()
            cachedExecution = extractExecution(pageHtml)
            if (cachedExecution == null) {
                return@withContext Result.failure(Exception("未找到 execution token"))
            }
            
            println("[Checkin] execution token: ${cachedExecution?.take(30)}...")
            
            // 2. 获取验证码图片
            val captchaImageBytes = api.getCaptchaImage()
            println("[Checkin] 验证码图片大小: ${captchaImageBytes.size} bytes")
            
            Result.success(captchaImageBytes)
        } catch (e: Exception) {
            println("[Checkin] 获取验证码异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 使用手动输入的验证码登录
     */
    suspend fun loginWithCaptcha(username: String, password: String, captchaCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val execution = cachedExecution
                ?: return@withContext Result.failure(Exception("请先获取验证码"))
            
            println("[Checkin] 开始登录: username=$username, execution=${execution.take(20)}..., captcha=$captchaCode")
            
            // 1. RSA 加密密码（密码需要反转）
            val reversedPassword = password.reversed()
            val encryptedPassword = CheckinRSAEncryptor.encrypt(
                reversedPassword,
                CheckinApiService.RSA_MODULUS,
                CheckinApiService.RSA_EXPONENT
            )
            
            // 2. 提交登录
            val loginResponse = api.submitLogin(encryptedPassword, username, execution, captchaCode)
            
            println("[Checkin] 登录响应状态: ${loginResponse.status}")
            
            // 3. 检查重定向（成功登录会返回302）
            if (loginResponse.status.value == 302) {
                val redirectUrl = loginResponse.headers["Location"]
                    ?: return@withContext Result.failure(Exception("登录重定向失败"))
                
                println("[Checkin] 登录成功，重定向到: $redirectUrl")
                
                // 4. 跟随重定向获取 Session
                api.followRedirect(redirectUrl)
                cachedExecution = null // 清除缓存
                return@withContext Result.success(Unit)
            }
            
            // 登录失败，解析错误原因
            val responseBody = loginResponse.bodyAsText()
            println("[Checkin] 登录失败，响应长度: ${responseBody.length}")
            
            // 尝试提取具体错误信息
            val errorMsgRegex = Regex("""<div[^>]*class="[^"]*error[^"]*"[^>]*>([^<]+)</div>""")
            val matchResult = errorMsgRegex.find(responseBody)
            val serverError = matchResult?.groupValues?.getOrNull(1)?.trim()
            
            val errorMsg = when {
                serverError != null -> serverError
                responseBody.contains("验证码错误") || responseBody.contains("验证码不正确") -> "验证码错误"
                responseBody.contains("密码") -> "用户名或密码错误"
                responseBody.contains("用户") -> "用户名不存在"
                else -> "登录失败 (${loginResponse.status.value})"
            }
            
            println("[Checkin] 错误原因: $errorMsg")
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            println("[Checkin] 登录异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 登录后执行打卡（已登录状态下调用）
     */
    suspend fun performCheckinAfterLogin(account: CheckinAccountData): CheckinResult = withContext(Dispatchers.IO) {
        try {
            // 1. 获取用户组编码
            val groupCode = getUserGroupCode()
            if (groupCode == null) {
                updateCheckinStatus(account.id, "无用户组")
                return@withContext CheckinResult.Failed("未找到用户组信息")
            }
            
            // 2. 检查待签任务列表
            val pendingTasks = getPendingTasks()
            if (pendingTasks.isEmpty()) {
                updateCheckinStatus(account.id, "无任务")
                return@withContext CheckinResult.NoTask("当前没有待签到的任务")
            }
            
            // 3. 执行打卡
            val result = executeCheckin(groupCode)
            when (result) {
                is CheckinResult.Success -> updateCheckinStatus(account.id, "成功")
                is CheckinResult.AlreadyChecked -> updateCheckinStatus(account.id, "已签到")
                is CheckinResult.NoTask -> updateCheckinStatus(account.id, "无任务")
                is CheckinResult.Failed -> updateCheckinStatus(account.id, "失败")
            }
            result
        } catch (e: Exception) {
            updateCheckinStatus(account.id, "异常: ${e.message}")
            CheckinResult.Failed(e.message ?: "未知错误")
        }
    }
    
    /**
     * 获取用户组编码
     */
    private suspend fun getUserGroupCode(): String? {
        try {
            val response = api.getUserGroups()
            if (response.status.value != 200) return null
            
            val groupsResponse = json.decodeFromString<UserGroupsResponse>(response.bodyAsText())
            if (groupsResponse.resultCode != 0 || !groupsResponse.success) return null
            
            // 返回第一个组的编码
            return groupsResponse.result?.data?.firstOrNull()?.code
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * 获取待签任务列表
     */
    private suspend fun getPendingTasks(): List<CheckinTask> {
        try {
            val response = api.getTaskList(status = 1)  // 1 = 待签到
            if (response.status.value != 200) return emptyList()
            
            val taskListResponse = json.decodeFromString<CheckinTaskListResponse>(response.bodyAsText())
            if (taskListResponse.resultCode != 0) return emptyList()
            
            return taskListResponse.result?.data ?: emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
    }
    
    /**
     * 执行签到
     */
    private suspend fun executeCheckin(groupCode: String): CheckinResult {
        try {
            val response = api.submitCheckin(groupCode)
            val responseText = response.bodyAsText()
            
            val submitResponse = json.decodeFromString<CheckinSubmitResponse>(responseText)
            
            return if (submitResponse.success && submitResponse.resultCode == 0) {
                if (submitResponse.result?.data == true) {
                    CheckinResult.Success("签到成功")
                } else {
                    CheckinResult.AlreadyChecked("已签到或无需签到")
                }
            } else {
                CheckinResult.Failed(submitResponse.errorMsg ?: "签到失败")
            }
        } catch (e: Exception) {
            return CheckinResult.Failed(e.message ?: "签到异常")
        }
    }
    
    /**
     * 更新打卡状态
     */
    private fun updateCheckinStatus(accountId: Long, status: String) {
        val now = getCurrentTimeString()
        queries.updateCheckinStatus(
            lastCheckinTime = now,
            lastCheckinStatus = status,
            updatedAt = now,
            id = accountId
        )
    }
    
    /**
     * 从HTML中提取 execution token
     */
    private fun extractExecution(html: String): String? {
        val regex = """name="execution"\s+value="([^"]+)"""".toRegex()
        return regex.find(html)?.groupValues?.get(1)
    }
    
    /**
     * 获取当前时间字符串
     */
    private fun getCurrentTimeString(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.date} ${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
    }
    
    // ==================== 扩展函数 ====================
    
    private fun com.suseoaa.projectoaa.database.CheckinAccount.toData() = CheckinAccountData(
        id = id,
        studentId = studentId,
        password = password,
        name = name,
        remark = remark,
        lastCheckinTime = lastCheckinTime,
        lastCheckinStatus = lastCheckinStatus,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
