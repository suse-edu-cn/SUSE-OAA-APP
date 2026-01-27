package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.api.OaaApiService
import com.suseoaa.projectoaa.data.model.ChangePasswordRequest
import com.suseoaa.projectoaa.data.model.PersonData
import com.suseoaa.projectoaa.data.model.UpdateUserRequest

/**
 * 用户个人信息仓库
 */
class PersonRepository(
    private val api: OaaApiService,
    private val tokenManager: TokenManager
) {
    suspend fun logout() {
        tokenManager.clear()
    }

    suspend fun getPersonInfo(): Result<PersonData> {
        return try {
            val response = api.getPersonInfo()
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<String> {
        return try {
            val request = ChangePasswordRequest(oldPassword, newPassword)
            val response = api.changePassword(request)
            if (response.code == 200) {
                Result.success(response.message)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserInfo(username: String, name: String): Result<String> {
        return try {
            val response = api.updateUserInfo(UpdateUserRequest(username, name))
            if (response.code == 200) {
                Result.success(response.message)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
