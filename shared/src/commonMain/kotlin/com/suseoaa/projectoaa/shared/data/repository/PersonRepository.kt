package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.remote.api.OaaApiService
import com.suseoaa.projectoaa.shared.domain.model.changePassword.ChangePasswordRequest
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.UpdateAvatarRequest
import com.suseoaa.projectoaa.shared.domain.model.person.UpdateUserRequest

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
                Result.failure(Exception(response.message.ifEmpty { "获取个人信息失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(
//        oldPassword: String,
        newPassword: String,
        emailCode: String
    ): Result<String> {
        return try {
            val request = ChangePasswordRequest(newPassword, emailCode)
            val response = api.changePassword(request)
            if (response.code == 200) {
                Result.success(response.message.ifEmpty { "修改成功" })
            } else {
                Result.failure(Exception(response.message.ifEmpty { "修改密码失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //获取邮箱验证码
    suspend fun getEmailCode(): Result<String> {
        return try {
            val response = api.getEmailCode()
            if (response.code == 200) {
                Result.success(response.message.ifEmpty { "验证码已发送" })
            } else {
                Result.failure(Exception(response.message.ifEmpty { "获取验证码失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserInfo(username: String, name: String, email: String): Result<String> {
        return try {
            val response = api.updateUserInfo(UpdateUserRequest(username, name, email))
            if (response.code == 200) {
                Result.success(response.message.ifEmpty { "更新成功" })
            } else {
                Result.failure(Exception(response.message.ifEmpty { "更新信息失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAvatar(imageData: ByteArray): Result<String> {
        return try {
            val response = api.uploadAvatar(imageData)
            if (response.code == 200) {
                Result.success(response.message.ifEmpty { "上传成功" })
            } else {
                Result.failure(Exception(response.message.ifEmpty { "上传头像失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun queryUsers(department: String, name: String, role: String): Result<List<com.suseoaa.projectoaa.shared.domain.model.person.UserQueryData>> {
        return try {
            val response = api.queryUsers(com.suseoaa.projectoaa.shared.domain.model.person.UserQueryRequest(department, name, role))
            if (response.code == 200) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "查询用户失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changeUserMessage(users: List<com.suseoaa.projectoaa.shared.domain.model.person.UserQueryData>): Result<String> {
        return try {
            val response = api.changeUserMessage(users)
            if (response.code == 200) {
                Result.success("修改成功")
            } else {
                Result.failure(Exception(response.message.ifEmpty { "修改失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
