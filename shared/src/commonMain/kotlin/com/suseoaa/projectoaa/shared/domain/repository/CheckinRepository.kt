package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinLocations
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTask

/**
 * 密码登录签到的契约。
 *
 * 抽出接口是为了让上层（CheckinViewModel）能在测试里换成假实现——签到流程涉及
 * 统一认证登录、图形验证码、短信二次验证、任务列表分页，是全应用最复杂也最不敢
 * 动的一段逻辑，而它此前完全没有测试，正是因为没有这道缝。
 *
 * 默认参数写在接口这一侧，实现类的 override 不再重复声明（Kotlin 不允许）。
 */
interface CheckinRepository {

    // ==================== 账号管理 ====================

    fun getAllAccounts(): List<CheckinAccountData>

    fun getAccountById(id: Long): CheckinAccountData?

    fun addAccount(
        studentId: String,
        password: String,
        name: String = "",
        remark: String = "",
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name,
    ): Result<Unit>

    fun updateAccount(
        id: Long,
        studentId: String,
        password: String,
        name: String,
        remark: String,
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name,
    ): Result<Unit>

    fun deleteAccount(id: Long): Result<Unit>

    fun isAccountExists(studentId: String): Boolean

    fun updateSession(accountId: Long, sessionToken: String, sessionExpireTime: String): Result<Unit>

    fun clearSession(accountId: Long): Result<Unit>

    fun updateLocation(accountId: Long, locationName: String): Result<Unit>

    // ==================== 登录 ====================

    suspend fun tryAutoLoginWithRememberMe(account: CheckinAccountData): Result<Boolean>

    suspend fun fetchCaptchaImage(): Result<ByteArray>

    suspend fun loginWithCaptcha(
        username: String,
        password: String,
        captchaCode: String,
        accountId: Long? = null,
    ): Result<Unit>

    fun isSmsVerificationRequired(error: Throwable?): Boolean

    fun hasPendingSmsChallenge(): Boolean

    fun getPendingSmsMaskedPhone(): String?

    fun clearPendingSmsChallenge()

    suspend fun sendSmsCodeForPendingLogin(): Result<Unit>

    suspend fun submitSmsCodeForPendingLogin(smsCode: String): Result<Unit>

    // ==================== 签到 ====================

    /** 登录后执行打卡：获取任务列表 → 找到今日任务 → 提交位置签到。 */
    suspend fun performCheckinAfterLogin(account: CheckinAccountData): CheckinResult

    /** 对指定任务打卡，支持对已签到的任务重复签到。 */
    suspend fun checkinForSpecificTaskInternal(taskId: Long, account: CheckinAccountData): CheckinResult

    /** 获取三类任务：Triple<待打卡, 已打卡, 缺勤>。 */
    suspend fun getAllTasks(
        initialLoadCount: Int = 5,
    ): Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>>

    /** 为 [startIndex, endIndex) 区间的任务补齐打卡时间。 */
    suspend fun loadCheckinTimeForTasksInternal(
        tasks: List<CheckinTask>,
        startIndex: Int,
        endIndex: Int,
    ): Result<List<CheckinTask>>
}
