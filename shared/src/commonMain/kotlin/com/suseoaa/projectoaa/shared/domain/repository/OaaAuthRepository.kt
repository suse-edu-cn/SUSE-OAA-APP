package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.login.LoginResponse

/**
 * OaaAuthRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface OaaAuthRepository {

    suspend fun login(username: String, password: String): Result<LoginResponse>
}
