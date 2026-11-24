package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.ServerHealthStatus
import com.suseoaa.projectoaa.shared.domain.model.person.StoredFile

/**
 * PersonRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface PersonRepository {
    /** 注销后端的本设备会话并清空本地数据；后端调用失败也会清本地。 */
    suspend fun logout()

    suspend fun getPersonInfo(): Result<PersonData>

    /** 当前用户及其职位等级，用于决定界面上显示哪些管理入口。 */
    suspend fun getCurrentUser(): Result<CurrentUser>

    /** 从会话内存缓存同步读取当前用户信息，无缓存时返回 null。 */
    fun getCachedCurrentUser(): CurrentUser? = null

    /** 获取当前用户信息，支持指定是否跳过内存缓存强制刷新。 */
    suspend fun getCurrentUser(forceRefresh: Boolean): Result<CurrentUser> = getCurrentUser()

    /** 修改用户名、邮箱与年级，姓名不可修改；[grade] 为 null 时沿用当前年级。 */
    suspend fun updateProfile(username: String, email: String, grade: Int? = null): Result<String>

    /** 上传头像并设为当前头像。 */
    suspend fun uploadAvatar(imageData: ByteArray): Result<String>

    /** 通用文件上传（POST /upload/file，最大 100MB）。 */
    suspend fun uploadFile(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        scene: String = "file"
    ): Result<StoredFile>

    /** 向当前账号绑定的邮箱发送注销验证码。 */
    suspend fun sendDeleteAccountCode(): Result<Unit>

    /** 凭邮箱验证码注销当前账号（进入后端冷静期），成功后本机退出登录。 */
    suspend fun deleteAccount(code: String): Result<Unit>

    /** 向当前账号绑定的邮箱发送取消注销冷静期验证码（scene = cancel_delete）。 */
    suspend fun sendCancelDeleteCode(): Result<Unit>

    /** 冷静期内凭邮箱验证码撤销注销，恢复账号正常状态。 */
    suspend fun cancelDeleteAccount(code: String): Result<Unit>

    /** 检测协会后端服务存活与就绪状态（GET /livez 与 GET /healthz）。 */
    suspend fun checkServerHealth(): Result<ServerHealthStatus>
}
