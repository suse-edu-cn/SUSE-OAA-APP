package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.remote.api.OaaApiService
import com.suseoaa.projectoaa.shared.domain.model.changePassword.ChangePasswordRequest
import com.suseoaa.projectoaa.shared.domain.model.changePassword.ChangePasswordResponse
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.PersonResponse
import com.suseoaa.projectoaa.shared.domain.model.person.UpdatePersonResponse
import com.suseoaa.projectoaa.shared.domain.model.person.UpdateUserRequest

/**
 * 用户仓库
 */
class UserRepository(
    private val userApi: OaaApiService,
    private val tokenManager: TokenManager
) {
    /**
     * 获取用户信息
     */
    suspend fun getUserInfo(): Result<PersonData> {
        return try {
            val response = userApi.getPersonInfo()
            
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "获取用户信息失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新用户信息
     */
    suspend fun updateUser(request: UpdateUserRequest): Result<UpdatePersonResponse> {
        return try {
            val response = userApi.updateUserInfo(request)
            
            if (response.code == 200) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "更新信息失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新用户信息（简化接口）
     */
    suspend fun updateUserInfo(username: String, name: String, email: String): Result<UpdatePersonResponse> {
        return updateUser(UpdateUserRequest(username = username, name = name, email = email))
    }

    /**
     * 修改密码
     */
    suspend fun changePassword(
        newPassword: String,
        emailCode: String
    ): Result<ChangePasswordResponse> {
        return try {
            val request = ChangePasswordRequest(
                newPassword = newPassword,
                emailCode = emailCode
            )
            val response = userApi.changePassword(request)
            
            if (response.code == 200) {
                // 修改密码成功后清除 Token
                tokenManager.clearToken()
                Result.success(response)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "修改密码失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 登出
     */
    suspend fun logout() {
        tokenManager.clearAll()
    }
}
