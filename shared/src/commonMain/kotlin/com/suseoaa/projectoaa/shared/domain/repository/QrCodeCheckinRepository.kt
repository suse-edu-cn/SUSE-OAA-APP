package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinLocations
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTask
import com.suseoaa.projectoaa.shared.domain.model.checkin.EduUserInfo
import com.suseoaa.projectoaa.shared.domain.model.checkin.SopSessionUser
import com.suseoaa.projectoaa.shared.domain.model.checkin.WechatScanStatusData

/**
 * 微信扫码登录签到的契约。
 *
 * 与密码登录（[CheckinRepository]）走完全独立的会话与 Cookie 存储，因此单独一套接口。
 */
interface QrCodeCheckinRepository {

    // ==================== 扫码登录流程 ====================

    suspend fun getClientId(): Result<String>

    suspend fun getQrCodeImage(clientId: String): Result<String>

    suspend fun checkScanStatus(clientId: String): Result<WechatScanStatusData>

    suspend fun handleScanCallback(callbackUrl: String): Result<String>

    suspend fun getSessionCookie(sopSessionCookie: String): Result<String>

    suspend fun completeSsoWithSopSession(cookies: String): Result<String>

    fun extractUserInfoFromSopSession(sopSession: String): SopSessionUser?

    suspend fun getEduUserInfoWithCookies(cookies: String): Result<EduUserInfo>

    // ==================== 账号与会话 ====================

    fun saveQrCodeAccount(
        studentId: String,
        name: String,
        sessionToken: String,
        sessionExpireTime: String,
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name,
    ): Result<Long>

    suspend fun refreshSessionIfExpired(account: CheckinAccountData): Result<String>

    // ==================== 签到 ====================

    suspend fun performCheckinWithSession(account: CheckinAccountData): CheckinResult

    suspend fun checkinForSpecificTask(
        cookies: String,
        taskId: Long,
        account: CheckinAccountData,
    ): CheckinResult

    /** 获取三类任务：Triple<待签到, 已完成, 缺勤>。 */
    suspend fun getAllTasksWithCookies(
        cookies: String,
        initialLoadCount: Int = 5,
    ): Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>>

    /** 为 [startIndex, endIndex) 区间的任务补齐打卡时间。 */
    suspend fun loadCheckinTimeForTasks(
        tasks: List<CheckinTask>,
        startIndex: Int,
        endIndex: Int,
        cookies: String,
    ): Result<List<CheckinTask>>
}
