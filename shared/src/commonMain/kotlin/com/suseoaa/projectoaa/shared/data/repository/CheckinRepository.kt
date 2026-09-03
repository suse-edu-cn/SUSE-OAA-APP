package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.repository.checkin.CheckinAccountStore
import com.suseoaa.projectoaa.shared.data.repository.checkin.CheckinTaskRepository
import com.suseoaa.projectoaa.shared.data.repository.checkin.CookieStorageTaskGateway
import com.suseoaa.projectoaa.shared.data.repository.checkin.UiasLoginRepository
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinLocations
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTask

/**
 * 密码登录签到的对外门面。
 *
 * 本身不含业务实现，只把请求分派给三个各司其职的协作者：
 * - [CheckinAccountStore]：账号的本地增删改查
 * - [UiasLoginRepository]：UIAS 统一认证登录（验证码 / 短信 / rememberMe）
 * - [CheckinTaskRepository]：签到任务的读取与提交
 *
 * 打卡流程：
 * 1. 获取验证码图片并由用户手动输入（或 OCR 自动识别）
 * 2. 登录 UIAS 统一认证系统
 * 3. 获取待签到任务列表并找到今日任务
 * 4. 提交位置签到
 *
 * 扫码登录走独立的 [QrCodeCheckinRepository]，两者不再共用实现。
 */
class CheckinRepository(
    private val accountStore: CheckinAccountStore,
    private val loginRepository: UiasLoginRepository,
    private val taskRepository: CheckinTaskRepository,
    private val taskGateway: CookieStorageTaskGateway
) {

    // ==================== 账号管理 ====================

    fun getAllAccounts(): List<CheckinAccountData> = accountStore.getAll()

    fun getAccountById(id: Long): CheckinAccountData? = accountStore.getById(id)

    fun addAccount(
        studentId: String,
        password: String,
        name: String = "",
        remark: String = "",
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name
    ): Result<Unit> = accountStore.insertPasswordAccount(
        studentId = studentId,
        password = password,
        name = name,
        remark = remark,
        selectedLocation = selectedLocation
    )

    fun updateAccount(
        id: Long,
        studentId: String,
        password: String,
        name: String,
        remark: String,
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name
    ): Result<Unit> = accountStore.update(
        id = id,
        studentId = studentId,
        password = password,
        name = name,
        remark = remark,
        selectedLocation = selectedLocation
    )

    fun deleteAccount(id: Long): Result<Unit> = accountStore.delete(id)

    fun isAccountExists(studentId: String): Boolean = accountStore.exists(studentId)

    fun updateSession(
        accountId: Long,
        sessionToken: String,
        sessionExpireTime: String
    ): Result<Unit> = accountStore.updateSession(accountId, sessionToken, sessionExpireTime)

    fun clearSession(accountId: Long): Result<Unit> = accountStore.clearSession(accountId)

    fun updateLocation(accountId: Long, locationName: String): Result<Unit> =
        accountStore.updateLocation(accountId, locationName)

    // ==================== 登录 ====================

    suspend fun tryAutoLoginWithRememberMe(account: CheckinAccountData): Result<Boolean> =
        loginRepository.tryAutoLoginWithRememberMe(account)

    suspend fun fetchCaptchaImage(): Result<ByteArray> = loginRepository.fetchCaptchaImage()

    suspend fun loginWithCaptcha(
        username: String,
        password: String,
        captchaCode: String,
        accountId: Long? = null
    ): Result<Unit> = loginRepository.loginWithCaptcha(username, password, captchaCode, accountId)

    fun isSmsVerificationRequired(error: Throwable?): Boolean =
        loginRepository.isSmsVerificationRequired(error)

    fun hasPendingSmsChallenge(): Boolean = loginRepository.hasPendingSmsChallenge()

    fun getPendingSmsMaskedPhone(): String? = loginRepository.getPendingSmsMaskedPhone()

    fun clearPendingSmsChallenge() = loginRepository.clearPendingSmsChallenge()

    suspend fun sendSmsCodeForPendingLogin(): Result<Unit> =
        loginRepository.sendSmsCodeForPendingLogin()

    suspend fun submitSmsCodeForPendingLogin(smsCode: String): Result<Unit> =
        loginRepository.submitSmsCodeForPendingLogin(smsCode)

    // ==================== 签到 ====================

    /**
     * 登录后执行打卡：获取任务列表 → 找到今日任务 → 提交位置签到。
     */
    suspend fun performCheckinAfterLogin(account: CheckinAccountData): CheckinResult =
        taskRepository.checkinTodayTask(
            gateway = taskGateway,
            location = account.randomLocation(),
            onStatus = { accountStore.updateCheckinStatus(account.id, it) }
        )

    /** 对指定任务打卡，支持对已签到的任务重复签到 */
    suspend fun checkinForSpecificTaskInternal(
        taskId: Long,
        account: CheckinAccountData
    ): CheckinResult = taskRepository.checkinTask(
        gateway = taskGateway,
        taskId = taskId,
        location = account.randomLocation(),
        onStatus = { accountStore.updateCheckinStatus(account.id, it) }
    )

    /** 获取三类任务：Triple<待打卡, 已打卡, 缺勤> */
    suspend fun getAllTasks(
        initialLoadCount: Int = 5
    ): Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>> =
        taskRepository.allTasks(taskGateway, initialLoadCount)

    /** 为 [startIndex, endIndex) 区间的任务补齐打卡时间 */
    suspend fun loadCheckinTimeForTasksInternal(
        tasks: List<CheckinTask>,
        startIndex: Int,
        endIndex: Int
    ): Result<List<CheckinTask>> =
        taskRepository.loadCheckinTime(taskGateway, tasks, startIndex, endIndex)

    private fun CheckinAccountData.randomLocation() =
        CheckinLocations.randomLocationForCampus(selectedLocation)
}
