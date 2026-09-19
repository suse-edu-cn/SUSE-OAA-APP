package com.suseoaa.projectoaa.presentation.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinLocations
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTask
import com.suseoaa.projectoaa.shared.domain.model.checkin.EduUserInfo
import com.suseoaa.projectoaa.shared.domain.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.domain.repository.QrCodeCheckinRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.suseoaa.projectoaa.shared.util.AppLog
import com.suseoaa.projectoaa.domain.checkin.AutoLoginResult
import com.suseoaa.projectoaa.domain.checkin.PasswordAutoLogin

/** 密码登录的发起入口，验证码/短信通过后据此决定接着做什么。 */
internal enum class PasswordLoginEntry {
    CHECKIN,
    TASKS
}

/**
 * 652打卡 ViewModel
 *
 * 支持两种登录方式：
 * 1. 密码登录 - 使用 CheckinRepository
 * 2. 扫码登录 - 使用 QrCodeCheckinRepository
 */
class CheckinViewModel(
    private val passwordRepository: CheckinRepository,
    private val qrCodeRepository: QrCodeCheckinRepository,
    private val autoLogin: PasswordAutoLogin
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckinUiState())
    val uiState: StateFlow<CheckinUiState> = _uiState.asStateFlow()

    // 记录当前cookieStorage中已登录的密码账号学号，避免重复登录
    private var loggedInPasswordStudentId: String? = null

    // 标记当前登录入口，用于验证码/短信验证完成后的续流程。
    private var currentPasswordLoginEntry: PasswordLoginEntry = PasswordLoginEntry.CHECKIN

    /**
     * 账号管理已拆到独立的委派对象；ViewModel 保留同名方法只做转发，
     * 这样界面侧的调用点一处不用改，拆分对外完全无感。
     */
    private val accounts = CheckinAccountsDelegate(_uiState, viewModelScope, passwordRepository)

    /** 任务流程对登录流程的依赖，收敛成显式的四件事（见 CheckinLoginGateway）。 */
    private val loginGateway: CheckinLoginGateway = object : CheckinLoginGateway {
        override var loggedInStudentId: String?
            get() = loggedInPasswordStudentId
            set(value) { loggedInPasswordStudentId = value }

        override var loginEntry: PasswordLoginEntry
            get() = currentPasswordLoginEntry
            set(value) { currentPasswordLoginEntry = value }

        override suspend fun autoLogin(account: CheckinAccountData): Boolean =
            autoLoginForPasswordAccount(account)

        override fun requireSmsVerification(account: CheckinAccountData, entry: PasswordLoginEntry) =
            challenge.showSmsVerificationDialog(account, entry)

        override fun requireCaptcha(
            account: CheckinAccountData,
            errorMessage: String?,
            existingCaptchaBytes: ByteArray?,
            entry: PasswordLoginEntry,
        ) = challenge.showManualCaptchaDialog(account, errorMessage, existingCaptchaBytes, entry)

        override fun refreshAccounts() = loadAccounts()
    }

    private val tasks = CheckinTasksDelegate(
        _uiState, viewModelScope, passwordRepository, qrCodeRepository, loginGateway
    )

    private val challenge = CheckinPasswordChallengeDelegate(
        uiState = _uiState,
        scope = viewModelScope,
        passwordRepository = passwordRepository,
        session = loginGateway,
        loadTasks = tasks::loadTasksForAccount,
        refreshAccounts = ::loadAccounts,
    )

    // ==================== 二次验证（转发给 CheckinPasswordChallengeDelegate）====================

    fun refreshCaptcha() = challenge.refreshCaptcha()

    fun submitCaptchaAndCheckin(captchaCode: String) = challenge.submitCaptchaAndCheckin(captchaCode)

    fun sendSmsCode() = challenge.sendSmsCode()

    fun submitSmsCodeAndCheckin(smsCode: String) = challenge.submitSmsCodeAndCheckin(smsCode)

    fun cancelSmsVerification() = challenge.cancelSmsVerification()

    private val webViewLogin = CheckinWebViewLoginDelegate(
        _uiState, viewModelScope, passwordRepository, qrCodeRepository, ::loadAccounts
    )

    // ==================== 扫码添加账号（转发给 CheckinWebViewLoginDelegate）====================

    fun showWebViewLoginDialog() = webViewLogin.showWebViewLoginDialog()

    fun hideWebViewLoginDialog() = webViewLogin.hideWebViewLoginDialog()

    fun onWebViewLoginSuccess(cookies: Map<String, String>) =
        webViewLogin.onWebViewLoginSuccess(cookies)

    fun onWebViewLoginError(error: String) = webViewLogin.onWebViewLoginError(error)

    fun hideReloginDialog() = webViewLogin.hideReloginDialog()

    fun startRelogin() = webViewLogin.startRelogin()

    fun onReloginSuccess(cookies: Map<String, String>) = webViewLogin.onReloginSuccess(cookies)

    fun updateAccountSession(sessionToken: String, sessionExpireTime: String) =
        webViewLogin.updateAccountSession(sessionToken, sessionExpireTime)

    // ==================== 任务列表（转发给 CheckinTasksDelegate）====================

    fun loadTasksForAccount(account: CheckinAccountData) = tasks.loadTasksForAccount(account)

    fun clearTasks() = tasks.clearTasks()

    fun loadMoreCompletedTasks() = tasks.loadMoreCompletedTasks()

    fun checkinForTask(task: CheckinTask, allowRepeat: Boolean = true) =
        tasks.checkinForTask(task, allowRepeat)

    init {
        loadAccounts()
    }

    // ==================== 账号管理（转发给 CheckinAccountsDelegate）====================

    fun loadAccounts() = accounts.loadAccounts()

    fun setAccountFilter(filter: AccountFilterType) = accounts.setAccountFilter(filter)

    fun getFilteredAccounts(): List<CheckinAccountData> = accounts.filteredAccounts()

    fun addAccount(
        studentId: String,
        password: String,
        name: String = "",
        remark: String = "",
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name
    ) = accounts.addAccount(studentId, password, name, remark, selectedLocation)

    fun updateAccount(
        id: Long,
        studentId: String,
        password: String,
        name: String,
        remark: String,
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name
    ) = accounts.updateAccount(id, studentId, password, name, remark, selectedLocation)

    fun deleteAccount(id: Long) = accounts.deleteAccount(id)

    @Suppress("unused")
    fun updateLocation(accountId: Long, locationName: String) =
        accounts.updateLocation(accountId, locationName)

    fun showAddDialog() = accounts.showAddDialog()

    fun hideAddDialog() = accounts.hideAddDialog()

    fun showEditDialog(account: CheckinAccountData) = accounts.showEditDialog(account)

    fun hideEditDialog() = accounts.hideEditDialog()

    fun clearMessages() = accounts.clearMessages()

    /**
     * 批量打卡（仅密码登录账号）
     */
    fun batchCheckin() {
        val passwordAccounts = _uiState.value.accounts.filter { !it.isQrCodeLogin }
        if (passwordAccounts.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "没有可用的密码登录账号") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            var successCount = 0
            var failCount = 0

            for (account in passwordAccounts) {
                try {
                    _uiState.update { it.copy(currentCheckingAccount = account) }

                    // 使用自动打卡流程
                    val (success, message) = performAutoCheckinSync(account)
                    
                    val accountName = account.name.ifBlank { account.studentId }
                    com.suseoaa.projectoaa.util.ToastManager.showToast("[$accountName] $message")
                    
                    if (success) {
                        successCount++
                    } else {
                        failCount++
                    }

                    // 每个账号之间稍作延迟，避免请求过快
                    delay(500)
                } catch (e: Throwable) {
                    failCount++
                    val accountName = account.name.ifBlank { account.studentId }
                    com.suseoaa.projectoaa.util.ToastManager.showToast("[$accountName] 打卡异常")
                    AppLog.e("[BatchCheckin] 账号 ${account.studentId} 打卡失败: ${e.message}")
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentCheckingAccount = null,
                    successMessage = "批量打卡完成: 成功 $successCount 个，失败 $failCount 个"
                )
            }
        }
    }

    /**
     * 同步执行自动打卡（用于批量打卡）
     */
    private suspend fun performAutoCheckinSync(account: CheckinAccountData): Pair<Boolean, String> {
        try {
            // 先尝试复用 rememberMe 登录态，失败再回退验证码登录。
            val loginSuccess = autoLoginForPasswordAccount(account)
            if (!loginSuccess) {
                if (passwordRepository.hasPendingSmsChallenge()) {
                    passwordRepository.clearPendingSmsChallenge()
                }
                return Pair(false, "自动登录失败")
            }
            loggedInPasswordStudentId = account.studentId

            // 执行打卡
            val checkinResult = passwordRepository.performCheckinAfterLogin(account)
            return when (checkinResult) {
                is CheckinResult.Success -> Pair(true, checkinResult.message)
                is CheckinResult.AlreadyChecked -> Pair(true, checkinResult.message)
                is CheckinResult.NoTask -> Pair(true, checkinResult.message)
                is CheckinResult.Failed -> Pair(false, checkinResult.error)
            }
        } catch (e: Throwable) {
            return Pair(false, "异常: ${e.message}")
        }
    }

    /**
     * 为密码登录账号自动登录（不打卡，仅登录以获取 cookie）。
     * 具体的 rememberMe / 验证码 OCR / 重试策略见 [PasswordAutoLogin]，
     * 这里只负责维护「当前 cookieStorage 里是哪个账号」这一份界面状态。
     */
    private suspend fun autoLoginForPasswordAccount(account: CheckinAccountData): Boolean {
        // fetchCaptchaImage 会清空 cookie，登录期间先按未登录处理
        loggedInPasswordStudentId = null
        return when (autoLogin.login(account)) {
            is AutoLoginResult.Success -> {
                loggedInPasswordStudentId = account.studentId
                true
            }

            is AutoLoginResult.SmsRequired,
            is AutoLoginResult.Failed -> false
        }
    }

    // ==================== 打卡操作（带验证码） ====================

    /**
     * 开始打卡流程
     * - 密码登录账号：自动尝试OCR识别并打卡，失败时才显示验证码对话框
     * - 扫码登录账号：直接使用Session签到，如果Session过期则提示重新扫码
     */
    fun startCheckin(account: CheckinAccountData) {
        if (account.isQrCodeLogin) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, currentCheckingAccount = account) }
                var isSessionOk = account.isSessionValid()
                var currentAccount = account
                if (!isSessionOk) {
                    AppLog.d("[Checkin] startCheckin: Session已过期，尝试自动刷新...")
                    _uiState.update { it.copy(successMessage = "正在更新登录状态...") }
                    val refreshResult = qrCodeRepository.refreshSessionIfExpired(account)
                    if (refreshResult.isSuccess) {
                        isSessionOk = true
                        val updatedAccount = passwordRepository.getAccountById(account.id)
                        if (updatedAccount != null) {
                            currentAccount = updatedAccount
                        }
                    }
                }
                
                if (isSessionOk) {
                    performQrCodeCheckin(currentAccount)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentCheckingAccount = null,
                            accountNeedRelogin = account,
                            showReloginDialog = true
                        )
                    }
                }
            }
        } else {
            // 密码登录账号 - 自动尝试OCR识别并打卡
            currentPasswordLoginEntry = PasswordLoginEntry.CHECKIN
            performAutoCheckin(account)
        }
    }

    /**
     * 自动打卡流程（密码登录账号）
     * 1. 获取验证码图片
     * 2. OCR自动识别
     * 3. 自动登录并打卡
     * 4. 如果识别失败或验证码错误，才弹出手动输入对话框
     */
    private fun performAutoCheckin(account: CheckinAccountData, retryCount: Int = 0) {
        viewModelScope.launch {
            currentPasswordLoginEntry = PasswordLoginEntry.CHECKIN
            _uiState.update { it.copy(isLoading = true, currentCheckingAccount = account) }

            try {
                val fastLogin = passwordRepository.tryAutoLoginWithRememberMe(account).getOrDefault(false)
                if (fastLogin) {
                    loggedInPasswordStudentId = account.studentId
                    AppLog.d("[AutoCheckin] 使用 rememberMe 快速登录成功")

                    val checkinResult = passwordRepository.performCheckinAfterLogin(account)
                    val message = when (checkinResult) {
                        is CheckinResult.Success -> checkinResult.message
                        is CheckinResult.AlreadyChecked -> checkinResult.message
                        is CheckinResult.NoTask -> checkinResult.message
                        is CheckinResult.Failed -> checkinResult.error
                    }
                    
                    com.suseoaa.projectoaa.util.ToastManager.showToast(message)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentCheckingAccount = null
                        )
                    }
                    loadAccounts()
                    return@launch
                }

                // 1. 获取验证码图片
                val captchaResult = passwordRepository.fetchCaptchaImage()
                if (captchaResult.isFailure) {
                    // 获取验证码失败，显示手动输入对话框
                    challenge.showManualCaptchaDialog(account, captchaResult.exceptionOrNull()?.message)
                    return@launch
                }

                val captchaBytes = captchaResult.getOrThrow()

                // 2. OCR自动识别
                val ocrResult = try {
                    com.suseoaa.projectoaa.util.PlatformCaptchaOcr.recognize(captchaBytes)
                } catch (t: Throwable) {
                    AppLog.e("[AutoCheckin] OCR 运行时异常: ${t.message}")
                    challenge.showManualCaptchaDialog(account, "OCR组件不可用，已降级为手动验证码", captchaBytes)
                    return@launch
                }

                if (ocrResult.isFailure || ocrResult.getOrNull()?.length != 4) {
                    // OCR识别失败，显示手动输入对话框
                    AppLog.e("[AutoCheckin] OCR识别失败: ${ocrResult.exceptionOrNull()?.message ?: "识别结果长度不正确"}")
                    challenge.showManualCaptchaDialog(account, null, captchaBytes)
                    return@launch
                }

                val captchaCode = ocrResult.getOrThrow()
                AppLog.d("[AutoCheckin] OCR识别成功: $captchaCode")

                // 3. 自动登录
                // fetchCaptchaImage 会清除cookies，所以登录状态已失效
                loggedInPasswordStudentId = null
                val loginResult = passwordRepository.loginWithCaptcha(
                    username = account.studentId,
                    password = account.password,
                    captchaCode = captchaCode,
                    accountId = account.id
                )

                if (loginResult.isFailure) {
                    val errorMsg = loginResult.exceptionOrNull()?.message ?: ""
                    if (passwordRepository.isSmsVerificationRequired(loginResult.exceptionOrNull())) {
                        AppLog.d("[AutoCheckin] 进入短信二次验证流程")
                        challenge.showSmsVerificationDialog(account, PasswordLoginEntry.CHECKIN)
                        return@launch
                    }
                    // 验证码错误，最多重试2次
                    if ((errorMsg.contains("验证码") || errorMsg.contains(
                            "captcha",
                            ignoreCase = true
                        )) && retryCount < 2
                    ) {
                        AppLog.e("[AutoCheckin] 验证码错误，重试第 ${retryCount + 1} 次")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentCheckingAccount = null
                            )
                        }
                        performAutoCheckin(account, retryCount + 1)
                        return@launch
                    }
                    // 其他登录错误或重试次数用尽，显示手动输入对话框
                    AppLog.e("[AutoCheckin] 登录失败: $errorMsg")
                    challenge.showManualCaptchaDialog(account, errorMsg)
                    return@launch
                }

                // 登录成功，记录登录状态
                loggedInPasswordStudentId = account.studentId

                // 4. 执行打卡
                val checkinResult = passwordRepository.performCheckinAfterLogin(account)
                val message = when (checkinResult) {
                    is CheckinResult.Success -> checkinResult.message
                    is CheckinResult.AlreadyChecked -> checkinResult.message
                    is CheckinResult.NoTask -> checkinResult.message
                    is CheckinResult.Failed -> checkinResult.error
                }
                
                com.suseoaa.projectoaa.util.ToastManager.showToast(message)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentCheckingAccount = null
                    )
                }
                loadAccounts()

            } catch (e: Throwable) {
                AppLog.e("[AutoCheckin] 异常: ${e.message}")
                challenge.showManualCaptchaDialog(account, e.message)
            }
        }
    }

    /**
     * 执行扫码登录账号的签到
     */
    private fun performQrCodeCheckin(account: CheckinAccountData) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentCheckingAccount = account) }

            val result = qrCodeRepository.performCheckinWithSession(account)
            val message = when (result) {
                is CheckinResult.Success -> result.message
                is CheckinResult.AlreadyChecked -> result.message
                is CheckinResult.NoTask -> result.message
                is CheckinResult.Failed -> {
                    // 检查是否是 Session 过期
                    if (result.error.contains("过期") || result.error.contains("重新登录")) {
                        // 清除 Session，提示重新扫码
                        passwordRepository.clearSession(account.id)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentCheckingAccount = null,
                                accountNeedRelogin = account,
                                showReloginDialog = true
                            )
                        }
                        loadAccounts()
                        return@launch
                    }
                    result.error
                }
            }
            
            com.suseoaa.projectoaa.util.ToastManager.showToast(message)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentCheckingAccount = null
                )
            }
            loadAccounts()
        }
    }

    /**
     * 取消打卡
     */
    fun cancelCheckin() {
        passwordRepository.clearPendingSmsChallenge()
        challenge.stopResendCountdown()
        val fromTasks = currentPasswordLoginEntry == PasswordLoginEntry.TASKS
        _uiState.update {
            it.copy(
                showCaptchaDialog = false,
                showSmsDialog = false,
                currentCheckingAccount = null,
                captchaImageBytes = null,
                isLoadingCaptcha = false,
                isLoggingIn = false,
                smsMaskedPhone = null,
                isSendingSmsCode = false,
                isVerifyingSmsCode = false,
                smsResendCountdownSeconds = 0,
                isLoadingTasks = false,
                selectedAccount = if (fromTasks) null else it.selectedAccount,
                pendingTasks = if (fromTasks) emptyList() else it.pendingTasks,
                completedTasks = if (fromTasks) emptyList() else it.completedTasks,
                absentTasks = if (fromTasks) emptyList() else it.absentTasks,
                displayedCompletedCount = if (fromTasks) 6 else it.displayedCompletedCount
            )
        }
        currentPasswordLoginEntry = PasswordLoginEntry.CHECKIN
    }

    // ==================== 对话框控制 ====================

    // ==================== WebView 扫码登录操作 ====================

    // ==================== 任务列表操作 ====================

    // ==================== 旧的扫码登录相关操作（保留兼容）====================

    /**
     * 显示扫码添加账号对话框。
     *
     * 名字沿用历史：早期是自绘二维码 + 轮询扫码状态，现已全部改走 WebView，
     * 那套轮询代码（fetchQrCode / refreshQrCode / confirmQrCodeLogin）已无人调用并删除。
     * 界面调用点仍叫这个名字，这里转发过去。
     */
    fun showQrCodeDialog() = showWebViewLoginDialog()

    override fun onCleared() {
        challenge.dispose()
        super.onCleared()
    }
}
