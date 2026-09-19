package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTask
import com.suseoaa.projectoaa.shared.domain.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.domain.repository.QrCodeCheckinRepository
import com.suseoaa.projectoaa.shared.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 任务流程需要从登录流程拿到的东西。
 *
 * 拉取任务前必须确保已登录，而登录可能中途要求图形验证码或短信二次验证——
 * 这就是任务与登录两条流程无法各自独立的根源。把这份耦合显式声明成接口，
 * 至少让"任务流程依赖登录流程的哪几件事"变成一目了然的四条，而不是散落在
 * 三百行里的隐式读写。
 */
internal interface CheckinSessionState {
    /** cookie 中当前已登录的密码账号学号；null 表示未登录。 */
    var loggedInStudentId: String?

    /** 当前登录入口，验证码/短信通过后据此决定接着做什么。 */
    var loginEntry: PasswordLoginEntry
}

internal interface CheckinLoginGateway : CheckinSessionState {
    /** 尝试自动登录（含 OCR 识别验证码），返回是否成功。 */
    suspend fun autoLogin(account: CheckinAccountData): Boolean

    fun requireSmsVerification(account: CheckinAccountData, entry: PasswordLoginEntry)

    fun requireCaptcha(
        account: CheckinAccountData,
        errorMessage: String?,
        existingCaptchaBytes: ByteArray? = null,
        entry: PasswordLoginEntry = loginEntry,
    )

    /** 打卡结果会改变账号的最后签到状态，需要刷新账号列表。 */
    fun refreshAccounts()
}

/**
 * 签到任务列表的加载、分页与逐项打卡。
 *
 * 按登录方式分叉：扫码账号靠 Session（过期先刷新，刷不出来要求重新扫码），
 * 密码账号要先自动登录（失败回落到验证码或短信）。这些分支已由
 * CheckinTaskLoadingTest 覆盖。
 */
internal class CheckinTasksDelegate(
    private val uiState: MutableStateFlow<CheckinUiState>,
    private val scope: CoroutineScope,
    private val passwordRepository: CheckinRepository,
    private val qrCodeRepository: QrCodeCheckinRepository,
    private val login: CheckinLoginGateway,
) {

    /**
     * 加载指定账号的打卡任务列表
     * @param account 要查看任务的账号
     */
    fun loadTasksForAccount(account: CheckinAccountData) {
        scope.launch {
            val initialDisplayCount = 6  // 初始显示的已打卡任务数量

            uiState.update {
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
                        AppLog.d("[TaskList] 扫码登录 Session 已过期，尝试自动刷新...")
                        val refreshResult = qrCodeRepository.refreshSessionIfExpired(account)
                        if (refreshResult.isSuccess) {
                            currentCookies = refreshResult.getOrThrow()
                            isSessionOk = true
                        }
                    }

                    if (!isSessionOk) {
                        uiState.update {
                            it.copy(
                                isLoadingTasks = false,
                                accountNeedRelogin = account,
                                showReloginDialog = true
                            )
                        }
                        return@launch
                    }

                    AppLog.d("[TaskList] 使用扫码登录的Session Token")
                    var result: Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>>? = null
                    try {
                        result = qrCodeRepository.getAllTasksWithCookies(currentCookies, initialDisplayCount)
                    } catch (e: Exception) {
                        val errMsg = e.message ?: ""
                        if (errMsg.contains("401") || errMsg.contains("未登录") || errMsg.contains("过期")) {
                            AppLog.d("[TaskList] 任务请求返回登录已失效(401)，尝试强制刷新 Session...")
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
                    if (login.loggedInStudentId != account.studentId) {
                        login.loginEntry = PasswordLoginEntry.TASKS
                        AppLog.d("[TaskList] 密码登录账号，需要登录 (当前=${login.loggedInStudentId}, 需要=${account.studentId})")
                        uiState.update { it.copy(successMessage = "正在自动登录...") }
                        val loginSuccess = login.autoLogin(account)
                        if (!loginSuccess) {
                            if (passwordRepository.hasPendingSmsChallenge()) {
                                uiState.update { it.copy(isLoadingTasks = false) }
                                login.requireSmsVerification(account, PasswordLoginEntry.TASKS)
                                return@launch
                            }

                            uiState.update { it.copy(isLoadingTasks = false) }
                            login.requireCaptcha(
                                account = account,
                                errorMessage = "自动识别失败或验证码已过期",
                                entry = PasswordLoginEntry.TASKS
                            )
                            return@launch
                        }

                        login.loggedInStudentId = account.studentId
                        AppLog.d("[TaskList] 自动登录成功")
                    } else {
                        AppLog.d("[TaskList] 密码登录账号，已登录，直接加载任务列表")
                    }
                    passwordRepository.getAllTasks(initialDisplayCount)
                }

                uiState.update {
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
                AppLog.e("[TaskList] 加载失败: ${e.message}")
                uiState.update {
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
        uiState.update {
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
        val state = uiState.value
        val account = state.selectedAccount ?: return

        // 如果已经显示全部，不再加载
        if (state.displayedCompletedCount >= state.completedTasks.size || state.isLoadingMoreCompleted) {
            return
        }

        scope.launch {
            uiState.update { it.copy(isLoadingMoreCompleted = true) }

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

                uiState.update {
                    it.copy(
                        completedTasks = updatedTasks,
                        displayedCompletedCount = endIndex,
                        isLoadingMoreCompleted = false
                    )
                }
            } catch (e: Exception) {
                AppLog.e("[TaskList] 加载更多失败: ${e.message}")
                uiState.update {
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
        val account = uiState.value.selectedAccount
        if (account == null) {
            uiState.update { it.copy(errorMessage = "请先选择账号") }
            return
        }

        // 如果任务在已打卡列表中且不允许重复，提示用户
        if (!allowRepeat && uiState.value.completedTasks.any { it.id == task.id }) {
            uiState.update { it.copy(errorMessage = "该任务已打卡，不可重复打卡") }
            return
        }

        scope.launch {
            // 仅标记当前正在打卡的任务ID，不影响全局isLoading
            uiState.update {
                it.copy(checkingTaskId = task.id)
            }

            var currentAccount = account
            if (account.isQrCodeLogin) {
                var isSessionOk = account.isSessionValid()
                if (!isSessionOk) {
                    AppLog.d("[CheckinForTask] 扫码登录 Session 已过期，尝试自动刷新...")
                    uiState.update { it.copy(successMessage = "正在自动更新登录凭证...") }
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
                    uiState.update {
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
                if (login.loggedInStudentId != account.studentId) {
                    AppLog.d("[CheckinForTask] 密码登录账号，需要登录...")
                    val loginSuccess = login.autoLogin(account)
                    if (!loginSuccess) {
                        uiState.update {
                            it.copy(
                                checkingTaskId = null,
                                errorMessage = "自动登录失败，无法执行打卡"
                            )
                        }
                        return@launch
                    }
                    login.loggedInStudentId = account.studentId
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

            uiState.update {
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

            login.refreshAccounts()
        }
    }}
