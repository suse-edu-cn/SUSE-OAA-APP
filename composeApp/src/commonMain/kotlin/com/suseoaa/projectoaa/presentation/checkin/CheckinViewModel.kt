package com.suseoaa.projectoaa.presentation.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.data.model.CheckinAccountData
import com.suseoaa.projectoaa.data.model.CheckinLocations
import com.suseoaa.projectoaa.data.model.CheckinResult
import com.suseoaa.projectoaa.data.model.CheckinTask
import com.suseoaa.projectoaa.data.model.EduUserInfo
import com.suseoaa.projectoaa.data.repository.CheckinRepository
import com.suseoaa.projectoaa.data.repository.QrCodeCheckinRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 账号筛选类型
 */
enum class AccountFilterType {
    ALL,           // 全部账号
    PASSWORD,      // 账号密码登录
    QRCODE         // 扫码登录
}

/**
 * 652打卡 UI 状态
 */
data class CheckinUiState(
    val accounts: List<CheckinAccountData> = emptyList(),
    val accountFilter: AccountFilterType = AccountFilterType.ALL,  // 账号筛选
    val isLoading: Boolean = false,
    val currentCheckingAccount: CheckinAccountData? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // 任务列表
    val pendingTasks: List<CheckinTask> = emptyList(),      // 待打卡任务
    val completedTasks: List<CheckinTask> = emptyList(),    // 已打卡任务
    val absentTasks: List<CheckinTask> = emptyList(),       // 缺勤任务（未打卡）
    val isLoadingTasks: Boolean = false,
    val selectedAccount: CheckinAccountData? = null,        // 当前查看任务的账号
    // 已打卡任务分页显示状态
    val displayedCompletedCount: Int = 6,                   // 当前显示的已打卡任务数量
    val isLoadingMoreCompleted: Boolean = false,            // 是否正在加载更多
    // 编辑对话框状态
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingAccount: CheckinAccountData? = null,
    // 验证码对话框状态（密码登录）
    val showCaptchaDialog: Boolean = false,
    val captchaImageBytes: ByteArray? = null,
    val isLoadingCaptcha: Boolean = false,
    val isLoggingIn: Boolean = false,
    // 扫码登录对话框状态
    val showQrCodeDialog: Boolean = false,
    val qrCodeImage: String? = null,         // 二维码图片 (Base64)
    val qrCodeClientId: String? = null,      // 用于轮询的 ClientId
    val isLoadingQrCode: Boolean = false,
    val qrCodeScanStatus: QrCodeScanStatus = QrCodeScanStatus.WAITING,
    val scannedStudentId: String? = null,    // 扫码后获取的学号
    val scannedName: String? = null,         // 扫码后获取的姓名
    val scannedCookies: String? = null,      // 扫码登录后的完整 Cookie
    // 需要重新扫码登录的账号（Session过期）
    val accountNeedRelogin: CheckinAccountData? = null,
    val showReloginDialog: Boolean = false,
    // WebView 扫码登录对话框状态 (保留兼容)
    val showWebViewLoginDialog: Boolean = false,
    val qrCodeUrl: String? = null,           // 旧字段，保留兼容
    val scannedUserInfo: EduUserInfo? = null // 旧字段，保留兼容
)

/**
 * 二维码扫描状态
 */
enum class QrCodeScanStatus {
    WAITING,    // 等待扫描
    SCANNED,    // 已扫描，等待确认
    CONFIRMED,  // 已确认
    EXPIRED,    // 已过期
    ERROR       // 错误
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
    private val qrCodeRepository: QrCodeCheckinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckinUiState())
    val uiState: StateFlow<CheckinUiState> = _uiState.asStateFlow()
    
    // 轮询扫码状态的 Job
    private var scanPollingJob: Job? = null

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
    fun getFilteredAccounts(): List<CheckinAccountData> {
        val state = _uiState.value
        return when (state.accountFilter) {
            AccountFilterType.ALL -> state.accounts
            AccountFilterType.PASSWORD -> state.accounts.filter { !it.isQrCodeLogin }
            AccountFilterType.QRCODE -> state.accounts.filter { it.isQrCodeLogin }
        }
    }

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
                    val result = performAutoCheckinSync(account)
                    if (result) {
                        successCount++
                    } else {
                        failCount++
                    }
                    
                    // 每个账号之间稍作延迟，避免请求过快
                    delay(500)
                } catch (e: Exception) {
                    failCount++
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
    private suspend fun performAutoCheckinSync(account: CheckinAccountData, retryCount: Int = 0): Boolean {
        try {
            // 1. 获取验证码图片
            val captchaResult = passwordRepository.fetchCaptchaImage()
            if (captchaResult.isFailure) {
                return false
            }
            
            val captchaBytes = captchaResult.getOrThrow()
            
            // 2. OCR自动识别
            val ocrResult = com.suseoaa.projectoaa.util.PlatformCaptchaOcr.recognize(captchaBytes)
            
            if (ocrResult.isFailure || ocrResult.getOrNull()?.length != 4) {
                return false
            }
            
            val captchaCode = ocrResult.getOrThrow()
            
            // 3. 自动登录
            val loginResult = passwordRepository.loginWithCaptcha(
                username = account.studentId,
                password = account.password,
                captchaCode = captchaCode
            )
            
            if (loginResult.isFailure) {
                val errorMsg = loginResult.exceptionOrNull()?.message ?: ""
                // 验证码错误，最多重试2次
                if ((errorMsg.contains("验证码") || errorMsg.contains("captcha", ignoreCase = true)) && retryCount < 2) {
                    return performAutoCheckinSync(account, retryCount + 1)
                }
                return false
            }
            
            // 4. 执行打卡
            val checkinResult = passwordRepository.performCheckinAfterLogin(account)
            return when (checkinResult) {
                is CheckinResult.Success -> true
                is CheckinResult.AlreadyChecked -> true
                is CheckinResult.NoTask -> true
                is CheckinResult.Failed -> false
            }
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 添加账号（密码登录）
     */
    fun addAccount(studentId: String, password: String, name: String = "", remark: String = "") {
        viewModelScope.launch {
            if (studentId.isBlank() || password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "学号和密码不能为空") }
                return@launch
            }

            if (passwordRepository.isAccountExists(studentId)) {
                _uiState.update { it.copy(errorMessage = "该学号已存在") }
                return@launch
            }

            val result = passwordRepository.addAccount(studentId, password, name, remark)
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
    fun updateAccount(id: Long, studentId: String, password: String, name: String, remark: String, selectedLocation: String = CheckinLocations.DEFAULT.name) {
        viewModelScope.launch {
            if (studentId.isBlank() || password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "学号和密码不能为空") }
                return@launch
            }

            val result = passwordRepository.updateAccount(id, studentId, password, name, remark, selectedLocation)
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
            // 扫码登录账号 - 检查Session是否有效
            if (account.isSessionValid()) {
                // Session 有效，直接签到
                performQrCodeCheckin(account)
            } else {
                // Session 过期，提示重新扫码
                _uiState.update {
                    it.copy(
                        accountNeedRelogin = account,
                        showReloginDialog = true
                    )
                }
            }
        } else {
            // 密码登录账号 - 自动尝试OCR识别并打卡
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
            _uiState.update { it.copy(isLoading = true, currentCheckingAccount = account) }
            
            try {
                // 1. 获取验证码图片
                val captchaResult = passwordRepository.fetchCaptchaImage()
                if (captchaResult.isFailure) {
                    // 获取验证码失败，显示手动输入对话框
                    showManualCaptchaDialog(account, captchaResult.exceptionOrNull()?.message)
                    return@launch
                }
                
                val captchaBytes = captchaResult.getOrThrow()
                
                // 2. OCR自动识别
                val ocrResult = com.suseoaa.projectoaa.util.PlatformCaptchaOcr.recognize(captchaBytes)
                
                if (ocrResult.isFailure || ocrResult.getOrNull()?.length != 4) {
                    // OCR识别失败，显示手动输入对话框
                    println("[AutoCheckin] OCR识别失败: ${ocrResult.exceptionOrNull()?.message ?: "识别结果长度不正确"}")
                    showManualCaptchaDialog(account, null, captchaBytes)
                    return@launch
                }
                
                val captchaCode = ocrResult.getOrThrow()
                println("[AutoCheckin] OCR识别成功: $captchaCode")
                
                // 3. 自动登录
                val loginResult = passwordRepository.loginWithCaptcha(
                    username = account.studentId,
                    password = account.password,
                    captchaCode = captchaCode
                )
                
                if (loginResult.isFailure) {
                    val errorMsg = loginResult.exceptionOrNull()?.message ?: ""
                    // 验证码错误，最多重试2次
                    if ((errorMsg.contains("验证码") || errorMsg.contains("captcha", ignoreCase = true)) && retryCount < 2) {
                        println("[AutoCheckin] 验证码错误，重试第 ${retryCount + 1} 次")
                        _uiState.update { it.copy(isLoading = false, currentCheckingAccount = null) }
                        performAutoCheckin(account, retryCount + 1)
                        return@launch
                    }
                    // 其他登录错误或重试次数用尽，显示手动输入对话框
                    println("[AutoCheckin] 登录失败: $errorMsg")
                    showManualCaptchaDialog(account, errorMsg)
                    return@launch
                }
                
                // 4. 执行打卡
                val checkinResult = passwordRepository.performCheckinAfterLogin(account)
                val message = when (checkinResult) {
                    is CheckinResult.Success -> checkinResult.message
                    is CheckinResult.AlreadyChecked -> checkinResult.message
                    is CheckinResult.NoTask -> checkinResult.message
                    is CheckinResult.Failed -> checkinResult.error
                }
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentCheckingAccount = null,
                        successMessage = if (checkinResult is CheckinResult.Failed) null else message,
                        errorMessage = if (checkinResult is CheckinResult.Failed) message else null
                    )
                }
                loadAccounts()
                
            } catch (e: Exception) {
                println("[AutoCheckin] 异常: ${e.message}")
                showManualCaptchaDialog(account, e.message)
            }
        }
    }
    
    /**
     * 显示手动输入验证码对话框
     */
    private fun showManualCaptchaDialog(account: CheckinAccountData, errorMessage: String?, existingCaptchaBytes: ByteArray? = null) {
        _uiState.update {
            it.copy(
                isLoading = false,
                currentCheckingAccount = account,
                showCaptchaDialog = true,
                captchaImageBytes = existingCaptchaBytes,
                isLoadingCaptcha = existingCaptchaBytes == null,
                errorMessage = errorMessage?.let { msg -> "自动打卡失败: $msg，请手动输入" }
            )
        }
        // 如果没有现有验证码图片，获取新的
        if (existingCaptchaBytes == null) {
            refreshCaptcha()
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

            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentCheckingAccount = null,
                    successMessage = if (result is CheckinResult.Failed) null else message,
                    errorMessage = if (result is CheckinResult.Failed) message else null
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
                captchaCode = captchaCode
            )

            if (loginResult.isFailure) {
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

            // 2. 执行打卡
            val checkinResult = passwordRepository.performCheckinAfterLogin(account)
            val message = when (checkinResult) {
                is CheckinResult.Success -> checkinResult.message
                is CheckinResult.AlreadyChecked -> checkinResult.message
                is CheckinResult.NoTask -> checkinResult.message
                is CheckinResult.Failed -> checkinResult.error
            }

            _uiState.update {
                it.copy(
                    isLoggingIn = false,
                    showCaptchaDialog = false,
                    currentCheckingAccount = null,
                    captchaImageBytes = null,
                    successMessage = if (checkinResult is CheckinResult.Failed) null else message,
                    errorMessage = if (checkinResult is CheckinResult.Failed) message else null
                )
            }
            loadAccounts() // 刷新状态
        }
    }

    /**
     * 取消打卡
     */
    fun cancelCheckin() {
        _uiState.update {
            it.copy(
                showCaptchaDialog = false,
                currentCheckingAccount = null,
                captchaImageBytes = null,
                isLoadingCaptcha = false,
                isLoggingIn = false
            )
        }
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
                    studentId = userInfo.first
                    studentName = userInfo.second
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
            val now = Clock.System.now()
                .toLocalDateTime(TimeZone.of("Asia/Shanghai"))
            val expireTime = "${now.date.plus(kotlinx.datetime.DatePeriod(days = 7))} ${now.time}"
            
            val result = qrCodeRepository.saveQrCodeAccount(
                studentId = studentId,
                name = studentName,
                sessionToken = fullCookies,
                sessionExpireTime = expireTime,
                selectedLocation = CheckinLocations.DEFAULT.name
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
                val (pending, completed, absent) = if (account.isQrCodeLogin && account.isSessionValid()) {
                    // 扫码登录：使用已保存的sessionToken
                    println("[TaskList] 使用扫码登录的Session Token")
                    qrCodeRepository.getAllTasksWithCookies(account.sessionToken ?: "", initialDisplayCount)
                } else {
                    // 密码登录：使用cookieStorage中的当前cookies
                    // 假设用户已经通过打卡操作登录过了
                    println("[TaskList] 使用cookieStorage中的cookies")
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
                val updatedTasks = if (account.isQrCodeLogin && account.isSessionValid()) {
                    val cookies = account.sessionToken ?: ""
                    qrCodeRepository.loadCheckinTimeForTasks(
                        tasks = state.completedTasks,
                        startIndex = startIndex,
                        endIndex = endIndex,
                        cookies = cookies
                    ).getOrNull() ?: state.completedTasks
                } else {
                    passwordRepository.loadCheckinTimeForTasks(
                        tasks = state.completedTasks,
                        startIndex = startIndex,
                        endIndex = endIndex,
                        cookies = ""
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
        if (account == null || !account.isSessionValid()) {
            _uiState.update { it.copy(errorMessage = "账号Session无效，请重新登录") }
            return
        }
        
        // 如果任务在已打卡列表中且不允许重复，提示用户
        if (!allowRepeat && _uiState.value.completedTasks.any { it.id == task.id }) {
            _uiState.update { it.copy(errorMessage = "该任务已打卡，不可重复打卡") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    currentCheckingAccount = account
                )
            }
            
            // 使用特定任务打卡方法（支持再次打卡）
            val sessionToken = account.sessionToken ?: ""
            val cookies = if (sessionToken.contains(";") || sessionToken.contains("=")) {
                sessionToken  // 完整的Cookie字符串
            } else {
                "SESSION=$sessionToken"  // 单独的SESSION值
            }
            
            val result = qrCodeRepository.checkinForSpecificTask(
                cookies = cookies,
                taskId = task.id,
                account = account
            )
            
            val message = when (result) {
                is CheckinResult.Success -> result.message
                is CheckinResult.AlreadyChecked -> result.message
                is CheckinResult.NoTask -> result.message
                is CheckinResult.Failed -> result.error
            }
            
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentCheckingAccount = null,
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
    fun confirmQrCodeLogin(studentId: String, name: String, selectedLocation: String = CheckinLocations.DEFAULT.name) {
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
            val now = Clock.System.now()
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
    fun onReloginSuccess(cookies: Map<String, String>) {
        val account = _uiState.value.currentCheckingAccount ?: return
        viewModelScope.launch {
            val cookieString = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            val now = Clock.System.now()
                .toLocalDateTime(TimeZone.of("Asia/Shanghai"))
            val expireTime = "${now.date.plus(kotlinx.datetime.DatePeriod(days = 7))} ${now.time}"
            
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
    fun updateAccountSession(sessionToken: String, sessionExpireTime: String) {
        val account = _uiState.value.currentCheckingAccount ?: return
        viewModelScope.launch {
            val result = passwordRepository.updateSession(account.id, sessionToken, sessionExpireTime)
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
}
