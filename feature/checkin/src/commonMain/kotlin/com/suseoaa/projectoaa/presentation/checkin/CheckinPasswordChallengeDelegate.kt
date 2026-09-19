package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
import com.suseoaa.projectoaa.shared.domain.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 密码登录的二次验证：图形验证码与短信验证码。
 *
 * 两条路互为回退——验证码登录被判定需要短信时切到短信对话框，短信取消后又能回到
 * 验证码；登录成功后还要按 [CheckinSessionState.loginEntry] 决定是继续打卡还是去加载
 * 任务列表。这是 CheckinViewModel 里状态最绕的一段，分支由
 * CheckinPasswordChallengeTest 覆盖。
 *
 * 短信重发倒计时的 Job 由本类独占，[onCleared] 时要停掉。
 */
internal class CheckinPasswordChallengeDelegate(
    private val uiState: MutableStateFlow<CheckinUiState>,
    private val scope: CoroutineScope,
    private val passwordRepository: CheckinRepository,
    private val session: CheckinSessionState,
    private val loadTasks: (CheckinAccountData) -> Unit,
    private val refreshAccounts: () -> Unit,
) {

    private var smsResendCountdownJob: Job? = null

    private companion object {
        const val SMS_RESEND_COUNTDOWN_SECONDS = 30
    }

    /** 取消打卡等场景下主动终止重发倒计时。 */
    fun stopResendCountdown() = stopSmsResendCountdown()

    /** ViewModel 销毁时调用，避免倒计时协程泄漏。 */
    fun dispose() = stopSmsResendCountdown()

    /**
     * 显示手动输入验证码对话框
     */
    fun showManualCaptchaDialog(
        account: CheckinAccountData,
        errorMessage: String?,
        existingCaptchaBytes: ByteArray? = null,
        entry: PasswordLoginEntry = session.loginEntry
    ) {
        session.loginEntry = entry
        uiState.update {
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

    fun showSmsVerificationDialog(account: CheckinAccountData) {
        showSmsVerificationDialog(account, session.loginEntry)
    }

    fun showSmsVerificationDialog(
        account: CheckinAccountData,
        entry: PasswordLoginEntry
    ) {
        stopSmsResendCountdown()
        session.loginEntry = entry
        uiState.update {
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
     * 刷新验证码
     */
    fun refreshCaptcha() {
        scope.launch {
            uiState.update { it.copy(isLoadingCaptcha = true) }

            val result = passwordRepository.fetchCaptchaImage()
            if (result.isSuccess) {
                uiState.update {
                    it.copy(
                        captchaImageBytes = result.getOrNull(),
                        isLoadingCaptcha = false
                    )
                }
            } else {
                uiState.update {
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
        val account = uiState.value.currentCheckingAccount ?: return

        if (captchaCode.isBlank()) {
            uiState.update { it.copy(errorMessage = "请输入验证码") }
            return
        }

        scope.launch {
            uiState.update { it.copy(isLoggingIn = true) }

            // 1. 登录
            val loginResult = passwordRepository.loginWithCaptcha(
                username = account.studentId,
                password = account.password,
                captchaCode = captchaCode,
                accountId = account.id
            )

            if (loginResult.isFailure) {
                if (passwordRepository.isSmsVerificationRequired(loginResult.exceptionOrNull())) {
                    uiState.update { it.copy(isLoggingIn = false) }
                    showSmsVerificationDialog(account)
                    return@launch
                }
                uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        errorMessage = loginResult.exceptionOrNull()?.message ?: "登录失败"
                    )
                }
                // 刷新验证码
                refreshCaptcha()
                return@launch
            }

            session.loggedInStudentId = account.studentId

            if (session.loginEntry == PasswordLoginEntry.TASKS) {
                uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        showCaptchaDialog = false,
                        currentCheckingAccount = null,
                        captchaImageBytes = null,
                        successMessage = "登录成功，正在加载任务列表..."
                    )
                }
                loadTasks(account)
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

            uiState.update {
                it.copy(
                    isLoggingIn = false,
                    showCaptchaDialog = false,
                    currentCheckingAccount = null,
                    captchaImageBytes = null
                )
            }
            refreshAccounts() // 刷新状态
        }
    }

    fun sendSmsCode() {
        val state = uiState.value
        if (state.isVerifyingSmsCode || state.isSendingSmsCode || state.smsResendCountdownSeconds > 0) {
            return
        }

        startSmsResendCountdown()

        scope.launch {
            uiState.update { it.copy(isSendingSmsCode = true) }

            val result = passwordRepository.sendSmsCodeForPendingLogin()
            if (result.isFailure) {
                AppLog.e("[SmsVerification] sendSmsCode failed: ${result.exceptionOrNull()?.message}")
            }

            uiState.update { it.copy(isSendingSmsCode = false) }
        }
    }

    private fun startSmsResendCountdown() {
        stopSmsResendCountdown()
        uiState.update { it.copy(smsResendCountdownSeconds = SMS_RESEND_COUNTDOWN_SECONDS) }

        smsResendCountdownJob = scope.launch {
            for (remaining in (SMS_RESEND_COUNTDOWN_SECONDS - 1) downTo 0) {
                delay(1000)
                uiState.update { it.copy(smsResendCountdownSeconds = remaining) }
            }
        }
    }

    private fun stopSmsResendCountdown() {
        smsResendCountdownJob?.cancel()
        smsResendCountdownJob = null
    }

    fun submitSmsCodeAndCheckin(smsCode: String) {
        val account = uiState.value.currentCheckingAccount ?: return
        if (smsCode.isBlank()) {
            uiState.update { it.copy(errorMessage = "请输入短信验证码") }
            return
        }

        scope.launch {
            uiState.update { it.copy(isVerifyingSmsCode = true) }

            val verifyResult = passwordRepository.submitSmsCodeForPendingLogin(smsCode)
            if (verifyResult.isFailure) {
                uiState.update {
                    it.copy(
                        isVerifyingSmsCode = false,
                        errorMessage = verifyResult.exceptionOrNull()?.message ?: "短信验证失败"
                    )
                }
                return@launch
            }

            session.loggedInStudentId = account.studentId

            if (session.loginEntry == PasswordLoginEntry.TASKS) {
                stopSmsResendCountdown()
                uiState.update {
                    it.copy(
                        isVerifyingSmsCode = false,
                        showSmsDialog = false,
                        smsMaskedPhone = null,
                        smsResendCountdownSeconds = 0,
                        currentCheckingAccount = null,
                        successMessage = "登录成功，正在加载任务列表..."
                    )
                }
                loadTasks(account)
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
            uiState.update {
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
            refreshAccounts()
        }
    }

    fun cancelSmsVerification() {
        passwordRepository.clearPendingSmsChallenge()
        stopSmsResendCountdown()
        val fromTasks = session.loginEntry == PasswordLoginEntry.TASKS
        uiState.update {
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
        session.loginEntry = PasswordLoginEntry.CHECKIN
    }}
