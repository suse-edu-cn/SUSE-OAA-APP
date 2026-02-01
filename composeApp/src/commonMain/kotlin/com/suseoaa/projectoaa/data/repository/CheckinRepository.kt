package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.data.api.CheckinApiService
import com.suseoaa.projectoaa.data.model.*
import com.suseoaa.projectoaa.data.network.ClearableCookieStorage
import com.suseoaa.projectoaa.database.CourseDatabase
import com.suseoaa.projectoaa.util.CheckinRSAEncryptor
import io.ktor.client.call.*
import io.ktor.client.request.*
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
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
    private val json: Json,
    private val cookieStorage: ClearableCookieStorage
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
     * 添加打卡账号（密码登录）
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
                updatedAt = now,
                loginType = 0,
                sessionToken = null,
                sessionExpireTime = null,
                selectedLocation = CheckinLocations.DEFAULT.name
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 添加扫码登录账号
     */
    fun addQrCodeAccount(
        studentId: String,
        name: String,
        sessionToken: String,
        sessionExpireTime: String,
        selectedLocation: String = CheckinLocations.DEFAULT.name
    ): Result<Unit> {
        return try {
            val now = getCurrentTimeString()
            queries.insertQrCodeAccount(
                studentId = studentId,
                name = name,
                remark = "扫码登录",
                createdAt = now,
                updatedAt = now,
                sessionToken = sessionToken,
                sessionExpireTime = sessionExpireTime,
                selectedLocation = selectedLocation
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 更新打卡账号
     */
    fun updateAccount(
        id: Long, 
        studentId: String, 
        password: String, 
        name: String, 
        remark: String,
        selectedLocation: String = CheckinLocations.DEFAULT.name
    ): Result<Unit> {
        return try {
            val now = getCurrentTimeString()
            queries.update(
                studentId = studentId,
                password = password,
                name = name,
                remark = remark,
                updatedAt = now,
                selectedLocation = selectedLocation,
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
    
    /**
     * 更新账号 Session（扫码登录用）
     */
    fun updateSession(accountId: Long, sessionToken: String, sessionExpireTime: String): Result<Unit> {
        return try {
            val now = getCurrentTimeString()
            queries.updateSession(
                sessionToken = sessionToken,
                sessionExpireTime = sessionExpireTime,
                updatedAt = now,
                id = accountId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 清除账号 Session
     */
    fun clearSession(accountId: Long): Result<Unit> {
        return try {
            val now = getCurrentTimeString()
            queries.clearSession(updatedAt = now, id = accountId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 更新签到地点
     */
    fun updateLocation(accountId: Long, locationName: String): Result<Unit> {
        return try {
            val now = getCurrentTimeString()
            queries.updateLocation(selectedLocation = locationName, updatedAt = now, id = accountId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 扫码登录相关操作 ====================
    
    /**
     * 获取微信扫码登录的 ClientId
     */
    suspend fun getWechatClientId(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getWechatClientId()
            if (response.status.value != 200) {
                return@withContext Result.failure(Exception("获取 ClientId 失败 (${response.status.value})"))
            }
            
            val responseText = response.bodyAsText()
            println("[Checkin] getWechatClientId 响应: $responseText")
            
            val clientIdResponse = json.decodeFromString<WechatClientIdResponse>(responseText)
            val clientIdValue = clientIdResponse.getClientIdValue()
            
            println("[Checkin] 解析结果: data.clientId=${clientIdResponse.data?.clientId}, 最终值=$clientIdValue")
            
            if (clientIdValue.isNullOrBlank()) {
                return@withContext Result.failure(Exception("ClientId 为空，响应: $responseText"))
            }
            
            Result.success(clientIdValue)
        } catch (e: Exception) {
            println("[Checkin] getWechatClientId 异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 获取微信扫码登录的二维码图片
     * @return base64 格式的二维码图片数据
     */
    suspend fun getWechatQrCodeUrl(clientId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 使用正确的 appId（从 HAR 文件获取）
            val appId = "wx130c9f0196e29149"
            
            val response = api.getWechatQrCodeUrl(appId, clientId)
            val responseText = response.bodyAsText()
            
            println("[Checkin] getWechatQrCodeUrl 响应: ${responseText.take(500)}...")
            
            if (response.status.value != 200) {
                return@withContext Result.failure(Exception("获取二维码失败 (${response.status.value})"))
            }
            
            val qrCodeResponse = json.decodeFromString<WechatQrCodeResponse>(responseText)
            
            println("[Checkin] 二维码数据: img长度=${qrCodeResponse.data?.img?.length}, url=${qrCodeResponse.data?.url}")
            
            val qrCodeImage = qrCodeResponse.data?.getQrCodeImage()
            if (qrCodeImage.isNullOrBlank()) {
                return@withContext Result.failure(Exception("二维码图片为空"))
            }
            
            Result.success(qrCodeImage)
        } catch (e: Exception) {
            println("[Checkin] getWechatQrCodeUrl 异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 检查微信扫码状态
     * @return ScanStatus: WAITING(0), SCANNED(1), CONFIRMED(2), ERROR(-1)
     */
    suspend fun checkWechatScanStatus(clientId: String): Result<WechatScanStatusData> = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] checkWechatScanStatus 请求: clientId=$clientId")
            val response = api.checkWechatScanStatus(clientId)
            val responseText = response.bodyAsText()
            println("[Checkin] checkWechatScanStatus 响应: $responseText")
            
            if (response.status.value != 200) {
                return@withContext Result.failure(Exception("检查扫码状态失败 (${response.status.value})"))
            }
            
            val scanResponse = json.decodeFromString<WechatScanStatusResponse>(responseText)
            val data = scanResponse.data
            if (data == null) {
                return@withContext Result.failure(Exception("扫码状态数据为空"))
            }
            
            println("[Checkin] 扫码状态: status=${data.status}, callbackUrl=${data.callbackUrl}")
            Result.success(data)
        } catch (e: Exception) {
            println("[Checkin] checkWechatScanStatus 异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 获取 edu 用户信息
     */
    suspend fun getEduUserInfo(): Result<EduUserInfo> = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] getEduUserInfo 请求")
            val response = api.getEduUserInfo()
            val responseText = response.bodyAsText()
            println("[Checkin] getEduUserInfo 响应: ${responseText.take(500)}")
            
            if (response.status.value != 200) {
                return@withContext Result.failure(Exception("获取用户信息失败 (${response.status.value})"))
            }
            
            val userInfoResponse = json.decodeFromString<EduUserInfoResponse>(responseText)
            val userInfo = userInfoResponse.data
            if (userInfo == null) {
                return@withContext Result.failure(Exception("用户信息为空"))
            }
            
            println("[Checkin] 用户信息: code=${userInfo.code}, name=${userInfo.name}")
            Result.success(userInfo)
        } catch (e: Exception) {
            println("[Checkin] getEduUserInfo 异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 使用 Cookie 获取 edu 用户信息
     * 用于 WebView 扫码登录后获取用户信息
     */
    suspend fun getEduUserInfoWithCookies(cookies: String): Result<EduUserInfo> = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] getEduUserInfoWithCookies 请求, cookies=$cookies")
            val response = api.getEduUserInfoWithCookies(cookies)
            val responseText = response.bodyAsText()
            println("[Checkin] getEduUserInfoWithCookies 响应: ${responseText.take(500)}")
            
            if (response.status.value != 200) {
                return@withContext Result.failure(Exception("获取用户信息失败 (${response.status.value})"))
            }
            
            val userInfoResponse = json.decodeFromString<EduUserInfoResponse>(responseText)
            if (userInfoResponse.code != 200) {
                return@withContext Result.failure(Exception("获取用户信息失败: ${userInfoResponse.msg}"))
            }
            
            val userInfo = userInfoResponse.data
            if (userInfo == null) {
                return@withContext Result.failure(Exception("用户信息为空"))
            }
            
            println("[Checkin] 用户信息: code=${userInfo.code}, name=${userInfo.name}")
            Result.success(userInfo)
        } catch (e: Exception) {
            println("[Checkin] getEduUserInfoWithCookies 异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 使用 Cookie 获取用户组身份 (groupCode)
     * 用于签到时确定用户组
     */
    suspend fun getGroupIdentityWithCookies(cookies: String, openId: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] getGroupIdentityWithCookies 请求, openId=$openId")
            
            // 如果没有传入 openId，尝试从 cookies 中的 _sop_session_ 提取
            val effectiveOpenId = openId ?: extractSopSessionValue(cookies)?.let { extractOpenIdFromSopSession(it) }
            println("[Checkin] 使用的 openId: $effectiveOpenId")
            
            val response = api.getGroupIdentityWithCookies(cookies, effectiveOpenId)
            val responseText = response.bodyAsText()
            println("[Checkin] getGroupIdentityWithCookies 响应: $responseText")
            
            if (response.status.value != 200) {
                return@withContext Result.failure(Exception("获取用户组失败 (${response.status.value})"))
            }
            
            // 解析响应，支持两种格式：
            // 格式1: {"result":{"data":"_sudy2_XDH6iuWnV4=","total":1},...}  - data 是字符串
            // 格式2: {"result":{"data":[{"code":"xxx"}],"total":1},...}  - data 是数组
            val jsonObj = json.parseToJsonElement(responseText).jsonObject
            
            // 检查 resultCode 和 success
            val resultCode = try { jsonObj["resultCode"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1 } catch (e: Exception) { -1 }
            val success = try { jsonObj["success"]?.jsonPrimitive?.content?.toBoolean() ?: false } catch (e: Exception) { false }
            if (resultCode != 0 || !success) {
                val errorMsg = try { jsonObj["errorMsg"]?.jsonPrimitive?.content } catch (e: Exception) { null } ?: "未知错误"
                println("[Checkin] API 返回失败: resultCode=$resultCode, success=$success, errorMsg=$errorMsg")
                return@withContext Result.failure(Exception("获取用户组失败: $errorMsg"))
            }
            
            val resultObj = jsonObj["result"]?.jsonObject
            val dataElement = resultObj?.get("data")
            
            val groupCode: String? = when {
                // 格式1: data 是字符串
                dataElement is JsonPrimitive -> {
                    try { dataElement.content } catch (e: Exception) { null }
                }
                // 格式2: data 是数组，取第一个元素的 code
                dataElement is JsonArray -> {
                    try {
                        dataElement.firstOrNull()?.jsonObject?.get("code")?.jsonPrimitive?.content
                    } catch (e: Exception) { null }
                }
                else -> {
                    println("[Checkin] 无法解析 data 字段，类型未知: $dataElement")
                    null
                }
            }
            
            if (groupCode.isNullOrBlank()) {
                println("[Checkin] 用户组编码为空，响应内容: $responseText")
                return@withContext Result.failure(Exception("用户组编码为空"))
            }
            
            println("[Checkin] 用户组编码: $groupCode")
            Result.success(groupCode)
        } catch (e: Exception) {
            println("[Checkin] getGroupIdentityWithCookies 异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 从 _sop_session_ JWT 中提取 ticket
     * JWT 格式: header.payload.signature
     * payload 解码后包含 ticket 字段
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun extractTicketFromSopSession(sopSessionValue: String): String? {
        try {
            // JWT 格式: header.payload.signature
            val parts = sopSessionValue.split(".")
            if (parts.size != 3) {
                println("[Checkin] _sop_session_ 不是有效的 JWT 格式")
                return null
            }
            
            // 解码 payload (第二部分)
            val payload = parts[1]
            // Base64 URL 安全解码
            val paddedPayload = when (payload.length % 4) {
                2 -> payload + "=="
                3 -> payload + "="
                else -> payload
            }
            val decodedPayload = Base64.UrlSafe.decode(paddedPayload).decodeToString()
            println("[Checkin] JWT payload: $decodedPayload")
            
            // 解析 JSON 提取 ticket
            val payloadJson = json.parseToJsonElement(decodedPayload).jsonObject
            val ticket = payloadJson["ticket"]?.jsonPrimitive?.content
            println("[Checkin] 提取到 ticket: $ticket")
            return ticket
        } catch (e: Exception) {
            println("[Checkin] 解析 _sop_session_ 失败: ${e.message}")
            return null
        }
    }
    
    /**
     * 从 _sop_session_ JWT 中提取 openId (微信 OpenID)
     * 这个 openId 需要作为 open_id 参数传递给 /site/ API
     * 
     * JWT payload 结构:
     * {
     *   "uid": "23341010304",
     *   "ticket": "xxx",
     *   "extra": "{\"groupName\":\"\",\"identityType\":1,\"openId\":\"oXL_x6lMwe35D-T6qoiRM8_SErJA\",\"ybClientId\":\"xxx\"}"
     * }
     * 注意: openId 在 extra 字段内，extra 是嵌套的 JSON 字符串
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun extractOpenIdFromSopSession(sopSessionValue: String): String? {
        try {
            val parts = sopSessionValue.split(".")
            if (parts.size != 3) {
                println("[Checkin] JWT 格式错误，parts.size=${parts.size}")
                return null
            }
            
            val payload = parts[1]
            val paddedPayload = when (payload.length % 4) {
                2 -> payload + "=="
                3 -> payload + "="
                else -> payload
            }
            val decodedPayload = Base64.UrlSafe.decode(paddedPayload).decodeToString()
            println("[Checkin] JWT payload: $decodedPayload")
            
            val payloadJson = json.parseToJsonElement(decodedPayload).jsonObject
            
            // openId 在 extra 字段内，extra 是一个 JSON 字符串
            val extraString = payloadJson["extra"]?.jsonPrimitive?.content
            if (extraString.isNullOrBlank()) {
                println("[Checkin] extra 字段为空")
                return null
            }
            
            // 解析 extra JSON 字符串
            val extraJson = json.parseToJsonElement(extraString).jsonObject
            val openId = extraJson["openId"]?.jsonPrimitive?.content
            
            println("[Checkin] 从 extra 中提取到 openId: $openId")
            return openId
        } catch (e: Exception) {
            println("[Checkin] 提取 openId 失败: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 从 _sop_session_ JWT 中提取用户信息（学号和姓名）
     * JWT payload 结构:
     * {
     *   "uid": "23341010304",  // 学号
     *   "ticket": "xxx",
     *   "extra": "{\"userName\":\"张三\",\"openId\":\"xxx\",...}"
     * }
     * 返回 Pair<studentId, name>
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun extractUserInfoFromSopSession(sopSessionValue: String): Pair<String, String>? {
        try {
            val parts = sopSessionValue.split(".")
            if (parts.size != 3) {
                println("[Checkin] JWT 格式错误，parts.size=${parts.size}")
                return null
            }
            
            val payload = parts[1]
            val paddedPayload = when (payload.length % 4) {
                2 -> payload + "=="
                3 -> payload + "="
                else -> payload
            }
            val decodedPayload = Base64.UrlSafe.decode(paddedPayload).decodeToString()
            println("[Checkin] extractUserInfo JWT payload: $decodedPayload")
            
            val payloadJson = json.parseToJsonElement(decodedPayload).jsonObject
            
            // 提取学号 (uid)
            val uid = payloadJson["uid"]?.jsonPrimitive?.content
            if (uid.isNullOrBlank()) {
                println("[Checkin] uid 字段为空")
                return null
            }
            
            // 尝试从 extra 中提取姓名
            var userName = ""
            val extraString = payloadJson["extra"]?.jsonPrimitive?.content
            if (!extraString.isNullOrBlank()) {
                try {
                    val extraJson = json.parseToJsonElement(extraString).jsonObject
                    userName = extraJson["userName"]?.jsonPrimitive?.content ?: ""
                } catch (e: Exception) {
                    println("[Checkin] 解析 extra 获取 userName 失败: ${e.message}")
                }
            }
            
            println("[Checkin] 从 JWT 提取到用户信息: uid=$uid, userName=$userName")
            return Pair(uid, userName)
        } catch (e: Exception) {
            println("[Checkin] 提取用户信息失败: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 从 Cookie 字符串中提取 _sop_session_ 的值
     */
    private fun extractSopSessionValue(cookies: String): String? {
        return cookies.split(";")
            .map { it.trim() }
            .find { it.startsWith("_sop_session_=") }
            ?.substringAfter("_sop_session_=")
    }
    
    /**
     * 使用 _sop_session_ Cookie 完成 SSO 认证
     * 尝试多种方法获取 SESSION cookie：
     * 1. 访问 /xg/ 页面触发 SESSION 生成
     * 2. 调用 SSO 接口
     * 3. 调用 /site/user/current API
     */
    suspend fun completeSsoWithSopSession(sopSessionCookie: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] completeSsoWithSopSession 开始")
            
            // 提取 _sop_session_ 的值
            val sopSessionValue = sopSessionCookie.split(";")
                .map { it.trim() }
                .find { it.startsWith("_sop_session_=") }
                ?.substringAfter("_sop_session_=")
            
            if (sopSessionValue.isNullOrBlank()) {
                return@withContext Result.failure(Exception("未找到 _sop_session_ Cookie"))
            }
            
            // 从 JWT 中提取 openId 和 ticket
            val openId = extractOpenIdFromSopSession(sopSessionValue)
            val ticket = extractTicketFromSopSession(sopSessionValue)
            println("[Checkin] 提取到 openId: $openId, ticket: $ticket")
            
            var sessionValue: String? = null
            
            // 方法1: 访问 /xg/ 页面触发 SESSION 生成
            if (openId != null) {
                println("[Checkin] 方法1: 访问 /xg/ 页面...")
                try {
                    var currentCookies = sopSessionCookie
                    var response = api.accessXgPage(currentCookies, openId)
                    println("[Checkin] /xg/ 页面响应状态: ${response.status.value}")
                    
                    // 收集所有响应的 Set-Cookie
                    val allSetCookies = mutableListOf<String>()
                    response.headers.getAll("Set-Cookie")?.let { allSetCookies.addAll(it) }
                    
                    // 手动跟随重定向（最多 5 次）
                    var redirectCount = 0
                    while (response.status.value in 301..303 && redirectCount < 5) {
                        val location = response.headers["Location"]
                        if (location.isNullOrBlank()) break
                        
                        // 从 Set-Cookie 提取新的 cookie
                        response.headers.getAll("Set-Cookie")?.forEach { setCookie ->
                            allSetCookies.add(setCookie)
                            // 提取 cookie 名=值
                            val cookiePair = setCookie.substringBefore(";").trim()
                            if (!currentCookies.contains(cookiePair.substringBefore("="))) {
                                currentCookies = "$currentCookies; $cookiePair"
                            }
                        }
                        
                        println("[Checkin] 重定向 ${redirectCount + 1}: $location")
                        
                        // 跟随重定向
                        response = api.httpClient.get(location) {
                            header("Cookie", currentCookies)
                            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        }
                        response.headers.getAll("Set-Cookie")?.let { allSetCookies.addAll(it) }
                        redirectCount++
                    }
                    
                    println("[Checkin] 收集到的 Set-Cookie: ${allSetCookies.size} 个")
                    
                    // 从所有 Set-Cookie 中查找 SESSION
                    sessionValue = allSetCookies.find { it.contains("SESSION=") }
                        ?.let { cookie ->
                            val match = Regex("SESSION=([^;]+)").find(cookie)
                            match?.groupValues?.get(1)
                        }
                    
                    if (sessionValue == null) {
                        // 从 cookie storage 获取
                        val storageCookies = cookieStorage.getCookiesForHost("qfhy.suse.edu.cn")
                        sessionValue = storageCookies.find { it.name == "SESSION" }?.value
                        println("[Checkin] 从 cookie storage 获取 SESSION: ${sessionValue?.take(20)}...")
                    }
                    
                    println("[Checkin] 方法1 结果: SESSION=${sessionValue?.take(20)}...")
                } catch (e: Exception) {
                    println("[Checkin] 访问 /xg/ 页面异常: ${e.message}")
                }
            }
            
            // 方法2: 调用 SSO 接口
            if (sessionValue.isNullOrBlank() && ticket != null) {
                println("[Checkin] 方法2: 调用 SSO 接口...")
                try {
                    val ssoResponse = api.completeSsoWithTicket(ticket, sopSessionCookie)
                    println("[Checkin] SSO 响应状态: ${ssoResponse.status.value}")
                    
                    // 打印所有响应头
                    println("[Checkin] SSO 响应头:")
                    ssoResponse.headers.forEach { name, values ->
                        values.forEach { value ->
                            println("  $name: ${value.take(100)}")
                        }
                    }
                    
                    // 打印响应体（可能是重定向或包含 session 信息）
                    val ssoBody = ssoResponse.bodyAsText()
                    println("[Checkin] SSO 响应体: ${ssoBody.take(300)}")
                    
                    val ssoSetCookies = ssoResponse.headers.getAll("Set-Cookie")
                    
                    sessionValue = ssoSetCookies?.find { it.contains("SESSION=") }
                        ?.let { cookie ->
                            val match = Regex("SESSION=([^;]+)").find(cookie)
                            match?.groupValues?.get(1)
                        }
                    
                    if (sessionValue == null) {
                        val storageCookies = cookieStorage.getCookiesForHost("qfhy.suse.edu.cn")
                        sessionValue = storageCookies.find { it.name == "SESSION" }?.value
                        println("[Checkin] 从 cookie storage 获取 SESSION: ${sessionValue?.take(20)}...")
                    }
                } catch (e: Exception) {
                    println("[Checkin] SSO 接口异常: ${e.message}")
                }
            }
            
            // 方法3: 调用 /site/user/current API
            if (sessionValue.isNullOrBlank() && openId != null) {
                println("[Checkin] 方法3: 调用 /site/user/current API...")
                try {
                    val userResponse = api.initSiteSession(sopSessionCookie, openId)
                    println("[Checkin] user/current 响应状态: ${userResponse.status.value}")
                    
                    val userSetCookies = userResponse.headers.getAll("Set-Cookie")
                    println("[Checkin] user/current Set-Cookie: $userSetCookies")
                    
                    sessionValue = userSetCookies?.find { it.contains("SESSION=") }
                        ?.let { cookie ->
                            val match = Regex("SESSION=([^;]+)").find(cookie)
                            match?.groupValues?.get(1)
                        }
                    
                    if (sessionValue == null) {
                        val storageCookies = cookieStorage.getCookiesForHost("qfhy.suse.edu.cn")
                        sessionValue = storageCookies.find { it.name == "SESSION" }?.value
                        println("[Checkin] 从 cookie storage 获取 SESSION: ${sessionValue?.take(20)}...")
                    }
                } catch (e: Exception) {
                    println("[Checkin] user/current API 异常: ${e.message}")
                }
            }
            
            // 最终检查 cookie storage
            if (sessionValue.isNullOrBlank()) {
                println("[Checkin] 所有方法都未能获取 SESSION，打印 cookie storage 调试信息:")
                cookieStorage.debugPrintAllCookies()
                
                val storageCookies = cookieStorage.getCookiesForHost("qfhy.suse.edu.cn")
                sessionValue = storageCookies.find { it.name == "SESSION" }?.value
            }
            
            if (sessionValue.isNullOrBlank()) {
                return@withContext Result.failure(Exception("所有方法都未能获取 SESSION Cookie"))
            }
            
            // 组合完整的 cookie 字符串
            val fullCookies = "$sopSessionCookie; SESSION=$sessionValue"
            println("[Checkin] SSO 认证成功，完整 Cookie: ${fullCookies.take(100)}...")
            Result.success(fullCookies)
        } catch (e: Exception) {
            println("[Checkin] completeSsoWithSopSession 异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 使用 Cookie 执行签到
     * 用于 WebView 扫码登录后直接签到
     * 
     * 重要：/site/ 系统的 API 需要 SESSION cookie，不是 _sop_session_
     * 需要先通过 SSO 接口使用 ticket 获取 SESSION cookie
     */
    suspend fun performCheckinWithCookies(cookies: String, account: CheckinAccountData): CheckinResult = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] performCheckinWithCookies 开始, studentId=${account.studentId}")
            
            // 提取 openId（用于 Referer 参数）
            val openId = extractSopSessionValue(cookies)?.let { extractOpenIdFromSopSession(it) }
            println("[Checkin] 提取到的 openId: $openId")
            
            // 如果没有 openId，这是一个严重问题，很可能会导致后续请求失败
            if (openId.isNullOrBlank()) {
                println("[Checkin] 警告: 无法从 _sop_session_ 提取 openId，这可能导致认证失败")
            }
            
            // 首先检查是否已有 SESSION cookie
            val hasSession = cookies.contains("SESSION=")
            var effectiveCookies = cookies
            
            if (!hasSession) {
                // 需要通过 SSO 获取 SESSION cookie
                // /site/ 系统的 API 需要 SESSION，而不是 _sop_session_
                println("[Checkin] 没有 SESSION cookie，尝试通过 SSO 获取...")
                val ssoResult = completeSsoWithSopSession(cookies)
                if (ssoResult.isSuccess) {
                    effectiveCookies = ssoResult.getOrThrow()
                    println("[Checkin] SSO 成功，获取到完整 cookies: ${effectiveCookies.take(100)}...")
                } else {
                    val ssoError = ssoResult.exceptionOrNull()?.message
                    println("[Checkin] SSO 失败: $ssoError")
                    // 仍然尝试继续，但记录更详细的信息
                    println("[Checkin] 继续尝试使用原始 cookies (可能会失败)")
                }
            } else {
                println("[Checkin] 已有 SESSION cookie")
            }
            
            // 打印最终使用的 cookie（脱敏）
            println("[Checkin] 最终 cookies 包含 SESSION: ${effectiveCookies.contains("SESSION=")}")
            println("[Checkin] 最终 cookies 包含 _sop_session_: ${effectiveCookies.contains("_sop_session_=")}")
            
            // 1. 获取用户组编码（传入 openId 用于 Referer）
            val groupCodeResult = getGroupIdentityWithCookies(effectiveCookies, openId)
            if (groupCodeResult.isFailure) {
                return@withContext CheckinResult.Failed("获取用户组失败: ${groupCodeResult.exceptionOrNull()?.message}")
            }
            val groupCode = groupCodeResult.getOrThrow()
            
            // 2. 获取待签到任务（使用 effectiveCookies，传入 openId）
            val tasksResponse = api.getTaskListWithCookies(1, effectiveCookies, openId)
            if (tasksResponse.status.value != 200) {
                return@withContext CheckinResult.Failed("获取任务列表失败")
            }
            
            val taskListResponse = json.decodeFromString<CheckinTaskListResponse>(tasksResponse.bodyAsText())
            if (taskListResponse.resultCode != 0) {
                return@withContext CheckinResult.Failed("获取任务列表失败: ${taskListResponse.errorMsg}")
            }
            
            val pendingTasks = taskListResponse.result?.data ?: emptyList()
            if (pendingTasks.isEmpty()) {
                updateCheckinStatus(account.id, "无任务")
                return@withContext CheckinResult.NoTask("当前没有待签到的任务")
            }
            
            // 3. 执行签到（传入 openId 用于 Referer）
            val submitResponse = api.submitCheckinWithCookies(groupCode, effectiveCookies, openId)
            val submitText = submitResponse.bodyAsText()
            println("[Checkin] 签到响应: $submitText")
            
            val submitResult = json.decodeFromString<CheckinSubmitResponse>(submitText)
            
            return@withContext if (submitResult.success && submitResult.resultCode == 0) {
                updateCheckinStatus(account.id, "成功")
                CheckinResult.Success("签到成功")
            } else {
                val errorMsg = submitResult.errorMsg ?: "签到失败"
                updateCheckinStatus(account.id, "失败: $errorMsg")
                CheckinResult.Failed(errorMsg)
            }
        } catch (e: Exception) {
            println("[Checkin] performCheckinWithCookies 异常: ${e.message}")
            updateCheckinStatus(account.id, "异常: ${e.message}")
            CheckinResult.Failed(e.message ?: "未知错误")
        }
    }
    
    /**
     * 处理微信回调 URL，获取 Session
     */
    suspend fun handleWechatCallback(callbackUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] handleWechatCallback 请求: $callbackUrl")
            val response = api.handleWechatCallback(callbackUrl)
            println("[Checkin] handleWechatCallback 响应状态: ${response.status}")
            
            // 从 Set-Cookie 响应头中提取 SESSION
            val cookies = response.headers.getAll("Set-Cookie")
            println("[Checkin] Cookies: $cookies")
            
            val sessionToken = cookies?.firstOrNull { it.startsWith("SESSION=") }
                ?.substringAfter("SESSION=")
                ?.substringBefore(";")
            
            if (sessionToken.isNullOrBlank()) {
                // 如果没有 Cookie，尝试从当前 HttpClient 的 Cookie 存储中获取
                return@withContext Result.failure(Exception("未能获取 Session Token"))
            }
            
            println("[Checkin] 获取到 Session: ${sessionToken.take(20)}...")
            Result.success(sessionToken)
        } catch (e: Exception) {
            println("[Checkin] handleWechatCallback 异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    // ==================== 基于位置的签到（扫码登录账号使用）====================
    
    /**
     * 使用 Session 执行签到（扫码登录账号）
     */
    suspend fun performCheckinWithSession(account: CheckinAccountData): CheckinResult = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] performCheckinWithSession 开始, studentId=${account.studentId}, loginType=${account.loginType}")
            
            val sessionToken = account.sessionToken
            println("[Checkin] sessionToken: ${sessionToken?.take(50)}...")
            
            if (sessionToken.isNullOrBlank()) {
                println("[Checkin] sessionToken 为空!")
                return@withContext CheckinResult.Failed("Session 为空，请重新扫码登录")
            }
            
            // 检查 Session 是否过期
            if (!account.isSessionValid()) {
                println("[Checkin] Session 已过期, expireTime=${account.sessionExpireTime}")
                return@withContext CheckinResult.Failed("Session 已过期，请重新扫码登录")
            }
            
            // 判断 sessionToken 是完整的 Cookie 字符串还是单独的 SESSION 值
            // 完整的 Cookie 字符串会包含 "=" 且可能有多个 Cookie（用 ";" 分隔）
            val cookies = if (sessionToken.contains(";") || sessionToken.contains("=")) {
                // 完整的 Cookie 字符串
                println("[Checkin] 使用完整 Cookie 字符串")
                sessionToken
            } else {
                // 单独的 SESSION 值
                println("[Checkin] 使用单独 SESSION 值")
                "SESSION=$sessionToken"
            }
            
            // 使用 Cookie 执行签到
            return@withContext performCheckinWithCookies(cookies, account)
        } catch (e: Exception) {
            println("[Checkin] performCheckinWithSession 异常: ${e.message}")
            e.printStackTrace()
            updateCheckinStatus(account.id, "异常: ${e.message}")
            CheckinResult.Failed(e.message ?: "未知错误")
        }
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
            
            // 0. 清除旧的 Cookie，确保每次登录都是新会话
            cookieStorage.clear()
            
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
            println("[Checkin] getUserGroups 响应状态: ${response.status.value}")
            
            // 302 表示登录状态失效
            if (response.status.value == 302) {
                println("[Checkin] 登录状态失效 (302 重定向)")
                return null
            }
            if (response.status.value != 200) {
                println("[Checkin] getUserGroups 非200响应: ${response.status.value}")
                return null
            }
            
            val responseText = response.bodyAsText()
            println("[Checkin] getUserGroups 响应: ${responseText.take(200)}")
            
            val groupsResponse = json.decodeFromString<UserGroupsResponse>(responseText)
            if (groupsResponse.resultCode != 0 || !groupsResponse.success) return null
            
            // 返回第一个组的编码
            return groupsResponse.result?.data?.firstOrNull()?.code
        } catch (e: Exception) {
            println("[Checkin] getUserGroupCode 异常: ${e.message}")
            return null
        }
    }
    
    /**
     * 获取待签任务列表（未打卡）
     * @param cookies 完整的Cookie字符串
     */
    suspend fun getPendingTasksWithCookies(cookies: String): Result<List<CheckinTask>> = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] getPendingTasksWithCookies 请求")
            val response = api.getTaskListWithCookies(cookies, status = 1)
            val responseText = response.bodyAsText()
            println("[Checkin] getPendingTasksWithCookies 响应: $responseText")
            
            val taskListResponse = json.decodeFromString<CheckinTaskListResponse>(responseText)
            if (taskListResponse.success && taskListResponse.resultCode == 0) {
                val tasks = taskListResponse.result?.data ?: emptyList()
                println("[Checkin] 获取到 ${tasks.size} 个未打卡任务")
                Result.success(tasks)
            } else {
                println("[Checkin] 获取未打卡任务失败: ${taskListResponse.errorMsg}")
                Result.failure(Exception(taskListResponse.errorMsg ?: "获取任务列表失败"))
            }
        } catch (e: Exception) {
            println("[Checkin] getPendingTasksWithCookies 异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 获取已完成任务列表（已打卡）
     * @param cookies 完整的Cookie字符串
     */
    suspend fun getCompletedTasksWithCookies(cookies: String): Result<List<CheckinTask>> = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] getCompletedTasksWithCookies 请求")
            val response = api.getCompletedTasksWithCookies(cookies)
            val responseText = response.bodyAsText()
            println("[Checkin] getCompletedTasksWithCookies 响应: $responseText")
            
            val taskListResponse = json.decodeFromString<CheckinTaskListResponse>(responseText)
            if (taskListResponse.success && taskListResponse.resultCode == 0) {
                val tasks = taskListResponse.result?.data ?: emptyList()
                println("[Checkin] 获取到 ${tasks.size} 个已打卡任务")
                Result.success(tasks)
            } else {
                println("[Checkin] 获取已打卡任务失败: ${taskListResponse.errorMsg}")
                Result.failure(Exception(taskListResponse.errorMsg ?: "获取任务列表失败"))
            }
        } catch (e: Exception) {
            println("[Checkin] getCompletedTasksWithCookies 异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 获取缺勤任务列表（未打卡的过期任务）
     * @param cookies 完整的Cookie字符串
     */
    suspend fun getAbsentTasksWithCookies(cookies: String): Result<List<CheckinTask>> = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] getAbsentTasksWithCookies 请求")
            val response = api.getAbsentTasksWithCookies(cookies)
            val responseText = response.bodyAsText()
            println("[Checkin] getAbsentTasksWithCookies 响应: $responseText")
            
            val taskListResponse = json.decodeFromString<CheckinTaskListResponse>(responseText)
            if (taskListResponse.success && taskListResponse.resultCode == 0) {
                val tasks = taskListResponse.result?.data ?: emptyList()
                println("[Checkin] 获取到 ${tasks.size} 个缺勤任务")
                Result.success(tasks)
            } else {
                println("[Checkin] 获取缺勤任务失败: ${taskListResponse.errorMsg}")
                Result.failure(Exception(taskListResponse.errorMsg ?: "获取任务列表失败"))
            }
        } catch (e: Exception) {
            println("[Checkin] getAbsentTasksWithCookies 异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 对特定任务进行打卡（支持重复打卡）
     * @param cookies 完整的Cookie字符串
     * @param taskId 任务ID
     * @param account 账号信息
     * @return 打卡结果
     */
    suspend fun checkinForSpecificTask(
        cookies: String,
        taskId: Long,
        account: CheckinAccountData
    ): CheckinResult = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] checkinForSpecificTask 开始, taskId=$taskId")
            
            // 1. 获取任务详情，获取当前的签到ID
            val detailResponse = api.getTaskDetailWithSession(cookies, taskId)
            if (detailResponse.status.value != 200) {
                return@withContext CheckinResult.Failed("获取任务详情失败")
            }
            
            val detailText = detailResponse.bodyAsText()
            println("[Checkin] 任务详情响应: ${detailText.take(200)}")
            
            val detailResult = json.decodeFromString<CheckinDetailResponse>(detailText)
            if (!detailResult.success || detailResult.resultCode != 0) {
                return@withContext CheckinResult.Failed("获取任务详情失败: ${detailResult.errorMsg}")
            }
            
            val dkxx = detailResult.result?.data?.dkxx
            if (dkxx == null) {
                return@withContext CheckinResult.Failed("任务详情数据为空")
            }
            
            val currentSignId = dkxx.id
            println("[Checkin] 当前签到ID: $currentSignId, 状态: ${dkxx.qdzt}")
            
            // 2. 构造签到数据（参考Python脚本）
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val qdsj = "${now.date} ${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
            
            // 使用账号中配置的签到地点
            val selectedLocation = CheckinLocations.ALL.find { it.name == account.selectedLocation } 
                ?: CheckinLocations.DEFAULT
            
            val signData = buildJsonObject {
                put("id", currentSignId)
                put("qdzt", 1)  // 签到状态：1=已签到
                put("qdsj", qdsj)  // 签到时间
                put("isOuted", 0)  // 是否超出范围
                put("isLated", 0)  // 是否迟到
                put("dkddPhoto", "")  // 打卡地点照片
                put("qdddjtdz", selectedLocation.address)  // 签到地点具体地址
                put("location", selectedLocation.locationJson)  // 位置信息JSON
                put("txxx", "{}")  // 图像信息
            }
            
            println("[Checkin] 签到数据: $signData")
            
            // 3. 提交签到请求
            val submitResponse = api.submitLocationCheckin(cookies, signData.toString())
            val submitText = submitResponse.bodyAsText()
            println("[Checkin] 签到响应: $submitText")
            
            val submitResult = json.decodeFromString<CheckinSubmitResponse>(submitText)
            
            return@withContext if (submitResult.success && submitResult.resultCode == 0) {
                updateCheckinStatus(account.id, "成功")
                if (dkxx.qdzt == 1) {
                    CheckinResult.Success("再次签到成功")
                } else {
                    CheckinResult.Success("签到成功")
                }
            } else {
                val errorMsg = submitResult.errorMsg ?: "签到失败"
                updateCheckinStatus(account.id, "失败: $errorMsg")
                CheckinResult.Failed(errorMsg)
            }
        } catch (e: Exception) {
            println("[Checkin] checkinForSpecificTask 异常: ${e.message}")
            e.printStackTrace()
            updateCheckinStatus(account.id, "异常: ${e.message}")
            CheckinResult.Failed(e.message ?: "未知错误")
        }
    }
    
    /**
     * 获取所有任务（使用当前cookieStorage中的cookies）
     * @return Triple<待打卡任务列表, 已打卡任务列表, 缺勤任务列表>
     */
    suspend fun getAllTasks(): Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>> {
        val pendingTasks = getPendingTasksWithCookies("").getOrElse { emptyList() }
        val completedTasks = getCompletedTasksWithCookies("").getOrElse { emptyList() }
        val absentTasks = getAbsentTasksWithCookies("").getOrElse { emptyList() }
        return Triple(pendingTasks, completedTasks, absentTasks)
    }
    
    /**
     * 获取所有任务（包括未打卡和已打卡）
     * @param cookies 完整的Cookie字符串
     * @return Pair<未打卡任务列表, 已打卡任务列表>
     */
    suspend fun getAllTasksWithCookies(cookies: String): Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>> {
        val pendingTasks = getPendingTasksWithCookies(cookies).getOrElse { emptyList() }
        val completedTasks = getCompletedTasksWithCookies(cookies).getOrElse { emptyList() }
        val absentTasks = getAbsentTasksWithCookies(cookies).getOrElse { emptyList() }
        return Triple(pendingTasks, completedTasks, absentTasks)
    }
    
    /**
     * 获取待签任务列表（内部使用，保持兼容）
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
    
    /**
     * 简化的获取微信二维码URL方法
     * 用于新的二维码对话框
     */
    suspend fun getQrCodeForDialog(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. 获取clientId
            val clientIdResult = getWechatClientId()
            if (clientIdResult.isFailure) {
                return@withContext Result.failure(clientIdResult.exceptionOrNull() ?: Exception("获取ClientId失败"))
            }
            val clientId = clientIdResult.getOrNull() ?: return@withContext Result.failure(Exception("ClientId为空"))
            
            // 2. 获取二维码URL
            val qrCodeResult = getWechatQrCodeUrl(clientId)
            if (qrCodeResult.isFailure) {
                return@withContext Result.failure(qrCodeResult.exceptionOrNull() ?: Exception("获取二维码失败"))
            }
            
            qrCodeResult
        } catch (e: Exception) {
            println("[QrCode] 获取二维码失败: ${e.message}")
            Result.failure(e)
        }
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
        updatedAt = updatedAt,
        loginType = loginType.toInt(),
        sessionToken = sessionToken,
        sessionExpireTime = sessionExpireTime,
        selectedLocation = selectedLocation
    )
}
