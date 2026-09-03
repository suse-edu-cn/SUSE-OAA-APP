package com.suseoaa.projectoaa.shared.data.repository.checkin

import com.suseoaa.projectoaa.shared.data.remote.api.CheckinApiService
import com.suseoaa.projectoaa.shared.data.remote.api.QrCodeCheckinApiService
import io.ktor.client.statement.HttpResponse

/**
 * 签到任务接口的最小抽象。
 *
 * 密码登录与扫码登录访问的是同一批业务接口，差别只在于会话怎么带：
 * 前者依赖 HttpClient 自身的 Cookie 存储，后者需要显式携带完整 Cookie 与 openId。
 * 把这点差异隔离在网关里，任务列表与签到提交的业务逻辑就只需要写一份。
 */
interface CheckinTaskGateway {
    /** @param status 1=待签到, 2=已完成, 3=已缺勤 */
    suspend fun taskList(status: Int): HttpResponse
    suspend fun taskDetail(taskId: Long): HttpResponse
    suspend fun submitCheckin(requestBody: String): HttpResponse
}

/** 密码登录：会话由 HttpClient 的 Cookie 存储自动携带 */
class CookieStorageTaskGateway(
    private val api: CheckinApiService
) : CheckinTaskGateway {
    override suspend fun taskList(status: Int) = api.getTaskList(status)
    override suspend fun taskDetail(taskId: Long) = api.getTaskDetail(taskId)
    override suspend fun submitCheckin(requestBody: String) = api.submitLocationCheckin(requestBody)
}

/** 扫码登录：显式携带完整 Cookie 字符串，并用 openId 拼 Referer */
class QrCodeTaskGateway(
    private val api: QrCodeCheckinApiService,
    private val cookies: String,
    private val openId: String?
) : CheckinTaskGateway {
    override suspend fun taskList(status: Int) = api.getTaskList(cookies, status, openId)
    override suspend fun taskDetail(taskId: Long) = api.getTaskDetail(cookies, taskId, openId)
    override suspend fun submitCheckin(requestBody: String) =
        api.submitCheckin(cookies, requestBody, openId)
}
