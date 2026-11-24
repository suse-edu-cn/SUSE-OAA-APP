package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.shared.data.local.store.UserDataCleaner
import com.suseoaa.projectoaa.shared.data.remote.api.OaaAuthApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaUserApi
import com.suseoaa.projectoaa.shared.data.remote.oaaDevice
import com.suseoaa.projectoaa.shared.domain.model.changePassword.SCENE_CANCEL_DELETE
import com.suseoaa.projectoaa.shared.domain.model.changePassword.SCENE_DELETE_USER
import com.suseoaa.projectoaa.shared.domain.model.changePassword.SendCodeRequest
import com.suseoaa.projectoaa.shared.domain.model.login.LogoutRequest
import com.suseoaa.projectoaa.shared.domain.model.person.CancelDeleteRequest
import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.model.person.EnrollmentGrade
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import com.suseoaa.projectoaa.shared.domain.model.person.DeleteUserRequest
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.ServerHealthStatus
import com.suseoaa.projectoaa.shared.domain.model.person.StoredFile
import com.suseoaa.projectoaa.shared.domain.model.person.UpdateMeRequest
import com.suseoaa.projectoaa.shared.domain.repository.OrganizationRepository
import com.suseoaa.projectoaa.shared.domain.repository.PersonRepository
import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import kotlin.time.TimeSource

/**
 * 用户个人信息仓库
 */
class PersonRepositoryImpl(
    private val userApi: OaaUserApi,
    private val authApi: OaaAuthApi,
    private val organizationRepository: OrganizationRepository,
    private val userDataCleaner: UserDataCleaner,
    private val sessionStore: SessionStore? = null
) : PersonRepository {

    private data class CachedCurrentUser(
        val user: CurrentUser,
        val fetchedAtMs: Long,
        val sessionToken: String?
    )

    private val userMutex = Mutex()

    @Volatile
    private var cachedCurrentUser: CachedCurrentUser? = null

    override fun getCachedCurrentUser(): CurrentUser? {
        val entry = cachedCurrentUser ?: return null
        if (sessionStore != null && entry.sessionToken != sessionStore.cachedToken) {
            return null
        }
        return entry.user
    }

    override suspend fun logout() {
        // 后端注销失败（断网、token 已过期）不影响本地退出
        cachedCurrentUser = null
        authApi.logout(LogoutRequest(device = oaaDevice))
        userDataCleaner.clearSession()
    }

    override suspend fun getPersonInfo(): Result<PersonData> = userApi.me()

    override suspend fun getCurrentUser(): Result<CurrentUser> = getCurrentUser(forceRefresh = false)

    override suspend fun getCurrentUser(forceRefresh: Boolean): Result<CurrentUser> = userMutex.withLock {
        val currentToken = sessionStore?.cachedToken
        val existing = cachedCurrentUser
        if (sessionStore != null && existing != null && existing.sessionToken != currentToken) {
            cachedCurrentUser = null
        } else if (!forceRefresh && existing != null) {
            val ageMs = OaaClock.now().toEpochMilliseconds() - existing.fetchedAtMs
            if (ageMs < CURRENT_USER_TTL_MS) {
                return@withLock Result.success(existing.user)
            }
        }

        val person = userApi.me().getOrElse { return@withLock Result.failure(it) }
        // 职位 / 部门列表拿不到时降级为最低权限，不影响页面本身的加载
        val role = organizationRepository.getRoles().getOrNull()?.firstOrNull { it.name == person.role }
        val department = organizationRepository.getDepartments().getOrNull()
            ?.firstOrNull { it.name == person.department }
        val user = CurrentUser(
            person = person,
            level = person.roleLevel ?: role?.level ?: 0,
            departmentId = department?.id,
            roleId = role?.id
        )
        if (sessionStore == null || sessionStore.cachedToken == currentToken) {
            cachedCurrentUser = CachedCurrentUser(
                user = user,
                fetchedAtMs = OaaClock.now().toEpochMilliseconds(),
                sessionToken = currentToken
            )
        }
        Result.success(user)
    }

    override suspend fun updateProfile(username: String, email: String, grade: Int?): Result<String> {
        if (grade != null && !EnrollmentGrade.isValid(grade)) {
            return appFailure(AppError.Business(userMessage = EnrollmentGrade.ERROR_MESSAGE))
        }
        // 更新接口必填字段不改的也要原样带上，头像和未指定的年级沿用当前的
        val current = userApi.me().getOrElse { return Result.failure(it) }
        if (current.avatar.uri.isBlank() && current.avatar.url.isNotBlank()) {
            return appFailure(AppError.Business(userMessage = "头像信息无效，请先重新上传头像"))
        }
        return userApi.updateMe(
            UpdateMeRequest(
                username = username,
                email = email,
                avatar = current.avatar.uri,
                grade = grade ?: current.resolvedGrade
            )
        ).onSuccess {
            cachedCurrentUser = null
        }.map { "信息更新成功" }
    }

    override suspend fun uploadAvatar(imageData: ByteArray): Result<String> {
        val uploaded = userApi.uploadImage(
            scene = AVATAR_SCENE,
            bytes = imageData,
            fileName = "avatar.jpg",
            mimeType = "image/jpeg"
        ).getOrElse { return Result.failure(it) }
        val current = userApi.me().getOrElse { return Result.failure(it) }
        return userApi.updateMe(
            UpdateMeRequest(
                username = current.username,
                email = current.email,
                avatar = uploaded.uri,
                grade = current.resolvedGrade
            )
        ).onSuccess {
            cachedCurrentUser = null
        }.map { "头像更新成功" }
    }

    override suspend fun uploadFile(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        scene: String
    ): Result<StoredFile> = userApi.uploadFile(
        scene = scene,
        bytes = bytes,
        fileName = fileName,
        mimeType = mimeType
    )

    override suspend fun sendDeleteAccountCode(): Result<Unit> {
        // /auth/send 靠 account 找到要发验证码的邮箱
        val me = userApi.me().getOrElse { return Result.failure(it) }
        return authApi.sendCode(SendCodeRequest(account = me.username, scene = SCENE_DELETE_USER))
    }

    override suspend fun deleteAccount(code: String): Result<Unit> =
        userApi.delete(DeleteUserRequest(code = code), fallbackMessage = "注销账号失败")
            // 冷静期内账号还在，顺带注销本设备的 refresh token，再清本地会话
            .onSuccess { logout() }

    override suspend fun sendCancelDeleteCode(): Result<Unit> {
        val me = userApi.me().getOrElse { return Result.failure(it) }
        return authApi.sendCode(SendCodeRequest(account = me.username, scene = SCENE_CANCEL_DELETE))
    }

    override suspend fun cancelDeleteAccount(code: String): Result<Unit> =
        userApi.cancelDelete(CancelDeleteRequest(code = code))
            .onSuccess { cachedCurrentUser = null }

    override suspend fun checkServerHealth(): Result<ServerHealthStatus> = coroutineScope {
        val start = TimeSource.Monotonic.markNow()
        val liveDeferred = async { userApi.livez() }
        val healthDeferred = async { userApi.healthz() }
        val liveResult = liveDeferred.await()
        val healthResult = healthDeferred.await()
        val latencyMs = start.elapsedNow().inWholeMilliseconds

        val live = liveResult.isSuccess
        val ready = healthResult.isSuccess
        if (!live && !ready) {
            val error = liveResult.exceptionOrNull() ?: healthResult.exceptionOrNull()
            return@coroutineScope if (error != null) {
                Result.failure(error)
            } else {
                Result.success(
                    ServerHealthStatus(live = false, ready = false, latencyMs = latencyMs, message = "服务不可用")
                )
            }
        }
        val summary = when {
            live && ready -> "服务运行正常（延迟 ${latencyMs}ms）"
            live -> "服务已存活但尚未就绪（延迟 ${latencyMs}ms）"
            else -> "服务就绪探针响应异常（延迟 ${latencyMs}ms）"
        }
        Result.success(
            ServerHealthStatus(
                live = live,
                ready = ready,
                latencyMs = latencyMs,
                message = summary
            )
        )
    }

    private companion object {
        const val AVATAR_SCENE = "avatar"
        const val CURRENT_USER_TTL_MS = 60_000L
    }
}
