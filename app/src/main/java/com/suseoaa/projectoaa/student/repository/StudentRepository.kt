package com.suseoaa.projectoaa.student.repository

import javax.inject.Inject
import javax.inject.Singleton
import com.suseoaa.projectoaa.student.network.ApiService
import com.suseoaa.projectoaa.student.network.ApplicationRequest

@Singleton
class StudentRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun submitApplication(token: String, request: ApplicationRequest): Result<Unit> {
        return try {
            val response = api.submitApplication(token, request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "未知错误"
                Result.failure(Exception("提交失败: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("网络请求失败: ${e.message}"))
        }
    }
}