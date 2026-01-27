package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.data.api.OaaApiService
import com.suseoaa.projectoaa.data.model.RegisterRequest
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
        password: String
    ): Result<String> {
        return try {
            val request = RegisterRequest(
                studentId = studentId,
                name = name,
                username = username,
                password = password
            )
            val response = api.register(request)
            
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
