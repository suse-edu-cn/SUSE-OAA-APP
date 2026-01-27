package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.remote.api.UserApi
import com.suseoaa.projectoaa.shared.domain.model.changePassword.ChangePasswordRequest
import com.suseoaa.projectoaa.shared.domain.model.changePassword.ChangePasswordResponse
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.UpdatePersonResponse
import com.suseoaa.projectoaa.shared.domain.model.person.UpdateUserRequest

/**
 * 用户仓库
 */
class UserRepository(
    private val userApi: UserApi,
    private val tokenManager: TokenManager
) {
    /**
     * 获取用户信息
     */
    suspend fun getUserInfo(): Result<PersonData> {
        return try {
            val response = userApi.getUserInfo()
            
            if (response.code == 200 && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message, response.code)
            }
        } catch (e: Exception) {
            Result.Error("获取用户信息失败: ${e.message}", exception = e)
        }
    }

    /**
     * 更新用户信息
     */
    suspend fun updateUser(request: UpdateUserRequest): Result<UpdatePersonResponse> {
        return try {
            val response = userApi.updateUser(request)
            
            if (response.code == 200) {
                Result.Success(response)
            } else {
                Result.Error(response.message, response.code)
            }
        } catch (e: Exception) {
            Result.Error("更新用户信息失败: ${e.message}", exception = e)
        }
    }

    /**
     * 更新用户信息（简化接口）
     */
    suspend fun updateUserInfo(username: String, name: String): Result<UpdatePersonResponse> {
        return updateUser(UpdateUserRequest(username = username, name = name))
    }

    /**
     * 修改密码
     */
    suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Result<ChangePasswordResponse> {
        return try {
            val request = ChangePasswordRequest(
                oldPassword = oldPassword,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            )
            val response = userApi.changePassword(request)
            
            if (response.code == 200) {
                // 修改密码成功后清除 Token
                tokenManager.clearToken()
                Result.Success(response)
            } else {
                Result.Error(response.message, response.code)
            }
        } catch (e: Exception) {
            Result.Error("修改密码失败: ${e.message}", exception = e)
        }
    }

    /**
     * 登出
     */
    suspend fun logout() {
        tokenManager.clearAll()
    }
}
