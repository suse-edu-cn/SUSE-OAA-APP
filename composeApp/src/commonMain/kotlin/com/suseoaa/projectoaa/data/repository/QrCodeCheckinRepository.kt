package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.data.api.QrCodeCheckinApiService
import com.suseoaa.projectoaa.data.model.*
import com.suseoaa.projectoaa.data.network.ClearableCookieStorage
import com.suseoaa.projectoaa.database.CourseDatabase
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 扫码签到仓库 - 专门处理微信扫码登录和签到
 * 
 * 与密码登录签到完全隔离，使用独立的API和流程
 * 
 * ★★★ 完整流程（2025年验证） ★★★
 * 1. 获取二维码 (getClientId + getQrCodeUrl)
 * 2. 轮询扫码状态 (checkScanStatus)
 * 3. 扫码成功后处理回调获取 _sop_session_ (handleCallback)
 * 4. ★关键★ 调用 SSO API 获取 SESSION Cookie (getSsoSession)
 *    API: GET /site/appware/system/sso/loginUrl?service=<redirect_url>
 * 5. 保存账号信息到数据库
 * 6. 执行签到 (performCheckin)
 */
class QrCodeCheckinRepository(
    private val api: QrCodeCheckinApiService,
    private val database: CourseDatabase,
    private val json: Json,
    private val cookieStorage: ClearableCookieStorage
) {
    private val queries get() = database.checkinAccountQueries

    // ==================== 扫码登录流程 ====================

    /**
     * 步骤1: 获取微信扫码 ClientId
     */
    suspend fun getClientId(): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("[QrCheckin] 获取 ClientId...")
            val response = api.getClientId()
            
            if (response.status.value != 200) {
                return@withContext Result.failure(Exception("获取 ClientId 失败 (${response.status.value})"))
            }
            
            val responseText = response.bodyAsText()
            println("[QrCheckin] ClientId 响应: $responseText")
            
            val clientIdResponse = json.decodeFromString<WechatClientIdResponse>(responseText)
            val clientId = clientIdResponse.getClientIdValue()
            
            if (clientId.isNullOrBlank()) {
                return@withContext Result.failure(Exception("ClientId 为空"))
            }
            
            println("[QrCheckin] ClientId: $clientId")
            Result.success(clientId)
        } catch (e: Exception) {
            println("[QrCheckin] 获取 ClientId 异常: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 步骤2: 获取微信二维码图片 (Base64)
     */
    suspend fun getQrCodeImage(clientId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("[QrCheckin] 获取二维码, clientId=$clientId")
            val response = api.getQrCodeUrl(clientId)
            
            if (response.status.value != 200) {
                return@withContext Result.failure(Exception("获取二维码失败 (${response.status.value})"))
            }
            
            val responseText = response.bodyAsText()
            println("[QrCheckin] 二维码响应: ${responseText.take(200)}...")
            
            val qrCodeResponse = json.decodeFromString<WechatQrCodeResponse>(responseText)
            val qrCodeImage = qrCodeResponse.data?.getQrCodeImage()
            
            if (qrCodeImage.isNullOrBlank()) {
                return@withContext Result.failure(Exception("二维码图片为空"))
            }
            
            println("[QrCheckin] 二维码图片长度: ${qrCodeImage.length}")
            Result.success(qrCodeImage)
        } catch (e: Exception) {
            println("[QrCheckin] 获取二维码异常: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 步骤3: 检查扫码状态
     * @return ScanStatus: 0=等待扫码, 1=已扫码待确认, 2=已确认
     */
    suspend fun checkScanStatus(clientId: String): Result<WechatScanStatusData> = withContext(Dispatchers.IO) {
        try {
            val response = api.checkScanStatus(clientId)
            
            if (response.status.value != 200) {
                return@withContext Result.failure(Exception("检查扫码状态失败"))
            }
            
            val responseText = response.bodyAsText()
            println("[QrCheckin] 扫码状态: $responseText")
            
            val scanResponse = json.decodeFromString<WechatScanStatusResponse>(responseText)
            val data = scanResponse.data
                ?: return@withContext Result.failure(Exception("扫码状态数据为空"))
            
            Result.success(data)
        } catch (e: Exception) {
            println("[QrCheckin] 检查扫码状态异常: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 步骤4: 处理扫码回调，获取 _sop_session_ Cookie
     * @return _sop_session_ Cookie 字符串
     */
    suspend fun handleScanCallback(callbackUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("[QrCheckin] 处理回调: $callbackUrl")
            val response = api.handleCallback(callbackUrl)
            
            println("[QrCheckin] 回调响应状态: ${response.status.value}")
            
            // 从 Set-Cookie 响应头获取 _sop_session_
            val cookies = response.headers.getAll("Set-Cookie")
            println("[QrCheckin] Set-Cookie: $cookies")
            
            val sopSessionCookie = cookies?.find { it.contains("_sop_session_=") }
                ?.let { extractCookieValue(it, "_sop_session_") }
            
            // 如果响应头没有，尝试从 cookieStorage 获取
            val finalSopSession = sopSessionCookie 
                ?: cookieStorage.getCookiesForHost("qfhy.suse.edu.cn")
                    .find { it.name == "_sop_session_" }?.value
            
            if (finalSopSession.isNullOrBlank()) {
                return@withContext Result.failure(Exception("未获取到 _sop_session_ Cookie"))
            }
            
            println("[QrCheckin] _sop_session_ 长度: ${finalSopSession.length}")
            Result.success("_sop_session_=$finalSopSession")
        } catch (e: Exception) {
            println("[QrCheckin] 处理回调异常: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 步骤5: 获取 SESSION Cookie
     * 
     * ★★★ 关键修复（2025年验证） ★★★
     * 
     * 正确流程：调用 SSO API 获取 SESSION
     * GET /site/appware/system/sso/loginUrl?service=<redirect_url>
     * 
     * 之前的错误：以为 SESSION 是访问任何页面自动创建的
     * 实际情况：SESSION 必须通过专门的 SSO API 获取
     * 
     * @param sopSessionCookie _sop_session_ Cookie 字符串
     * @return 完整的 Cookie 字符串 (包含 _sop_session_ 和 SESSION)
     */
    suspend fun getSessionCookie(sopSessionCookie: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("[QrCheckin] ★★★ 获取 SESSION Cookie (使用 SSO API) ★★★")
            
            // 从 _sop_session_ JWT 中提取 openId
            val sopSessionValue = sopSessionCookie.substringAfter("_sop_session_=")
            val openId = extractOpenIdFromJwt(sopSessionValue)
            
            if (openId.isNullOrBlank()) {
                return@withContext Result.failure(Exception("无法从 JWT 中提取 openId"))
            }
            
            println("[QrCheckin] openId: $openId")
            
            // 清除旧的 cookies
            cookieStorage.clearForHost("qfhy.suse.edu.cn")
            
            // ★★★ 关键步骤 ★★★
            // 调用 SSO API 获取 SESSION
            // GET /site/appware/system/sso/loginUrl?service=<redirect_url>
            println("[QrCheckin] 调用 SSO API: /site/appware/system/sso/loginUrl")
            
            val ssoResponse = api.getSsoSession(sopSessionCookie, openId)
            
            println("[QrCheckin] SSO API 响应状态: ${ssoResponse.status.value}")
            
            // 从 Set-Cookie 响应头获取 SESSION
            var sessionValue: String? = null
            ssoResponse.headers.getAll("Set-Cookie")?.forEach { cookie ->
                println("[QrCheckin] Set-Cookie: $cookie")
                if (cookie.contains("SESSION=")) {
                    sessionValue = extractCookieValue(cookie, "SESSION")
                }
            }
            
            // 如果响应头没有，尝试从 cookieStorage 获取
            if (sessionValue.isNullOrBlank()) {
                sessionValue = cookieStorage.getCookiesForHost("qfhy.suse.edu.cn")
                    .find { it.name == "SESSION" }?.value
                println("[QrCheckin] 从 cookieStorage 获取 SESSION: ${sessionValue?.take(20)}...")
            }
            
            if (sessionValue.isNullOrBlank()) {
                println("[QrCheckin] ★★★ SSO API 未返回 SESSION，打印调试信息 ★★★")
                println("[QrCheckin] 响应 Location: ${ssoResponse.headers["Location"]}")
                cookieStorage.debugPrintAllCookies()
                return@withContext Result.failure(Exception("SSO API 未返回 SESSION Cookie"))
            }
            
            val fullCookies = "$sopSessionCookie; SESSION=$sessionValue"
            println("[QrCheckin] ★★★ 成功获取 SESSION: ${sessionValue.take(20)}... ★★★")
            println("[QrCheckin] 完整 Cookie 长度: ${fullCookies.length}")
            Result.success(fullCookies)
        } catch (e: Exception) {
            println("[QrCheckin] 获取 SESSION 异常: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 从 JWT 中提取 ticket
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun extractTicketFromJwt(jwtValue: String): String? {
        try {
            val parts = jwtValue.split(".")
            if (parts.size != 3) return null
            
            val payload = parts[1]
            val paddedPayload = when (payload.length % 4) {
                2 -> payload + "=="
                3 -> payload + "="
                else -> payload
            }
            val decodedPayload = Base64.UrlSafe.decode(paddedPayload).decodeToString()
            
            val payloadJson = json.parseToJsonElement(decodedPayload).jsonObject
            return payloadJson["ticket"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            println("[QrCheckin] 提取 ticket 失败: ${e.message}")
            return null
        }
    }

    /**
     * 从 Cookie 中提取用户信息 (学号、姓名)
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun extractUserInfoFromCookie(sopSessionCookie: String): Pair<String, String>? {
        try {
            val sopSessionValue = sopSessionCookie.substringAfter("_sop_session_=")
            
            val parts = sopSessionValue.split(".")
            if (parts.size != 3) return null
            
            val payload = parts[1]
            val paddedPayload = when (payload.length % 4) {
                2 -> payload + "=="
                3 -> payload + "="
                else -> payload
            }
            val decodedPayload = Base64.UrlSafe.decode(paddedPayload).decodeToString()
            println("[QrCheckin] JWT payload: $decodedPayload")
            
            val payloadJson = json.parseToJsonElement(decodedPayload).jsonObject
            
            // 提取学号 (uid)
            val uid = payloadJson["uid"]?.jsonPrimitive?.content ?: return null
            
            // 尝试从 extra 中提取姓名
            var userName = ""
            val extraString = payloadJson["extra"]?.jsonPrimitive?.content
            if (!extraString.isNullOrBlank()) {
                try {
                    val extraJson = json.parseToJsonElement(extraString).jsonObject
                    userName = extraJson["userName"]?.jsonPrimitive?.content ?: ""
                } catch (e: Exception) {
                    println("[QrCheckin] 解析 extra 失败: ${e.message}")
                }
            }
            
            println("[QrCheckin] 用户信息: uid=$uid, userName=$userName")
            return Pair(uid, userName)
        } catch (e: Exception) {
            println("[QrCheckin] 提取用户信息失败: ${e.message}")
            return null
        }
    }

    /**
     * 从 _sop_session_ JWT 中提取用户信息
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun extractUserInfoFromSopSession(sopSession: String): Pair<String, String>? {
        try {
            val parts = sopSession.split(".")
            if (parts.size != 3) return null
            
            val payload = parts[1]
            val paddedPayload = when (payload.length % 4) {
                2 -> payload + "=="
                3 -> payload + "="
                else -> payload
            }
            val decodedPayload = Base64.UrlSafe.decode(paddedPayload).decodeToString()
            println("[QrCheckin] JWT payload: $decodedPayload")
            
            val payloadJson = json.parseToJsonElement(decodedPayload).jsonObject
            
            // 提取学号 (uid)
            val uid = payloadJson["uid"]?.jsonPrimitive?.content ?: return null
            
            // 尝试从 extra 中提取姓名
            var userName = ""
            val extraString = payloadJson["extra"]?.jsonPrimitive?.content
            if (!extraString.isNullOrBlank()) {
                try {
                    val extraJson = json.parseToJsonElement(extraString).jsonObject
                    userName = extraJson["userName"]?.jsonPrimitive?.content ?: ""
                } catch (e: Exception) {
                    println("[QrCheckin] 解析 extra 失败: ${e.message}")
                }
            }
            
            println("[QrCheckin] 用户信息: uid=$uid, userName=$userName")
            return Pair(uid, userName)
        } catch (e: Exception) {
            println("[QrCheckin] 提取用户信息失败: ${e.message}")
            return null
        }
    }

    /**
     * 使用 Cookie 获取用户信息
     */
    suspend fun getEduUserInfoWithCookies(cookies: String): Result<EduUserInfo> = withContext(Dispatchers.IO) {
        try {
            println("[QrCheckin] 使用 Cookie 获取用户信息...")
            val response = api.getUserInfo(cookies)
            
            if (response.status.value != 200) {
                return@withContext Result.failure(Exception("获取用户信息失败 (${response.status.value})"))
            }
            
            val responseText = response.bodyAsText()
            println("[QrCheckin] 用户信息响应: $responseText")
            
            val userInfoResponse = json.decodeFromString<EduUserInfoResponse>(responseText)
            
            if (userInfoResponse.code != 200) {
                return@withContext Result.failure(Exception(userInfoResponse.msg ?: "获取用户信息失败"))
            }
            
            val userInfo = userInfoResponse.data 
                ?: return@withContext Result.failure(Exception("用户信息为空"))
            
            Result.success(userInfo)
        } catch (e: Exception) {
            println("[QrCheckin] 获取用户信息异常: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 使用 _sop_session_ 完成 SSO 获取 SESSION Cookie
     */
    suspend fun completeSsoWithSopSession(cookies: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("[QrCheckin] 使用 SSO 获取 SESSION...")
            
            // 从 cookies 中提取 _sop_session_
            val sopSession = cookies.split(";")
                .map { it.trim() }
                .find { it.startsWith("_sop_session_=") }
                ?.substringAfter("_sop_session_=")
            
            if (sopSession.isNullOrBlank()) {
                return@withContext Result.failure(Exception("未找到 _sop_session_"))
            }
            
            // 获取 SESSION Cookie
            val result = getSessionCookie("_sop_session_=$sopSession")
            result
        } catch (e: Exception) {
            println("[QrCheckin] SSO 获取 SESSION 异常: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 使用保存的 Session 执行签到
     */
    suspend fun performCheckinWithSession(account: CheckinAccountData): CheckinResult = withContext(Dispatchers.IO) {
        try {
            val cookies = account.sessionToken
            if (cookies.isNullOrBlank()) {
                return@withContext CheckinResult.Failed("Session 无效，请重新扫码登录")
            }
            
            // 检查 Session 是否过期
            if (!account.isSessionValid()) {
                return@withContext CheckinResult.Failed("Session 已过期，请重新扫码登录")
            }
            
            // 获取待签到任务
            val pendingResult = getPendingTasks(cookies)
            if (pendingResult.isFailure) {
                val error = pendingResult.exceptionOrNull()?.message ?: "获取任务失败"
                if (error.contains("401") || error.contains("未登录") || error.contains("过期")) {
                    return@withContext CheckinResult.Failed("Session 已过期，请重新扫码登录")
                }
                return@withContext CheckinResult.Failed(error)
            }
            
            val pendingTasks = pendingResult.getOrThrow()
            if (pendingTasks.isEmpty()) {
                return@withContext CheckinResult.NoTask("当前没有需要签到的任务")
            }
            
            // 获取签到地点
            val location = CheckinLocations.fromName(account.selectedLocation)
            
            // 对第一个任务执行签到
            val task = pendingTasks.first()
            performCheckin(cookies, task.id, location)
        } catch (e: Exception) {
            println("[QrCheckin] 签到异常: ${e.message}")
            CheckinResult.Failed(e.message ?: "签到失败")
        }
    }

    /**
     * 对指定任务执行签到
     */
    suspend fun checkinForSpecificTask(
        cookies: String,
        taskId: Long,
        account: CheckinAccountData
    ): CheckinResult = withContext(Dispatchers.IO) {
        try {
            val location = CheckinLocations.fromName(account.selectedLocation)
            performCheckin(cookies, taskId, location)
        } catch (e: Exception) {
            println("[QrCheckin] 指定任务签到异常: ${e.message}")
            CheckinResult.Failed(e.message ?: "签到失败")
        }
    }

    /**
     * 获取所有任务 (使用 Cookies)
     */
    suspend fun getAllTasksWithCookies(cookies: String, initialLoadCount: Int = 5): Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>> {
        return getAllTasks(cookies, initialLoadCount = initialLoadCount)
    }

    /**
     * 保存扫码账号 (供 ViewModel 调用)
     */
    fun saveQrCodeAccount(
        studentId: String,
        name: String,
        sessionToken: String,
        sessionExpireTime: String,
        selectedLocation: String
    ): Result<Long> {
        return saveQrCodeAccount(studentId, name, sessionToken, selectedLocation)
    }

    // ==================== 签到功能 ====================

    /**
     * 获取待签到任务列表 (status=1)
     */
    suspend fun getPendingTasks(cookies: String, openId: String? = null): Result<List<CheckinTask>> = 
        getTaskList(cookies, 1, openId)

    /**
     * 获取已完成任务列表 (status=2)
     * 初始加载时会获取前 5 个任务的签到时间
     * 任务按时间倒序排列，最近的在最上面
     */
    suspend fun getCompletedTasks(cookies: String, openId: String? = null, initialLoadCount: Int = 5): Result<List<CheckinTask>> = 
        withContext(Dispatchers.IO) {
            try {
                val effectiveOpenId = openId ?: extractOpenIdFromCookie(cookies)
                println("[QrCheckin] 获取已完成任务列表, openId=$effectiveOpenId")
                
                val response = api.getTaskList(cookies, 2, effectiveOpenId)
                
                if (response.status.value != 200) {
                    return@withContext Result.failure(Exception("获取任务列表失败 (${response.status.value})"))
                }
                
                val responseText = response.bodyAsText()
                val taskListResponse = json.decodeFromString<CheckinTaskListResponse>(responseText)
                
                if (!taskListResponse.success || taskListResponse.resultCode != 0) {
                    return@withContext Result.failure(Exception(taskListResponse.errorMsg ?: "获取任务列表失败"))
                }
                
                val tasks = taskListResponse.result?.data ?: emptyList()
                println("[QrCheckin] 获取到 ${tasks.size} 个已完成任务")
                
                // 先按时间倒序排列（最近的在最上面）
                val sortedTasks = tasks.sortedByDescending { task ->
                    task.needTime.ifEmpty { task.qdksrq }
                }
                
                // 只为前 initialLoadCount 个任务获取签到时间
                val tasksWithTime = sortedTasks.mapIndexed { index, task ->
                    if (index < initialLoadCount) {
                        try {
                            val detailResponse = api.getTaskDetail(cookies, task.id, effectiveOpenId)
                            if (detailResponse.status.value == 200) {
                                val detailText = detailResponse.bodyAsText()
                                val detailResult = json.decodeFromString<CheckinDetailResponse>(detailText)
                                val dkxx = detailResult.result?.data?.dkxx
                                if (dkxx != null && !dkxx.qdsj.isNullOrBlank()) {
                                    task.copy(qdsj = dkxx.qdsj, qdzt = dkxx.qdzt)
                                } else {
                                    task
                                }
                            } else {
                                task
                            }
                        } catch (e: Exception) {
                            println("[QrCheckin] 获取任务 ${task.id} 详情失败: ${e.message}")
                            task
                        }
                    } else {
                        task // 超过 initialLoadCount 的不获取详情
                    }
                }
                
                Result.success(tasksWithTime)
            } catch (e: Exception) {
                println("[QrCheckin] 获取已完成任务列表异常: ${e.message}")
                Result.failure(e)
            }
        }
    
    /**
     * 为指定范围的任务加载打卡时间
     * @param tasks 任务列表
     * @param startIndex 起始索引（包含）
     * @param endIndex 结束索引（不包含）
     * @param cookies Cookie
     * @param openId OpenID
     * @return 更新后的任务列表
     */
    suspend fun loadCheckinTimeForTasks(
        tasks: List<CheckinTask>,
        startIndex: Int,
        endIndex: Int,
        cookies: String,
        openId: String? = null
    ): Result<List<CheckinTask>> = withContext(Dispatchers.IO) {
        try {
            val effectiveOpenId = openId ?: extractOpenIdFromCookie(cookies)
            println("[QrCheckin] 加载打卡时间: [$startIndex, $endIndex)")
            
            val updatedTasks = tasks.toMutableList()
            
            for (i in startIndex until minOf(endIndex, tasks.size)) {
                val task = tasks[i]
                // 如果已经有打卡时间，跳过
                if (!task.qdsj.isNullOrBlank()) {
                    continue
                }
                
                try {
                    val detailResponse = api.getTaskDetail(cookies, task.id, effectiveOpenId)
                    if (detailResponse.status.value == 200) {
                        val detailText = detailResponse.bodyAsText()
                        val detailResult = json.decodeFromString<CheckinDetailResponse>(detailText)
                        val dkxx = detailResult.result?.data?.dkxx
                        if (dkxx != null && !dkxx.qdsj.isNullOrBlank()) {
                            updatedTasks[i] = task.copy(qdsj = dkxx.qdsj, qdzt = dkxx.qdzt)
                        }
                    }
                } catch (e: Exception) {
                    println("[QrCheckin] 获取任务 ${task.id} 详情失败: ${e.message}")
                }
            }
            
            Result.success(updatedTasks)
        } catch (e: Exception) {
            println("[QrCheckin] 批量加载打卡时间异常: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 获取缺勤任务列表 (status=3)
     */
    suspend fun getAbsentTasks(cookies: String, openId: String? = null): Result<List<CheckinTask>> = 
        getTaskList(cookies, 3, openId)

    /**
     * 获取任务列表
     * 按时间倒序排列，最近的在最上面
     */
    private suspend fun getTaskList(cookies: String, status: Int, openId: String? = null): Result<List<CheckinTask>> = 
        withContext(Dispatchers.IO) {
            try {
                val effectiveOpenId = openId ?: extractOpenIdFromCookie(cookies)
                println("[QrCheckin] 获取任务列表, status=$status, openId=$effectiveOpenId")
                
                val response = api.getTaskList(cookies, status, effectiveOpenId)
                
                if (response.status.value != 200) {
                    return@withContext Result.failure(Exception("获取任务列表失败 (${response.status.value})"))
                }
                
                val responseText = response.bodyAsText()
                println("[QrCheckin] 任务列表响应: ${responseText.take(300)}...")
                
                val taskListResponse = json.decodeFromString<CheckinTaskListResponse>(responseText)
                
                if (!taskListResponse.success || taskListResponse.resultCode != 0) {
                    return@withContext Result.failure(Exception(taskListResponse.errorMsg ?: "获取任务列表失败"))
                }
                
                val tasks = taskListResponse.result?.data ?: emptyList()
                // 按时间倒序排列（最近的在最上面）
                val sortedTasks = tasks.sortedByDescending { task ->
                    task.needTime.ifEmpty { task.qdksrq }
                }
                println("[QrCheckin] 获取到 ${sortedTasks.size} 个任务 (status=$status)")
                Result.success(sortedTasks)
            } catch (e: Exception) {
                println("[QrCheckin] 获取任务列表异常: ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * 获取所有任务
     * @return Triple<待签到, 已完成, 缺勤>
     */
    suspend fun getAllTasks(cookies: String, openId: String? = null, initialLoadCount: Int = 5): Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>> {
        val pending = getPendingTasks(cookies, openId).getOrElse { emptyList() }
        val completed = getCompletedTasks(cookies, openId, initialLoadCount).getOrElse { emptyList() }
        val absent = getAbsentTasks(cookies, openId).getOrElse { emptyList() }
        return Triple(pending, completed, absent)
    }

    /**
     * 执行签到
     * 
     * @param cookies 完整的 Cookie 字符串 (包含 SESSION)
     * @param taskId 任务ID
     * @param location 签到地点
     * @param openId 微信 OpenId (可选，用于 Referer)
     */
    suspend fun performCheckin(
        cookies: String,
        taskId: Long,
        location: CheckinLocation,
        openId: String? = null
    ): CheckinResult = withContext(Dispatchers.IO) {
        try {
            val effectiveOpenId = openId ?: extractOpenIdFromCookie(cookies)
            println("[QrCheckin] 执行签到, taskId=$taskId, location=${location.name}")
            
            // 1. 获取任务详情，获取签到记录ID
            val detailResponse = api.getTaskDetail(cookies, taskId, effectiveOpenId)
            
            if (detailResponse.status.value != 200) {
                return@withContext CheckinResult.Failed("获取任务详情失败 (${detailResponse.status.value})")
            }
            
            val detailText = detailResponse.bodyAsText()
            println("[QrCheckin] 任务详情: ${detailText.take(300)}...")
            
            val detailResult = json.decodeFromString<CheckinDetailResponse>(detailText)
            
            if (!detailResult.success || detailResult.resultCode != 0) {
                return@withContext CheckinResult.Failed("获取任务详情失败: ${detailResult.errorMsg}")
            }
            
            val dkxx = detailResult.result?.data?.dkxx
                ?: return@withContext CheckinResult.Failed("任务详情数据为空")
            
            val signId = dkxx.id
            val currentStatus = dkxx.qdzt
            println("[QrCheckin] 签到记录ID: $signId, 当前状态: $currentStatus")
            
            // 2. 构造签到请求
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val qdsj = "${now.date} ${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
            
            val signData = buildJsonObject {
                put("id", signId)
                put("qdzt", 1)  // 签到状态：1=已签到
                put("qdsj", qdsj)
                put("isOuted", 0)
                put("isLated", 0)
                put("dkddPhoto", "")
                put("qdddjtdz", location.address)
                put("location", location.locationJson)
                put("txxx", "{}")
            }
            
            println("[QrCheckin] 签到数据: $signData")
            
            // 3. 提交签到
            val submitResponse = api.submitCheckin(cookies, signData.toString(), effectiveOpenId)
            val submitText = submitResponse.bodyAsText()
            println("[QrCheckin] 签到响应: $submitText")
            
            val submitResult = json.decodeFromString<CheckinSubmitResponse>(submitText)
            
            return@withContext if (submitResult.success && submitResult.resultCode == 0) {
                if (currentStatus == 1) {
                    CheckinResult.Success("再次签到成功")
                } else {
                    CheckinResult.Success("签到成功")
                }
            } else {
                CheckinResult.Failed(submitResult.errorMsg ?: "签到失败")
            }
        } catch (e: Exception) {
            println("[QrCheckin] 签到异常: ${e.message}")
            e.printStackTrace()
            CheckinResult.Failed(e.message ?: "未知错误")
        }
    }

    // ==================== 账号管理 ====================

    /**
     * 保存扫码登录账号到数据库
     */
    fun saveQrCodeAccount(
        studentId: String,
        name: String,
        cookies: String,
        selectedLocation: String = CheckinLocations.DEFAULT.name
    ): Result<Long> {
        return try {
            val now = getCurrentTimeString()
            // 计算 Session 过期时间 (假设24小时后过期)
            val expireTime = calculateExpireTime(24)
            
            // 检查是否已存在
            val existing = queries.selectByStudentId(studentId).executeAsOneOrNull()
            if (existing != null) {
                // 更新现有账号
                queries.updateSession(
                    sessionToken = cookies,
                    sessionExpireTime = expireTime,
                    updatedAt = now,
                    id = existing.id
                )
                println("[QrCheckin] 更新账号: $studentId")
                Result.success(existing.id)
            } else {
                // 插入新账号
                queries.insertQrCodeAccount(
                    studentId = studentId,
                    name = name,
                    remark = "扫码登录",
                    createdAt = now,
                    updatedAt = now,
                    sessionToken = cookies,
                    sessionExpireTime = expireTime,
                    selectedLocation = selectedLocation
                )
                val newId = queries.selectByStudentId(studentId).executeAsOneOrNull()?.id ?: 0L
                println("[QrCheckin] 新增账号: $studentId, id=$newId")
                Result.success(newId)
            }
        } catch (e: Exception) {
            println("[QrCheckin] 保存账号失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 更新账号 Session
     */
    fun updateAccountSession(accountId: Long, cookies: String): Result<Unit> {
        return try {
            val now = getCurrentTimeString()
            val expireTime = calculateExpireTime(24)
            queries.updateSession(
                sessionToken = cookies,
                sessionExpireTime = expireTime,
                updatedAt = now,
                id = accountId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新签到状态
     */
    fun updateCheckinStatus(accountId: Long, status: String) {
        val now = getCurrentTimeString()
        queries.updateCheckinStatus(
            lastCheckinTime = now,
            lastCheckinStatus = status,
            updatedAt = now,
            id = accountId
        )
    }

    // ==================== 工具方法 ====================

    /**
     * 从 Cookie 字符串中提取 openId
     */
    private fun extractOpenIdFromCookie(cookies: String): String? {
        val sopSessionValue = cookies.split(";")
            .map { it.trim() }
            .find { it.startsWith("_sop_session_=") }
            ?.substringAfter("_sop_session_=")
            ?: return null
        return extractOpenIdFromJwt(sopSessionValue)
    }

    /**
     * 从 JWT 中提取 openId
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun extractOpenIdFromJwt(jwtValue: String): String? {
        try {
            val parts = jwtValue.split(".")
            if (parts.size != 3) return null
            
            val payload = parts[1]
            val paddedPayload = when (payload.length % 4) {
                2 -> payload + "=="
                3 -> payload + "="
                else -> payload
            }
            val decodedPayload = Base64.UrlSafe.decode(paddedPayload).decodeToString()
            
            val payloadJson = json.parseToJsonElement(decodedPayload).jsonObject
            val extraString = payloadJson["extra"]?.jsonPrimitive?.content ?: return null
            val extraJson = json.parseToJsonElement(extraString).jsonObject
            
            return extraJson["openId"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            println("[QrCheckin] 提取 openId 失败: ${e.message}")
            return null
        }
    }

    /**
     * 从 Set-Cookie 字符串中提取 Cookie 值
     */
    private fun extractCookieValue(setCookie: String, name: String): String? {
        val regex = Regex("$name=([^;]+)")
        return regex.find(setCookie)?.groupValues?.get(1)
    }

    /**
     * 获取当前时间字符串
     */
    private fun getCurrentTimeString(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.date} ${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
    }

    /**
     * 计算过期时间
     */
    private fun calculateExpireTime(hours: Int): String {
        val now = Clock.System.now()
        val expireInstant = now.plus(kotlin.time.Duration.parse("${hours}h"))
        val expireTime = expireInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${expireTime.date} ${expireTime.hour.toString().padStart(2, '0')}:${expireTime.minute.toString().padStart(2, '0')}:${expireTime.second.toString().padStart(2, '0')}"
    }
}
