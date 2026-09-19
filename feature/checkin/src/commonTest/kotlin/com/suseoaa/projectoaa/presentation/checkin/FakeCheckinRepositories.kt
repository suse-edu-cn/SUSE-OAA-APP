package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.shared.domain.model.checkin.SopSessionUser
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTask
import com.suseoaa.projectoaa.shared.domain.model.checkin.EduUserInfo
import com.suseoaa.projectoaa.shared.domain.model.checkin.WechatScanStatusData
import com.suseoaa.projectoaa.shared.domain.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.domain.repository.QrCodeCheckinRepository

/**
 * 密码登录签到仓库的测试替身。
 *
 * 账号列表放在内存里，增删改直接作用其上，这样 ViewModel 调用 loadAccounts()
 * 重新拉取时能看到变化，可以验证"操作成功后会刷新列表"这类行为。
 */
class FakeCheckinRepository : CheckinRepository {

    val accounts = mutableListOf<CheckinAccountData>()

    /** 置为非 null 时，getAllAccounts 抛出它，用来验证加载失败分支。 */
    var loadError: Throwable? = null

    /** 置为非 null 时，写操作返回该失败。 */
    var writeError: Throwable? = null

    var addAccountCalls = 0
        private set
    var deleteAccountCalls = 0
        private set

    private fun <T> writeResult(value: T): Result<T> =
        writeError?.let { Result.failure(it) } ?: Result.success(value)

    override fun getAllAccounts(): List<CheckinAccountData> {
        loadError?.let { throw it }
        return accounts.toList()
    }

    override fun getAccountById(id: Long): CheckinAccountData? = accounts.find { it.id == id }

    override fun addAccount(
        studentId: String,
        password: String,
        name: String,
        remark: String,
        selectedLocation: String,
    ): Result<Unit> {
        addAccountCalls++
        if (writeError != null) return writeResult(Unit)
        accounts += CheckinAccountData(
            id = (accounts.maxOfOrNull { it.id } ?: 0L) + 1,
            studentId = studentId, password = password, name = name,
            remark = remark, selectedLocation = selectedLocation,
        )
        return Result.success(Unit)
    }

    override fun updateAccount(
        id: Long,
        studentId: String,
        password: String,
        name: String,
        remark: String,
        selectedLocation: String,
    ): Result<Unit> {
        if (writeError != null) return writeResult(Unit)
        val index = accounts.indexOfFirst { it.id == id }
        if (index >= 0) {
            accounts[index] = accounts[index].copy(
                studentId = studentId, password = password, name = name,
                remark = remark, selectedLocation = selectedLocation,
            )
        }
        return Result.success(Unit)
    }

    override fun deleteAccount(id: Long): Result<Unit> {
        deleteAccountCalls++
        if (writeError != null) return writeResult(Unit)
        accounts.removeAll { it.id == id }
        return Result.success(Unit)
    }

    override fun isAccountExists(studentId: String): Boolean =
        accounts.any { it.studentId == studentId }

    override fun updateSession(accountId: Long, sessionToken: String, sessionExpireTime: String) =
        writeResult(Unit)

    override fun clearSession(accountId: Long) = writeResult(Unit)

    override fun updateLocation(accountId: Long, locationName: String) = writeResult(Unit)

    // ---- 登录相关：本组测试不覆盖，给出稳定的默认值 ----

    var autoLoginResult: Result<Boolean> = Result.success(false)
    override suspend fun tryAutoLoginWithRememberMe(account: CheckinAccountData) = autoLoginResult

    var captchaImage: Result<ByteArray> = Result.success(byteArrayOf(1, 2, 3))
    override suspend fun fetchCaptchaImage() = captchaImage

    var loginResult: Result<Unit> = Result.success(Unit)
    var loginWithCaptchaCalls = 0
        private set

    override suspend fun loginWithCaptcha(
        username: String,
        password: String,
        captchaCode: String,
        accountId: Long?,
    ): Result<Unit> {
        loginWithCaptchaCalls++
        return loginResult
    }

    var smsRequired = false
    override fun isSmsVerificationRequired(error: Throwable?) = smsRequired

    var pendingSmsChallenge = false
    override fun hasPendingSmsChallenge() = pendingSmsChallenge

    var maskedPhone: String? = "138****0000"
    override fun getPendingSmsMaskedPhone(): String? = maskedPhone

    var clearPendingSmsCalls = 0
        private set
    override fun clearPendingSmsChallenge() { clearPendingSmsCalls++ }

    var sendSmsCalls = 0
        private set
    var sendSmsResult: Result<Unit> = Result.success(Unit)
    override suspend fun sendSmsCodeForPendingLogin(): Result<Unit> {
        sendSmsCalls++
        return sendSmsResult
    }

    var submitSmsResult: Result<Unit> = Result.success(Unit)
    override suspend fun submitSmsCodeForPendingLogin(smsCode: String) = submitSmsResult

    var checkinResult: CheckinResult = CheckinResult.Success("签到成功")
    override suspend fun performCheckinAfterLogin(account: CheckinAccountData) = checkinResult
    override suspend fun checkinForSpecificTaskInternal(taskId: Long, account: CheckinAccountData) =
        checkinResult

    var tasks: Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>> =
        Triple(emptyList(), emptyList(), emptyList())
    override suspend fun getAllTasks(initialLoadCount: Int) = tasks

    override suspend fun loadCheckinTimeForTasksInternal(
        tasks: List<CheckinTask>,
        startIndex: Int,
        endIndex: Int,
    ) = Result.success(tasks)
}

/** 扫码登录仓库的测试替身，本组测试只覆盖密码登录路径，这里给稳定默认值。 */
class FakeQrCodeCheckinRepository : QrCodeCheckinRepository {
    override suspend fun getClientId() = Result.success("client-id")
    override suspend fun getQrCodeImage(clientId: String) = Result.success("qr-image")
    override suspend fun checkScanStatus(clientId: String) = Result.success(WechatScanStatusData())
    override suspend fun handleScanCallback(callbackUrl: String) = Result.success("cookies")
    override suspend fun getSessionCookie(sopSessionCookie: String) = Result.success("session")
    /** JWT 里能否解析出用户信息；null 表示解析不出，走 API 兜底。 */
    var sopSessionUser: SopSessionUser? = null
    override fun extractUserInfoFromSopSession(sopSession: String) = sopSessionUser

    var eduUserInfo: Result<EduUserInfo> = Result.success(EduUserInfo())
    override suspend fun getEduUserInfoWithCookies(cookies: String) = eduUserInfo

    var ssoResult: Result<String> = Result.success("full-cookies")
    override suspend fun completeSsoWithSopSession(cookies: String) = ssoResult

    /** 记录最近一次保存的账号，便于断言实际写入了什么。 */
    var savedAccount: Triple<String, String, String>? = null
        private set
    var saveResult: Result<Long> = Result.success(1L)

    override fun saveQrCodeAccount(
        studentId: String,
        name: String,
        sessionToken: String,
        sessionExpireTime: String,
        selectedLocation: String,
    ): Result<Long> {
        savedAccount = Triple(studentId, name, sessionToken)
        return saveResult
    }

    var refreshSessionResult: Result<String> = Result.success("cookies")
    override suspend fun refreshSessionIfExpired(account: CheckinAccountData) = refreshSessionResult

    var checkinResult: CheckinResult = CheckinResult.Success("签到成功")
    override suspend fun performCheckinWithSession(account: CheckinAccountData) = checkinResult
    override suspend fun checkinForSpecificTask(
        cookies: String,
        taskId: Long,
        account: CheckinAccountData,
    ) = checkinResult

    var tasks: Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>> =
        Triple(emptyList(), emptyList(), emptyList())

    /** 置为非 null 时，拉取任务抛出它，用于验证 401 后的重试分支。 */
    var tasksError: Throwable? = null

    override suspend fun getAllTasksWithCookies(cookies: String, initialLoadCount: Int):
        Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>> {
        tasksError?.let { throw it }
        return tasks
    }

    override suspend fun loadCheckinTimeForTasks(
        tasks: List<CheckinTask>,
        startIndex: Int,
        endIndex: Int,
        cookies: String,
    ) = Result.success(tasks)
}
