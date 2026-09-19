package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinLocations
import com.suseoaa.projectoaa.shared.domain.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.domain.repository.QrCodeCheckinRepository
import com.suseoaa.projectoaa.shared.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * 微信扫码（WebView）添加账号与会话续期。
 *
 * 链路有多级兜底：先从 `_sop_session_` JWT 里取学号，取不到再调接口；拿到学号后
 * 还要再换一次签到专用的 SESSION 才能落库——WebView 返回的是 /edu/ 路径的会话，
 * 签到接口要的是 /xg/app/qddk/admin 返回的那个。各级失败给出可分辨的提示，
 * 否则用户只会看到"添加失败"而不知道卡在哪一步。分支由 CheckinWebViewLoginTest 覆盖。
 */
internal class CheckinWebViewLoginDelegate(
    private val uiState: MutableStateFlow<CheckinUiState>,
    private val scope: CoroutineScope,
    private val passwordRepository: CheckinRepository,
    private val qrCodeRepository: QrCodeCheckinRepository,
    private val refreshAccounts: () -> Unit,
) {

    /**
     * 显示 WebView 扫码登录对话框
     * 使用 WebView 加载微信扫码页面，获取 Cookie 后调用 API 获取用户信息
     */
    fun showWebViewLoginDialog() {
        uiState.update {
            it.copy(
                showWebViewLoginDialog = true,
                scannedUserInfo = null,
                scannedCookies = null
            )
        }
    }

    /**
     * 隐藏 WebView 登录对话框
     */
    fun hideWebViewLoginDialog() {
        uiState.update {
            it.copy(
                showWebViewLoginDialog = false,
                scannedUserInfo = null,
                scannedCookies = null
            )
        }
    }

    /**
     * WebView 扫码登录成功后处理
     * @param cookies WebView 获取的 Cookie 字符串
     */
    fun onWebViewLoginSuccess(cookies: Map<String, String>) {
        if (uiState.value.isLoading) {
            AppLog.d("[Checkin] onWebViewLoginSuccess: 已经在登录处理中，忽略重复的成功回调")
            return
        }
        scope.launch {
            uiState.update { it.copy(isLoading = true) }

            // 将 Cookie Map 转为字符串
            val cookieString = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            AppLog.d("[Checkin] WebView 登录成功，Cookie: $cookieString")

            var studentId: String? = null
            var studentName: String = ""

            // 优先尝试从 _sop_session_ JWT 中提取用户信息
            val sopSession = cookies["_sop_session_"]
            if (!sopSession.isNullOrBlank()) {
                val userInfo = qrCodeRepository.extractUserInfoFromSopSession(sopSession)
                if (userInfo != null) {
                    studentId = userInfo.studentId
                    studentName = userInfo.name
                    AppLog.d("[Checkin] 从 JWT 获取到用户信息: $studentId, $studentName")
                }
            }

            // 如果 JWT 中没有获取到，尝试调用 API
            if (studentId.isNullOrBlank()) {
                AppLog.d("[Checkin] JWT 中未获取到学号，尝试调用 API...")
                val userInfoResult = qrCodeRepository.getEduUserInfoWithCookies(cookieString)

                if (userInfoResult.isSuccess) {
                    val userInfo = userInfoResult.getOrThrow()
                    studentId = userInfo.code
                    studentName = userInfo.name ?: ""
                    AppLog.d("[Checkin] 从 API 获取到用户信息: $studentId, $studentName")
                } else {
                    AppLog.e("[Checkin] API 获取用户信息失败: ${userInfoResult.exceptionOrNull()?.message}")
                }
            }

            // 检查是否获取到学号
            if (studentId.isNullOrBlank()) {
                uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "获取学号失败，请确保已完成微信扫码授权"
                    )
                }
                return@launch
            }

            AppLog.d("[Checkin] 最终用户信息: studentId=$studentId, name=$studentName")

            // 检查账号是否已存在
            val exists = passwordRepository.isAccountExists(studentId)
            AppLog.d("[Checkin] 账号是否已存在: $exists")

            if (exists) {
                uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "该学号账号已存在"
                    )
                }
                return@launch
            }

            // 在保存账号前，必须访问签到页面获取用于 /site/ API 的 SESSION
            // WebView 返回的 SESSION 是 /edu/ 路径的，签到 API 需要 /xg/app/qddk/admin 返回的 SESSION
            var fullCookies = cookieString
            AppLog.d("[Checkin] 尝试获取签到专用 SESSION...")
            val ssoResult = qrCodeRepository.completeSsoWithSopSession(cookieString)
            if (ssoResult.isSuccess) {
                fullCookies = ssoResult.getOrThrow()
                AppLog.d("[Checkin] 获取签到 SESSION 成功")
            } else {
                AppLog.e("[Checkin] 获取签到 SESSION 失败: ${ssoResult.exceptionOrNull()?.message}")
                uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "获取签到授权失败，请重试: ${ssoResult.exceptionOrNull()?.message}"
                    )
                }
                return@launch
            }

            // 保存账号
            val now = com.suseoaa.projectoaa.shared.util.OaaClock.now()
                .toLocalDateTime(TimeZone.of("Asia/Shanghai"))
            val expireTime = "${now.date.plus(DatePeriod(days = 7))} ${now.time}"

            val result = qrCodeRepository.saveQrCodeAccount(
                studentId = studentId,
                name = studentName,
                sessionToken = fullCookies,
                sessionExpireTime = expireTime,
                selectedLocation = CheckinLocations.DEFAULT_CAMPUS.name
            )

            if (result.isSuccess) {
                uiState.update {
                    it.copy(
                        isLoading = false,
                        showWebViewLoginDialog = false,
                        successMessage = "账号添加成功！学号: $studentId, 姓名: $studentName",
                        scannedUserInfo = null,
                        scannedCookies = null
                    )
                }
                refreshAccounts()
            } else {
                uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "添加账号失败: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    /**
     * WebView 登录失败处理
     */
    fun onWebViewLoginError(error: String) {
        uiState.update {
            it.copy(
                errorMessage = "扫码登录失败: $error"
            )
        }
    }

    /**
     * 隐藏重新登录对话框
     */
    fun hideReloginDialog() {
        uiState.update {
            it.copy(
                showReloginDialog = false,
                accountNeedRelogin = null
            )
        }
    }

    /**
     * 开始重新扫码登录
     */
    fun startRelogin() {
        val account = uiState.value.accountNeedRelogin ?: return
        uiState.update {
            it.copy(
                showReloginDialog = false,
                accountNeedRelogin = null,
                showWebViewLoginDialog = true,
                currentCheckingAccount = account // 记住要更新的账号
            )
        }
    }

    /**
     * WebView 重新登录成功后处理
     */
    @Suppress("unused")
    fun onReloginSuccess(cookies: Map<String, String>) {
        val account = uiState.value.currentCheckingAccount ?: return
        scope.launch {
            val cookieString = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            val now = com.suseoaa.projectoaa.shared.util.OaaClock.now()
                .toLocalDateTime(TimeZone.of("Asia/Shanghai"))
            val expireTime = "${now.date.plus(DatePeriod(days = 7))} ${now.time}"

            val result = passwordRepository.updateSession(account.id, cookieString, expireTime)
            if (result.isSuccess) {
                uiState.update {
                    it.copy(
                        successMessage = "重新登录成功",
                        showWebViewLoginDialog = false,
                        currentCheckingAccount = null
                    )
                }
                refreshAccounts()
            } else {
                uiState.update {
                    it.copy(errorMessage = "更新Session失败: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    /**
     * 更新账号的 Session（重新扫码登录后）
     */
    @Suppress("unused")
    fun updateAccountSession(sessionToken: String, sessionExpireTime: String) {
        val account = uiState.value.currentCheckingAccount ?: return
        scope.launch {
            val result =
                passwordRepository.updateSession(account.id, sessionToken, sessionExpireTime)
            if (result.isSuccess) {
                uiState.update {
                    it.copy(
                        successMessage = "登录成功",
                        showQrCodeDialog = false,
                        qrCodeUrl = null,
                        qrCodeClientId = null,
                        currentCheckingAccount = null
                    )
                }
                refreshAccounts()
            } else {
                uiState.update {
                    it.copy(errorMessage = "更新Session失败: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }}
