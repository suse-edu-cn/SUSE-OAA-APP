package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaApiService
import com.suseoaa.projectoaa.shared.domain.model.register.RegisterRequest
import kotlinx.serialization.json.Json

/**
 * OAA 后端注册仓库
 */
class OaaRegisterRepository(
    private val api: OaaApiService,
    private val json: Json
) {
    suspend fun register(
        studentId: String,
        name: String,
        username: String,
        password: String,
        email: String
    ): Result<String> {
        return try {
            val request = RegisterRequest(
                studentId = studentId,
                name = name,
                username = username,
                password = password,
                email = email
            )
            val response = api.register(request)

            if (response.code == 200) {
                Result.success(response.message.ifEmpty { "注册成功" })
            } else {
                Result.failure(Exception(response.message.ifEmpty { "注册失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
