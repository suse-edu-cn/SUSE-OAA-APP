package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.QrCodeCheckinApiService
import com.suseoaa.projectoaa.shared.data.remote.network.ClearableCookieStorage
import com.suseoaa.projectoaa.shared.data.repository.checkin.CheckinAccountStore
import com.suseoaa.projectoaa.shared.data.repository.checkin.CheckinClock
import com.suseoaa.projectoaa.shared.data.repository.checkin.CheckinCookies
import com.suseoaa.projectoaa.shared.data.repository.checkin.CheckinException
import com.suseoaa.projectoaa.shared.data.repository.checkin.CheckinTaskRepository
import com.suseoaa.projectoaa.shared.data.repository.checkin.QrCodeTaskGateway
import com.suseoaa.projectoaa.shared.data.repository.checkin.SopSessionParser
import com.suseoaa.projectoaa.shared.data.repository.checkin.SopSessionUser
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinLocations
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTask
import com.suseoaa.projectoaa.shared.domain.model.checkin.EduUserInfo
import com.suseoaa.projectoaa.shared.domain.model.checkin.EduUserInfoResponse
import com.suseoaa.projectoaa.shared.domain.model.checkin.WechatClientIdResponse
import com.suseoaa.projectoaa.shared.domain.model.checkin.WechatQrCodeResponse
import com.suseoaa.projectoaa.shared.domain.model.checkin.WechatScanStatusData
import com.suseoaa.projectoaa.shared.domain.model.checkin.WechatScanStatusResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * 扫码签到仓库 —— 负责微信扫码登录与会话维护。
 *
 * 完整流程：
 * 1. 获取二维码（[getClientId] + [getQrCodeImage]）
 * 2. 轮询扫码状态（[checkScanStatus]）
 * 3. 扫码成功后处理回调拿到 `_sop_session_`（[handleScanCallback]）
 * 4. ★关键★ 调用 SSO 接口用 `_sop_session_` 换取 SESSION Cookie（[getSessionCookie]）
 *    SESSION 不会因为访问页面自动产生，必须走 SSO 接口
 * 5. 保存账号（[saveQrCodeAccount]）
 * 6. 执行签到（[performCheckinWithSession]）
 *
 * 任务列表与签到提交本身与密码登录完全一致，统一委托给 [CheckinTaskRepository]，
 * 本类只负责把扫码会话包装成 [QrCodeTaskGateway]。
 */
class QrCodeCheckinRepository(
    private val api: QrCodeCheckinApiService,
    private val accountStore: CheckinAccountStore,
    private val taskRepository: CheckinTaskRepository,
    private val sopSessionParser: SopSessionParser,
    private val json: Json,
    private val cookieStorage: ClearableCookieStorage
) {

    // ==================== 扫码登录流程 ====================

    /** 步骤1: 获取微信扫码 ClientId */
    suspend fun getClientId(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            println("[QrCheckin] 获取 ClientId...")
            val response = api.getClientId()
            if (response.status.value != 200) {
                throw CheckinException("获取 ClientId 失败 (${response.status.value})")
            }
            val clientId = json
                .decodeFromString<WechatClientIdResponse>(response.bodyAsText())
                .getClientIdValue()
            if (clientId.isNullOrBlank()) throw CheckinException("ClientId 为空")
            println("[QrCheckin] ClientId: $clientId")
            clientId
        }.onFailure { println("[QrCheckin] 获取 ClientId 异常: ${it.message}") }
    }

    /** 步骤2: 获取微信二维码图片（Base64） */
    suspend fun getQrCodeImage(clientId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            println("[QrCheckin] 获取二维码, clientId=$clientId")
            val response = api.getQrCodeUrl(clientId)
            if (response.status.value != 200) {
                throw CheckinException("获取二维码失败 (${response.status.value})")
            }
            val image = json
                .decodeFromString<WechatQrCodeResponse>(response.bodyAsText())
                .data?.getQrCodeImage()
            if (image.isNullOrBlank()) throw CheckinException("二维码图片为空")
            println("[QrCheckin] 二维码图片长度: ${image.length}")
            image
        }.onFailure { println("[QrCheckin] 获取二维码异常: ${it.message}") }
    }

    /** 步骤3: 轮询扫码状态。status: 0=等待扫码, 1=已扫码待确认, 2=已确认 */
    suspend fun checkScanStatus(clientId: String): Result<WechatScanStatusData> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.checkScanStatus(clientId)
                if (response.status.value != 200) throw CheckinException("检查扫码状态失败")
                val responseText = response.bodyAsText()
                println("[QrCheckin] 扫码状态: $responseText")
                json.decodeFromString<WechatScanStatusResponse>(responseText).data
                    ?: throw CheckinException("扫码状态数据为空")
            }.onFailure { println("[QrCheckin] 检查扫码状态异常: ${it.message}") }
        }

    /** 步骤4: 处理扫码回调，拿到 `_sop_session_` Cookie */
    suspend fun handleScanCallback(callbackUrl: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                println("[QrCheckin] 处理回调: $callbackUrl")
                val response = api.handleCallback(callbackUrl)
                println("[QrCheckin] 回调响应状态: ${response.status.value}")

                val sopSession = CheckinCookies
                    .fromSetCookies(response.headers.getAll("Set-Cookie"), SOP_SESSION)
                    ?: cookieStorage.getCookiesForHost(QFHY_HOST)
                        .find { it.name == SOP_SESSION }?.value
                    ?: throw CheckinException("未获取到 _sop_session_ Cookie")

                println("[QrCheckin] _sop_session_ 长度: ${sopSession.length}")
                "$SOP_SESSION=$sopSession"
            }.onFailure { println("[QrCheckin] 处理回调异常: ${it.message}") }
        }

    /**
     * 步骤5: 用 `_sop_session_` 换取 SESSION Cookie。
     *
     * SESSION 必须通过专门的 SSO 接口获取（GET /site/appware/system/sso/loginUrl），
     * 访问普通页面不会自动产生 SESSION —— 这是本流程最容易踩坑的一步。
     *
     * @return 同时含 `_sop_session_` 与 `SESSION` 的完整 Cookie 字符串
     */
    suspend fun getSessionCookie(sopSessionCookie: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sopSessionValue = sopSessionCookie.substringAfter("$SOP_SESSION=")
                val openId = sopSessionParser.openIdOf(sopSessionValue)
                    ?: throw CheckinException("无法从 JWT 中提取 openId")
                println("[QrCheckin] openId: $openId")

                cookieStorage.clearForHost(QFHY_HOST)

                val ssoResponse = api.getSsoSession(sopSessionCookie, openId)
                println("[QrCheckin] SSO API 响应状态: ${ssoResponse.status.value}")

                val session = CheckinCookies
                    .fromSetCookies(ssoResponse.headers.getAll("Set-Cookie"), SESSION)
                    ?: cookieStorage.getCookiesForHost(QFHY_HOST)
                        .find { it.name == SESSION }?.value
                if (session.isNullOrBlank()) {
                    println("[QrCheckin] SSO API 未返回 SESSION，Location=${ssoResponse.headers["Location"]}")
                    cookieStorage.debugPrintAllCookies()
                    throw CheckinException("SSO API 未返回 SESSION Cookie")
                }

                println("[QrCheckin] 成功获取 SESSION: ${session.take(20)}...")
                "$sopSessionCookie; $SESSION=$session"
            }.onFailure { println("[QrCheckin] 获取 SESSION 异常: ${it.message}") }
        }

    /** 从任意 Cookie 字符串出发完成 SSO，得到含 SESSION 的完整 Cookie */
    suspend fun completeSsoWithSopSession(cookies: String): Result<String> =
        withContext(Dispatchers.IO) {
            val sopSession = sopSessionParser.sopSessionValueOf(cookies)
                ?: return@withContext Result.failure(CheckinException("未找到 _sop_session_"))
            getSessionCookie("$SOP_SESSION=$sopSession")
        }

    // ==================== 用户信息 ====================

    /** 从 `_sop_session_` JWT 中解析学号与姓名 */
    fun extractUserInfoFromSopSession(sopSession: String): SopSessionUser? =
        sopSessionParser.userInfoOf(sopSession)

    /** 用 Cookie 调接口获取用户信息，作为 JWT 解析失败时的兜底 */
    suspend fun getEduUserInfoWithCookies(cookies: String): Result<EduUserInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                println("[QrCheckin] 使用 Cookie 获取用户信息...")
                val response = api.getUserInfo(cookies)
                if (response.status.value != 200) {
                    throw CheckinException("获取用户信息失败 (${response.status.value})")
                }
                val body = json.decodeFromString<EduUserInfoResponse>(response.bodyAsText())
                if (body.code != 200) throw CheckinException(body.msg ?: "获取用户信息失败")
                body.data ?: throw CheckinException("用户信息为空")
            }.onFailure { println("[QrCheckin] 获取用户信息异常: ${it.message}") }
        }

    // ==================== 账号与会话 ====================

    /** 保存扫码账号，已存在则只刷新会话 */
    fun saveQrCodeAccount(
        studentId: String,
        name: String,
        sessionToken: String,
        sessionExpireTime: String,
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name
    ): Result<Long> = accountStore.saveQrCodeAccount(
        studentId = studentId,
        name = name,
        sessionToken = sessionToken,
        sessionExpireTime = sessionExpireTime,
        selectedLocation = selectedLocation
    )

    /**
     * 会话过期时用 `_sop_session_` 自动换取新的 SESSION。
     * 刷新成功后把有效期延长 7 天，避免频繁重新扫码。
     */
    suspend fun refreshSessionIfExpired(account: CheckinAccountData): Result<String> {
        val cookies = account.sessionToken
            ?: return Result.failure(CheckinException("Session token is null"))
        println("[QrCheckin] 正在自动刷新账号 ${account.studentId} 的 Session...")
        return completeSsoWithSopSession(cookies)
            .onSuccess { newCookies ->
                accountStore.updateSession(
                    accountId = account.id,
                    sessionToken = newCookies,
                    sessionExpireTime = CheckinClock.afterHours(REFRESHED_SESSION_VALID_HOURS)
                )
                println("[QrCheckin] 账号 ${account.studentId} Session 自动刷新成功")
            }
            .onFailure { println("[QrCheckin] 账号 ${account.studentId} Session 刷新失败: ${it.message}") }
    }

    // ==================== 任务与签到 ====================

    /**
     * 用已保存的会话签到：取待签到任务列表，对第一条执行签到。
     * 会话过期或接口返回未登录时，先自动刷新会话再重试一次。
     */
    suspend fun performCheckinWithSession(account: CheckinAccountData): CheckinResult =
        withContext(Dispatchers.IO) {
            try {
                val savedCookies = account.sessionToken
                if (savedCookies.isNullOrBlank()) {
                    return@withContext CheckinResult.Failed("Session 无效，请重新扫码登录")
                }

                var cookies: String = if (account.isSessionValid()) {
                    savedCookies
                } else {
                    println("[QrCheckin] Session 已过期，尝试自动刷新...")
                    refreshSessionIfExpired(account).getOrElse {
                        return@withContext CheckinResult.Failed("Session 已过期且自动刷新失败，请重新扫码登录")
                    }
                }

                var pendingResult = taskRepository.pendingTasks(gatewayFor(cookies))
                if (pendingResult.isSessionExpired()) {
                    println("[QrCheckin] 任务请求返回登录已失效，尝试刷新 Session...")
                    val refreshed = refreshSessionIfExpired(account).getOrNull()
                    if (refreshed != null) {
                        cookies = refreshed
                        pendingResult = taskRepository.pendingTasks(gatewayFor(refreshed))
                    }
                }

                val pendingTasks = pendingResult.getOrElse { error ->
                    val message = error.message ?: "获取任务失败"
                    return@withContext if (pendingResult.isSessionExpired()) {
                        CheckinResult.Failed("Session 已过期，请重新扫码登录")
                    } else {
                        CheckinResult.Failed(message)
                    }
                }
                if (pendingTasks.isEmpty()) {
                    return@withContext CheckinResult.NoTask("当前没有需要签到的任务")
                }

                taskRepository.checkinTask(
                    gateway = gatewayFor(cookies),
                    taskId = pendingTasks.first().id,
                    location = account.randomLocation()
                )
            } catch (e: Exception) {
                println("[QrCheckin] 签到异常: ${e.message}")
                CheckinResult.Failed(e.message ?: "签到失败")
            }
        }

    /** 对指定任务签到 */
    suspend fun checkinForSpecificTask(
        cookies: String,
        taskId: Long,
        account: CheckinAccountData
    ): CheckinResult = taskRepository.checkinTask(
        gateway = gatewayFor(cookies),
        taskId = taskId,
        location = account.randomLocation()
    )

    /** 获取三类任务：Triple<待签到, 已完成, 缺勤> */
    suspend fun getAllTasksWithCookies(
        cookies: String,
        initialLoadCount: Int = 5
    ): Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>> =
        taskRepository.allTasks(gatewayFor(cookies), initialLoadCount)

    /** 为 [startIndex, endIndex) 区间的任务补齐打卡时间 */
    suspend fun loadCheckinTimeForTasks(
        tasks: List<CheckinTask>,
        startIndex: Int,
        endIndex: Int,
        cookies: String
    ): Result<List<CheckinTask>> =
        taskRepository.loadCheckinTime(gatewayFor(cookies), tasks, startIndex, endIndex)

    // ==================== 内部实现 ====================

    private fun gatewayFor(cookies: String) = QrCodeTaskGateway(
        api = api,
        cookies = cookies,
        openId = sopSessionParser.openIdFromCookies(cookies)
    )

    /** 失败原因是否指向会话失效，用于决定要不要自动刷新后重试 */
    private fun Result<*>.isSessionExpired(): Boolean {
        val message = exceptionOrNull()?.message ?: return false
        return message.contains("401") || message.contains("未登录") || message.contains("过期")
    }

    private fun CheckinAccountData.randomLocation() =
        CheckinLocations.randomLocationForCampus(selectedLocation)

    private companion object {
        const val QFHY_HOST = "qfhy.suse.edu.cn"
        const val SOP_SESSION = "_sop_session_"
        const val SESSION = "SESSION"
        const val REFRESHED_SESSION_VALID_HOURS = 24 * 7
    }
}
