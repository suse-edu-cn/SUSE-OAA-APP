package com.suseoaa.projectoaa.shared.data.repository.checkin

import com.suseoaa.projectoaa.shared.database.CheckinAccount
import com.suseoaa.projectoaa.shared.database.CourseDatabase
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinLocations

/**
 * 签到账号的本地存储。
 *
 * 密码登录与扫码登录两条链路都要读写同一张账号表，之前各自在仓库里直接持有
 * `queries` 并重复实现了 `updateSession` / `updateCheckinStatus` 等方法，
 * 这里把数据库访问收敛成唯一入口。
 */
class CheckinAccountStore(private val database: CourseDatabase) {

    private val queries get() = database.checkinAccountQueries

    // ==================== 查询 ====================

    fun getAll(): List<CheckinAccountData> =
        queries.selectAll().executeAsList().map { it.toData() }

    fun getById(id: Long): CheckinAccountData? =
        queries.selectById(id).executeAsOneOrNull()?.toData()

    fun findByStudentId(studentId: String): CheckinAccountData? =
        queries.selectByStudentId(studentId).executeAsOneOrNull()?.toData()

    fun exists(studentId: String): Boolean =
        queries.selectByStudentId(studentId).executeAsOneOrNull() != null

    // ==================== 写入 ====================

    /** 新增密码登录账号 */
    fun insertPasswordAccount(
        studentId: String,
        password: String,
        name: String = "",
        remark: String = "",
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name
    ): Result<Unit> = runCatching {
        val now = CheckinClock.nowString()
        queries.insert(
            studentId = studentId,
            password = password,
            name = name,
            remark = remark,
            createdAt = now,
            updatedAt = now,
            loginType = 0,
            sessionToken = null,
            sessionExpireTime = null,
            selectedLocation = selectedLocation
        )
    }

    /**
     * 保存扫码登录账号：已存在则只刷新会话，不存在才插入。
     * @return 账号 ID
     */
    fun saveQrCodeAccount(
        studentId: String,
        name: String,
        sessionToken: String,
        sessionExpireTime: String,
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name
    ): Result<Long> = runCatching {
        val now = CheckinClock.nowString()
        val existing = queries.selectByStudentId(studentId).executeAsOneOrNull()
        if (existing != null) {
            queries.updateSession(
                sessionToken = sessionToken,
                sessionExpireTime = sessionExpireTime,
                updatedAt = now,
                id = existing.id
            )
            println("[CheckinStore] 更新扫码账号: $studentId")
            existing.id
        } else {
            queries.insertQrCodeAccount(
                studentId = studentId,
                name = name,
                remark = "扫码登录",
                createdAt = now,
                updatedAt = now,
                sessionToken = sessionToken,
                sessionExpireTime = sessionExpireTime,
                selectedLocation = selectedLocation
            )
            val newId = queries.selectByStudentId(studentId).executeAsOneOrNull()?.id ?: 0L
            println("[CheckinStore] 新增扫码账号: $studentId, id=$newId")
            newId
        }
    }

    fun update(
        id: Long,
        studentId: String,
        password: String,
        name: String,
        remark: String,
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name
    ): Result<Unit> = runCatching {
        queries.update(
            studentId = studentId,
            password = password,
            name = name,
            remark = remark,
            updatedAt = CheckinClock.nowString(),
            selectedLocation = selectedLocation,
            id = id
        )
    }

    fun delete(id: Long): Result<Unit> = runCatching { queries.deleteById(id) }

    fun updateSession(
        accountId: Long,
        sessionToken: String,
        sessionExpireTime: String
    ): Result<Unit> = runCatching {
        queries.updateSession(
            sessionToken = sessionToken,
            sessionExpireTime = sessionExpireTime,
            updatedAt = CheckinClock.nowString(),
            id = accountId
        )
    }

    fun clearSession(accountId: Long): Result<Unit> = runCatching {
        queries.clearSession(updatedAt = CheckinClock.nowString(), id = accountId)
    }

    fun updateLocation(accountId: Long, locationName: String): Result<Unit> = runCatching {
        queries.updateLocation(
            selectedLocation = locationName,
            updatedAt = CheckinClock.nowString(),
            id = accountId
        )
    }

    /** 记录最近一次签到的时间与结果文案 */
    fun updateCheckinStatus(accountId: Long, status: String) {
        val now = CheckinClock.nowString()
        queries.updateCheckinStatus(
            lastCheckinTime = now,
            lastCheckinStatus = status,
            updatedAt = now,
            id = accountId
        )
    }

    private fun CheckinAccount.toData() = CheckinAccountData(
        id = id,
        studentId = studentId,
        password = password,
        name = name,
        remark = remark,
        lastCheckinTime = lastCheckinTime,
        lastCheckinStatus = lastCheckinStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
        loginType = loginType.toInt(),
        sessionToken = sessionToken,
        sessionExpireTime = sessionExpireTime,
        selectedLocation = selectedLocation
    )
}
