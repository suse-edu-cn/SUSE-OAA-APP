package com.suseoaa.projectoaa.presentation.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinLocations
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTask
import com.suseoaa.projectoaa.shared.domain.model.checkin.EduUserInfo
import com.suseoaa.projectoaa.shared.data.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.data.repository.QrCodeCheckinRepository
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

private enum class PasswordLoginEntry {
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

    // 轮询扫码状态的 Job
    private var scanPollingJob: Job? = null

    // 短信验证码重发倒计时 Job
    private var smsResendCountdownJob: Job? = null

    private companion object {
        const val SMS_RESEND_COUNTDOWN_SECONDS = 30
    }

    // 记录当前cookieStorage中已登录的密码账号学号，避免重复登录
    private var loggedInPasswordStudentId: String? = null

    // 标记当前登录入口，用于验证码/短信验证完成后的续流程。
    private var currentPasswordLoginEntry: PasswordLoginEntry = PasswordLoginEntry.CHECKIN

    init {
        loadAccounts()
    }

    /**
     * 加载所有账号
     */
    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val accounts = passwordRepository.getAllAccounts()
                _uiState.update { it.copy(accounts = accounts, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "加载账号失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 设置账号筛选类型
     */
    fun setAccountFilter(filter: AccountFilterType) {
        _uiState.update { it.copy(accountFilter = filter) }
    }

    /**
     * 获取筛选后的账号列表
     */
    fun getFilteredAccounts(): List<CheckinAccountData> = _uiState.value.filteredAccounts

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
                    println("[BatchCheckin] 账号 ${account.studentId} 打卡失败: ${e.message}")
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

    /**
     * 添加账号（密码登录）
     */
    fun addAccount(
        studentId: String,
        password: String,
        name: String = "",
        remark: String = "",
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name
    ) {
        viewModelScope.launch {
            if (studentId.isBlank() || password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "学号和密码不能为空") }
                return@launch
            }

            if (passwordRepository.isAccountExists(studentId)) {
                _uiState.update { it.copy(errorMessage = "该学号已存在") }
                return@launch
            }

            val result =
                passwordRepository.addAccount(studentId, password, name, remark, selectedLocation)
            if (result.isSuccess) {
                _uiState.update { it.copy(successMessage = "添加成功", showAddDialog = false) }
                loadAccounts()
            } else {
                _uiState.update { it.copy(errorMessage = "添加失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    /**
     * 更新账号
     */
    fun updateAccount(
        id: Long,
        studentId: String,
        password: String,
        name: String,
        remark: String,
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name
    ) {
        viewModelScope.launch {
            if (studentId.isBlank() || password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "学号和密码不能为空") }
                return@launch
            }

            val result = passwordRepository.updateAccount(
                id,
                studentId,
                password,
                name,
                remark,
                selectedLocation
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        successMessage = "更新成功",
                        showEditDialog = false,
                        editingAccount = null
                    )
                }
                loadAccounts()
            } else {
                _uiState.update { it.copy(errorMessage = "更新失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    /**
     * 删除账号
     */
    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            val result = passwordRepository.deleteAccount(id)
            if (result.isSuccess) {
                _uiState.update { it.copy(successMessage = "删除成功") }
                loadAccounts()
            } else {
                _uiState.update { it.copy(errorMessage = "删除失败: ${result.exceptionOrNull()?.message}") }
            }
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
                    println("[Checkin] startCheckin: Session已过期，尝试自动刷新...")
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
                    println("[AutoCheckin] 使用 rememberMe 快速登录成功")

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
                    showManualCaptchaDialog(account, captchaResult.exceptionOrNull()?.message)
                    return@launch
                }

                val captchaBytes = captchaResult.getOrThrow()

                // 2. OCR自动识别
                val ocrResult = try {
                    com.suseoaa.projectoaa.util.PlatformCaptchaOcr.recognize(captchaBytes)
                } catch (t: Throwable) {
                    println("[AutoCheckin] OCR 运行时异常: ${t.message}")
                    showManualCaptchaDialog(account, "OCR组件不可用，已降级为手动验证码", captchaBytes)
                    return@launch
                }

                if (ocrResult.isFailure || ocrResult.getOrNull()?.length != 4) {
                    // OCR识别失败，显示手动输入对话框
                    println("[AutoCheckin] OCR识别失败: ${ocrResult.exceptionOrNull()?.message ?: "识别结果长度不正确"}")
                    showManualCaptchaDialog(account, null, captchaBytes)
                    return@launch
                }

                val captchaCode = ocrResult.getOrThrow()
                println("[AutoCheckin] OCR识别成功: $captchaCode")

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
                        println("[AutoCheckin] 进入短信二次验证流程")
                        showSmsVerificationDialog(account, PasswordLoginEntry.CHECKIN)
                        return@launch
                    }
                    // 验证码错误，最多重试2次
                    if ((errorMsg.contains("验证码") || errorMsg.contains(
                            "captcha",
                            ignoreCase = true
                        )) && retryCount < 2
                    ) {
                        println("[AutoCheckin] 验证码错误，重试第 ${retryCount + 1} 次")
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
                    println("[AutoCheckin] 登录失败: $errorMsg")
                    showManualCaptchaDialog(account, errorMsg)
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
                println("[AutoCheckin] 异常: ${e.message}")
                showManualCaptchaDialog(account, e.message)
            }
        }
    }

    /**
     * 显示手动输入验证码对话框
     */
    private fun showManualCaptchaDialog(
        account: CheckinAccountData,
        errorMessage: String?,
        existingCaptchaBytes: ByteArray? = null,
        entry: PasswordLoginEntry = currentPasswordLoginEntry
    ) {
        currentPasswordLoginEntry = entry
        _uiState.update {
            it.copy(
                isLoading = false,
                isLoadingTasks = false,
                currentCheckingAccount = account,
                showCaptchaDialog = true,
                captchaImageBytes = existingCaptchaBytes,
                isLoadingCaptcha = existingCaptchaBytes == null,
                errorMessage = errorMessage?.let { msg -> "自动登录失败: $msg，请手动输入验证码" }
            )
        }
        // 如果没有现有验证码图片，获取新的
        if (existingCaptchaBytes == null) {
            refreshCaptcha()
        }
    }

    private fun showSmsVerificationDialog(account: CheckinAccountData) {
        showSmsVerificationDialog(account, currentPasswordLoginEntry)
    }

    private fun showSmsVerificationDialog(
        account: CheckinAccountData,
        entry: PasswordLoginEntry
    ) {
        stopSmsResendCountdown()
        currentPasswordLoginEntry = entry
        _uiState.update {
            it.copy(
                isLoading = false,
                showCaptchaDialog = false,
                captchaImageBytes = null,
                isLoadingCaptcha = false,
                isLoggingIn = false,
                currentCheckingAccount = account,
                showSmsDialog = true,
                smsMaskedPhone = passwordRepository.getPendingSmsMaskedPhone(),
                isSendingSmsCode = false,
                isVerifyingSmsCode = false,
                smsResendCountdownSeconds = 0,
                errorMessage = null
            )
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
     * 刷新验证码
     */
    fun refreshCaptcha() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCaptcha = true) }

            val result = passwordRepository.fetchCaptchaImage()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        captchaImageBytes = result.getOrNull(),
                        isLoadingCaptcha = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingCaptcha = false,
                        errorMessage = "获取验证码失败: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    /**
     * 提交验证码并执行打卡
     */
    fun submitCaptchaAndCheckin(captchaCode: String) {
        val account = _uiState.value.currentCheckingAccount ?: return

        if (captchaCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入验证码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true) }

            // 1. 登录
            val loginResult = passwordRepository.loginWithCaptcha(
                username = account.studentId,
                password = account.password,
                captchaCode = captchaCode,
                accountId = account.id
            )

            if (loginResult.isFailure) {
                if (passwordRepository.isSmsVerificationRequired(loginResult.exceptionOrNull())) {
                    _uiState.update { it.copy(isLoggingIn = false) }
                    showSmsVerificationDialog(account)
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        errorMessage = loginResult.exceptionOrNull()?.message ?: "登录失败"
                    )
                }
                // 刷新验证码
                refreshCaptcha()
                return@launch
            }

            loggedInPasswordStudentId = account.studentId

            if (currentPasswordLoginEntry == PasswordLoginEntry.TASKS) {
                _uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        showCaptchaDialog = false,
                        currentCheckingAccount = null,
                        captchaImageBytes = null,
                        successMessage = "登录成功，正在加载任务列表..."
                    )
                }
                loadTasksForAccount(account)
                return@launch
            }

            // 2. 执行打卡
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
                    isLoggingIn = false,
                    showCaptchaDialog = false,
                    currentCheckingAccount = null,
                    captchaImageBytes = null
                )
            }
            loadAccounts() // 刷新状态
        }
    }

    fun sendSmsCode() {
        val state = _uiState.value
        if (state.isVerifyingSmsCode || state.isSendingSmsCode || state.smsResendCountdownSeconds > 0) {
            return
        }

        startSmsResendCountdown()

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingSmsCode = true) }

            val result = passwordRepository.sendSmsCodeForPendingLogin()
            if (result.isFailure) {
                println("[SmsVerification] sendSmsCode failed: ${result.exceptionOrNull()?.message}")
            }

            _uiState.update { it.copy(isSendingSmsCode = false) }
        }
    }

    private fun startSmsResendCountdown() {
        stopSmsResendCountdown()
        _uiState.update { it.copy(smsResendCountdownSeconds = SMS_RESEND_COUNTDOWN_SECONDS) }

        smsResendCountdownJob = viewModelScope.launch {
            for (remaining in (SMS_RESEND_COUNTDOWN_SECONDS - 1) downTo 0) {
                delay(1000)
                _uiState.update { it.copy(smsResendCountdownSeconds = remaining) }
            }
        }
    }

    private fun stopSmsResendCountdown() {
        smsResendCountdownJob?.cancel()
        smsResendCountdownJob = null
    }

    fun submitSmsCodeAndCheckin(smsCode: String) {
        val account = _uiState.value.currentCheckingAccount ?: return
        if (smsCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入短信验证码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingSmsCode = true) }

            val verifyResult = passwordRepository.submitSmsCodeForPendingLogin(smsCode)
            if (verifyResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isVerifyingSmsCode = false,
                        errorMessage = verifyResult.exceptionOrNull()?.message ?: "短信验证失败"
                    )
                }
                return@launch
            }

            loggedInPasswordStudentId = account.studentId

            if (currentPasswordLoginEntry == PasswordLoginEntry.TASKS) {
                stopSmsResendCountdown()
                _uiState.update {
                    it.copy(
                        isVerifyingSmsCode = false,
                        showSmsDialog = false,
                        smsMaskedPhone = null,
                        smsResendCountdownSeconds = 0,
                        currentCheckingAccount = null,
                        successMessage = "登录成功，正在加载任务列表..."
                    )
                }
                loadTasksForAccount(account)
                return@launch
            }

            val checkinResult = passwordRepository.performCheckinAfterLogin(account)
            val message = when (checkinResult) {
                is CheckinResult.Success -> checkinResult.message
                is CheckinResult.AlreadyChecked -> checkinResult.message
                is CheckinResult.NoTask -> checkinResult.message
                is CheckinResult.Failed -> checkinResult.error
            }

            stopSmsResendCountdown()
            _uiState.update {
                it.copy(
                    isVerifyingSmsCode = false,
                    showSmsDialog = false,
                    smsMaskedPhone = null,
                    smsResendCountdownSeconds = 0,
                    currentCheckingAccount = null,
                    successMessage = if (checkinResult is CheckinResult.Failed) null else message,
                    errorMessage = if (checkinResult is CheckinResult.Failed) message else null
                )
            }
            loadAccounts()
        }
    }

    fun cancelSmsVerification() {
        passwordRepository.clearPendingSmsChallenge()
        stopSmsResendCountdown()
        val fromTasks = currentPasswordLoginEntry == PasswordLoginEntry.TASKS
        _uiState.update {
            it.copy(
                showSmsDialog = false,
                smsMaskedPhone = null,
                isSendingSmsCode = false,
                isVerifyingSmsCode = false,
                smsResendCountdownSeconds = 0,
                currentCheckingAccount = null,
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

    /**
     * 取消打卡
     */
    fun cancelCheckin() {
        passwordRepository.clearPendingSmsChallenge()
        stopSmsResendCountdown()
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

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun showEditDialog(account: CheckinAccountData) {
        _uiState.update { it.copy(showEditDialog = true, editingAccount = account) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, editingAccount = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // ==================== WebView 扫码登录操作 ====================

    /**
     * 显示 WebView 扫码登录对话框
     * 使用 WebView 加载微信扫码页面，获取 Cookie 后调用 API 获取用户信息
     */
    fun showWebViewLoginDialog() {
        _uiState.update {
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
        _uiState.update {
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
        if (_uiState.value.isLoading) {
            println("[Checkin] onWebViewLoginSuccess: 已经在登录处理中，忽略重复的成功回调")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 将 Cookie Map 转为字符串
            val cookieString = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            println("[Checkin] WebView 登录成功，Cookie: $cookieString")

            var studentId: String? = null
            var studentName: String = ""

            // 优先尝试从 _sop_session_ JWT 中提取用户信息
            val sopSession = cookies["_sop_session_"]
            if (!sopSession.isNullOrBlank()) {
                val userInfo = qrCodeRepository.extractUserInfoFromSopSession(sopSession)
                if (userInfo != null) {
                    studentId = userInfo.studentId
                    studentName = userInfo.name
                    println("[Checkin] 从 JWT 获取到用户信息: $studentId, $studentName")
                }
            }

            // 如果 JWT 中没有获取到，尝试调用 API
            if (studentId.isNullOrBlank()) {
                println("[Checkin] JWT 中未获取到学号，尝试调用 API...")
                val userInfoResult = qrCodeRepository.getEduUserInfoWithCookies(cookieString)

                if (userInfoResult.isSuccess) {
                    val userInfo = userInfoResult.getOrThrow()
                    studentId = userInfo.code
                    studentName = userInfo.name ?: ""
                    println("[Checkin] 从 API 获取到用户信息: $studentId, $studentName")
                } else {
                    println("[Checkin] API 获取用户信息失败: ${userInfoResult.exceptionOrNull()?.message}")
                }
            }

            // 检查是否获取到学号
            if (studentId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "获取学号失败，请确保已完成微信扫码授权"
                    )
                }
                return@launch
            }

            println("[Checkin] 最终用户信息: studentId=$studentId, name=$studentName")

            // 检查账号是否已存在
            val exists = passwordRepository.isAccountExists(studentId)
            println("[Checkin] 账号是否已存在: $exists")

            if (exists) {
                _uiState.update {
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
            println("[Checkin] 尝试获取签到专用 SESSION...")
            val ssoResult = qrCodeRepository.completeSsoWithSopSession(cookieString)
            if (ssoResult.isSuccess) {
                fullCookies = ssoResult.getOrThrow()
                println("[Checkin] 获取签到 SESSION 成功")
            } else {
                println("[Checkin] 获取签到 SESSION 失败: ${ssoResult.exceptionOrNull()?.message}")
                _uiState.update {
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showWebViewLoginDialog = false,
                        successMessage = "账号添加成功！学号: $studentId, 姓名: $studentName",
                        scannedUserInfo = null,
                        scannedCookies = null
                    )
                }
                loadAccounts()
            } else {
                _uiState.update {
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
        _uiState.update {
            it.copy(
                errorMessage = "扫码登录失败: $error"
            )
        }
    }

    // ==================== 任务列表操作 ====================

    /**
     * 加载指定账号的打卡任务列表
     * @param account 要查看任务的账号
     */
    fun loadTasksForAccount(account: CheckinAccountData) {
        viewModelScope.launch {
            val initialDisplayCount = 6  // 初始显示的已打卡任务数量

            _uiState.update {
                it.copy(
                    isLoadingTasks = true,
                    selectedAccount = account,
                    pendingTasks = emptyList(),
                    completedTasks = emptyList(),
                    absentTasks = emptyList(),
                    displayedCompletedCount = initialDisplayCount
                )
            }

            try {
                // 根据登录类型获取任务（初始加载打卡时间的数量与显示数量一致）
                val (pending, completed, absent) = if (account.isQrCodeLogin) {
                    var currentCookies = account.sessionToken ?: ""
                    var isSessionOk = account.isSessionValid()
                    if (!isSessionOk) {
                        println("[TaskList] 扫码登录 Session 已过期，尝试自动刷新...")
                        val refreshResult = qrCodeRepository.refreshSessionIfExpired(account)
                        if (refreshResult.isSuccess) {
                            currentCookies = refreshResult.getOrThrow()
                            isSessionOk = true
                        }
                    }

                    if (!isSessionOk) {
                        _uiState.update {
                            it.copy(
                                isLoadingTasks = false,
                                accountNeedRelogin = account,
                                showReloginDialog = true
                            )
                        }
                        return@launch
                    }

                    println("[TaskList] 使用扫码登录的Session Token")
                    var result: Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>>? = null
                    try {
                        result = qrCodeRepository.getAllTasksWithCookies(currentCookies, initialDisplayCount)
                    } catch (e: Exception) {
                        val errMsg = e.message ?: ""
                        if (errMsg.contains("401") || errMsg.contains("未登录") || errMsg.contains("过期")) {
                            println("[TaskList] 任务请求返回登录已失效(401)，尝试强制刷新 Session...")
                            val refreshResult = qrCodeRepository.refreshSessionIfExpired(account)
                            if (refreshResult.isSuccess) {
                                currentCookies = refreshResult.getOrThrow()
                                result = qrCodeRepository.getAllTasksWithCookies(currentCookies, initialDisplayCount)
                            } else {
                                throw e
                            }
                        } else {
                            throw e
                        }
                    }
                    result ?: Triple(emptyList(), emptyList(), emptyList())
                } else {
                    // 密码登录：检查是否需要重新登录
                    if (loggedInPasswordStudentId != account.studentId) {
                        currentPasswordLoginEntry = PasswordLoginEntry.TASKS
                        println("[TaskList] 密码登录账号，需要登录 (当前=${loggedInPasswordStudentId}, 需要=${account.studentId})")
                        _uiState.update { it.copy(successMessage = "正在自动登录...") }
                        val loginSuccess = autoLoginForPasswordAccount(account)
                        if (!loginSuccess) {
                            if (passwordRepository.hasPendingSmsChallenge()) {
                                _uiState.update { it.copy(isLoadingTasks = false) }
                                showSmsVerificationDialog(account, PasswordLoginEntry.TASKS)
                                return@launch
                            }

                            _uiState.update { it.copy(isLoadingTasks = false) }
                            showManualCaptchaDialog(
                                account = account,
                                errorMessage = "自动识别失败或验证码已过期",
                                entry = PasswordLoginEntry.TASKS
                            )
                            return@launch
                        }

                        loggedInPasswordStudentId = account.studentId
                        println("[TaskList] 自动登录成功")
                    } else {
                        println("[TaskList] 密码登录账号，已登录，直接加载任务列表")
                    }
                    passwordRepository.getAllTasks(initialDisplayCount)
                }

                _uiState.update {
                    it.copy(
                        isLoadingTasks = false,
                        pendingTasks = pending,
                        completedTasks = completed,
                        absentTasks = absent,
                        displayedCompletedCount = initialDisplayCount,
                        successMessage = "加载成功：${pending.size}个待打卡，${completed.size}个已打卡，${absent.size}个缺勤"
                    )
                }
            } catch (e: Exception) {
                println("[TaskList] 加载失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoadingTasks = false,
                        errorMessage = "加载任务失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 清除任务列表（返回账号列表）
     */
    fun clearTasks() {
        _uiState.update {
            it.copy(
                selectedAccount = null,
                pendingTasks = emptyList(),
                completedTasks = emptyList(),
                absentTasks = emptyList(),
                displayedCompletedCount = 6
            )
        }
    }

    /**
     * 加载更多已打卡任务（显示更多 + 加载打卡时间）
     * 每次加载 6 个
     */
    fun loadMoreCompletedTasks() {
        val state = _uiState.value
        val account = state.selectedAccount ?: return

        // 如果已经显示全部，不再加载
        if (state.displayedCompletedCount >= state.completedTasks.size || state.isLoadingMoreCompleted) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreCompleted = true) }

            try {
                val loadCount = 6
                val startIndex = state.displayedCompletedCount
                val endIndex = minOf(startIndex + loadCount, state.completedTasks.size)

                // 为新显示的任务加载打卡时间
                val updatedTasks = if (account.isQrCodeLogin) {
                    var currentCookies = account.sessionToken ?: ""
                    var isSessionOk = account.isSessionValid()
                    if (!isSessionOk) {
                        val refreshResult = qrCodeRepository.refreshSessionIfExpired(account)
                        if (refreshResult.isSuccess) {
                            currentCookies = refreshResult.getOrThrow()
                            isSessionOk = true
                        }
                    }
                    if (isSessionOk) {
                        qrCodeRepository.loadCheckinTimeForTasks(
                            tasks = state.completedTasks,
                            startIndex = startIndex,
                            endIndex = endIndex,
                            cookies = currentCookies
                        ).getOrNull() ?: state.completedTasks
                    } else {
                        state.completedTasks
                    }
                } else {
                    // 密码登录：使用 cookie storage 内部方法
                    passwordRepository.loadCheckinTimeForTasksInternal(
                        tasks = state.completedTasks,
                        startIndex = startIndex,
                        endIndex = endIndex
                    ).getOrNull() ?: state.completedTasks
                }

                _uiState.update {
                    it.copy(
                        completedTasks = updatedTasks,
                        displayedCompletedCount = endIndex,
                        isLoadingMoreCompleted = false
                    )
                }
            } catch (e: Exception) {
                println("[TaskList] 加载更多失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoadingMoreCompleted = false,
                        errorMessage = "加载更多失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 对指定任务执行打卡
     * @param task 要打卡的任务
     * @param allowRepeat 是否允许重复打卡（对已打卡的任务）
     */
    fun checkinForTask(task: CheckinTask, allowRepeat: Boolean = true) {
        val account = _uiState.value.selectedAccount
        if (account == null) {
            _uiState.update { it.copy(errorMessage = "请先选择账号") }
            return
        }

        // 如果任务在已打卡列表中且不允许重复，提示用户
        if (!allowRepeat && _uiState.value.completedTasks.any { it.id == task.id }) {
            _uiState.update { it.copy(errorMessage = "该任务已打卡，不可重复打卡") }
            return
        }

        viewModelScope.launch {
            // 仅标记当前正在打卡的任务ID，不影响全局isLoading
            _uiState.update {
                it.copy(checkingTaskId = task.id)
            }

            var currentAccount = account
            if (account.isQrCodeLogin) {
                var isSessionOk = account.isSessionValid()
                if (!isSessionOk) {
                    println("[CheckinForTask] 扫码登录 Session 已过期，尝试自动刷新...")
                    _uiState.update { it.copy(successMessage = "正在自动更新登录凭证...") }
                    val refreshResult = qrCodeRepository.refreshSessionIfExpired(account)
                    if (refreshResult.isSuccess) {
                        isSessionOk = true
                        val updatedAccount = passwordRepository.getAccountById(account.id)
                        if (updatedAccount != null) {
                            currentAccount = updatedAccount
                        }
                    }
                }
                
                if (!isSessionOk) {
                    _uiState.update {
                        it.copy(
                            checkingTaskId = null,
                            accountNeedRelogin = account,
                            showReloginDialog = true
                        )
                    }
                    return@launch
                }
            }

            val result = if (currentAccount.isQrCodeLogin) {
                // 扫码登录：使用session cookies
                val sessionToken = currentAccount.sessionToken ?: ""
                val cookies = if (sessionToken.contains(";") || sessionToken.contains("=")) {
                    sessionToken
                } else {
                    "SESSION=$sessionToken"
                }
                qrCodeRepository.checkinForSpecificTask(
                    cookies = cookies,
                    taskId = task.id,
                    account = currentAccount
                )
            } else {
                // 密码登录：检查是否需要登录
                if (loggedInPasswordStudentId != account.studentId) {
                    println("[CheckinForTask] 密码登录账号，需要登录...")
                    val loginSuccess = autoLoginForPasswordAccount(account)
                    if (!loginSuccess) {
                        _uiState.update {
                            it.copy(
                                checkingTaskId = null,
                                errorMessage = "自动登录失败，无法执行打卡"
                            )
                        }
                        return@launch
                    }
                    loggedInPasswordStudentId = account.studentId
                }
                // 对选中的特定任务打卡
                passwordRepository.checkinForSpecificTaskInternal(
                    taskId = task.id,
                    account = account
                )
            }

            val message = when (result) {
                is CheckinResult.Success -> result.message
                is CheckinResult.AlreadyChecked -> result.message
                is CheckinResult.NoTask -> result.message
                is CheckinResult.Failed -> result.error
            }

            _uiState.update {
                it.copy(
                    checkingTaskId = null,
                    successMessage = if (result is CheckinResult.Failed) null else message,
                    errorMessage = if (result is CheckinResult.Failed) message else null
                )
            }

            // 刷新任务列表
            if (result is CheckinResult.Success || result is CheckinResult.AlreadyChecked) {
                delay(500)
                loadTasksForAccount(account)
            }

            loadAccounts()
        }
    }

    // ==================== 旧的扫码登录相关操作（保留兼容）====================

    /**
     * 显示扫码添加账号对话框（旧方式）
     */
    fun showQrCodeDialog() {
        // 改为使用 WebView 方式
        showWebViewLoginDialog()
    }

    /**
     * 隐藏扫码对话框
     */
    @Suppress("unused")
    fun hideQrCodeDialog() {
        // 取消轮询
        scanPollingJob?.cancel()

        _uiState.update {
            it.copy(
                showQrCodeDialog = false,
                qrCodeUrl = null,
                qrCodeClientId = null,
                isLoadingQrCode = false,
                qrCodeScanStatus = QrCodeScanStatus.WAITING,
                scannedUserInfo = null
            )
        }
    }

    /**
     * 获取扫码登录二维码（旧方式，保留但不再使用）
     */
    private fun fetchQrCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQrCode = true) }

            // 1. 获取 ClientId
            val clientIdResult = qrCodeRepository.getClientId()
            if (clientIdResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoadingQrCode = false,
                        errorMessage = "获取 ClientId 失败: ${clientIdResult.exceptionOrNull()?.message}",
                        qrCodeScanStatus = QrCodeScanStatus.ERROR
                    )
                }
                return@launch
            }

            val clientId = clientIdResult.getOrThrow()

            // 2. 获取二维码 URL
            val qrCodeResult = qrCodeRepository.getQrCodeImage(clientId)
            if (qrCodeResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoadingQrCode = false,
                        errorMessage = "获取二维码失败: ${qrCodeResult.exceptionOrNull()?.message}",
                        qrCodeScanStatus = QrCodeScanStatus.ERROR
                    )
                }
                return@launch
            }

            val qrCodeUrl = qrCodeResult.getOrThrow()

            _uiState.update {
                it.copy(
                    isLoadingQrCode = false,
                    qrCodeUrl = qrCodeUrl,
                    qrCodeClientId = clientId,
                    qrCodeScanStatus = QrCodeScanStatus.WAITING
                )
            }

            // 注意：由于微信扫码登录需要通过 WebView 回调来设置 Session，
            // 在原生 App 中无法自动检测扫码状态。
            // 用户需要手动输入学号来添加账号。
        }
    }

    /**
     * 刷新二维码
     */
    @Suppress("unused")
    fun refreshQrCode() {
        // 取消轮询
        scanPollingJob?.cancel()

        _uiState.update {
            it.copy(
                qrCodeUrl = null,
                qrCodeClientId = null,
                qrCodeScanStatus = QrCodeScanStatus.WAITING,
                scannedUserInfo = null
            )
        }
        fetchQrCode()
    }

    /**
     * 确认扫码登录并添加账号
     * 扫码成功后自动获取用户信息，用户可以直接确认添加
     */
    @Suppress("unused")
    fun confirmQrCodeLogin(
        studentId: String,
        name: String,
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name
    ) {
        viewModelScope.launch {
            // 取消轮询
            scanPollingJob?.cancel()

            val finalStudentId = studentId.ifBlank {
                _uiState.value.scannedUserInfo?.code ?: ""
            }
            val finalName = name.ifBlank {
                _uiState.value.scannedUserInfo?.name ?: ""
            }

            if (finalStudentId.isBlank()) {
                _uiState.update { it.copy(errorMessage = "学号不能为空") }
                return@launch
            }

            // 检查账号是否已存在
            if (passwordRepository.isAccountExists(finalStudentId)) {
                _uiState.update { it.copy(errorMessage = "该学号账号已存在") }
                return@launch
            }

            // 创建扫码登录账号
            val now = com.suseoaa.projectoaa.shared.util.OaaClock.now()
                .toLocalDateTime(TimeZone.of("Asia/Shanghai"))
            val expireTime = "${now.date.plus(DatePeriod(days = 7))} ${now.time}"

            // 扫码登录成功后，Session 已经存储在 HttpClient 的 Cookie 中
            // 这里先保存账号，Session 会在签到时自动使用
            val result = qrCodeRepository.saveQrCodeAccount(
                studentId = finalStudentId,
                name = finalName,
                sessionToken = "COOKIE_SESSION", // 标记使用 Cookie 中的 Session
                sessionExpireTime = expireTime,
                selectedLocation = selectedLocation
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        successMessage = "账号添加成功！",
                        showQrCodeDialog = false,
                        qrCodeUrl = null,
                        qrCodeClientId = null,
                        scannedUserInfo = null
                    )
                }
                loadAccounts()
            } else {
                _uiState.update {
                    it.copy(errorMessage = "添加失败: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    /**
     * 隐藏重新登录对话框
     */
    fun hideReloginDialog() {
        _uiState.update {
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
        val account = _uiState.value.accountNeedRelogin ?: return
        _uiState.update {
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
        val account = _uiState.value.currentCheckingAccount ?: return
        viewModelScope.launch {
            val cookieString = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            val now = com.suseoaa.projectoaa.shared.util.OaaClock.now()
                .toLocalDateTime(TimeZone.of("Asia/Shanghai"))
            val expireTime = "${now.date.plus(DatePeriod(days = 7))} ${now.time}"

            val result = passwordRepository.updateSession(account.id, cookieString, expireTime)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        successMessage = "重新登录成功",
                        showWebViewLoginDialog = false,
                        currentCheckingAccount = null
                    )
                }
                loadAccounts()
            } else {
                _uiState.update {
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
        val account = _uiState.value.currentCheckingAccount ?: return
        viewModelScope.launch {
            val result =
                passwordRepository.updateSession(account.id, sessionToken, sessionExpireTime)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        successMessage = "登录成功",
                        showQrCodeDialog = false,
                        qrCodeUrl = null,
                        qrCodeClientId = null,
                        currentCheckingAccount = null
                    )
                }
                loadAccounts()
            } else {
                _uiState.update {
                    it.copy(errorMessage = "更新Session失败: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    /**
     * 更新签到地点
     */
    @Suppress("unused")
    fun updateLocation(accountId: Long, locationName: String) {
        viewModelScope.launch {
            val result = passwordRepository.updateLocation(accountId, locationName)
            if (result.isSuccess) {
                _uiState.update { it.copy(successMessage = "签到地点已更新") }
                loadAccounts()
            } else {
                _uiState.update { it.copy(errorMessage = "更新失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    override fun onCleared() {
        scanPollingJob?.cancel()
        stopSmsResendCountdown()
        super.onCleared()
    }
}
