package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.person.PersonData

/**
 * PersonRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface PersonRepository {

    suspend fun logout(): Unit

    suspend fun getPersonInfo(): Result<PersonData>

    suspend fun changePassword(
        account: String,
        newPassword: String,
        emailCode: String
        ): Result<String>

    suspend fun getEmailCode(account: String): Result<String>

    suspend fun updateUserInfo(username: String, name: String, email: String): Result<String>

    suspend fun uploadAvatar(imageData: ByteArray): Result<String>

    suspend fun queryUsers(
        department: String,
        name: String,
        role: String
        ): Result<List<com.suseoaa.projectoaa.shared.domain.model.person.UserQueryData>>

    suspend fun changeUserMessage(users: List<com.suseoaa.projectoaa.shared.domain.model.person.UserQueryData>): Result<String>
}
