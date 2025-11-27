package com.suseoaa.projectoaa.student.repository

import com.suseoaa.projectoaa.common.network.NetworkModule
import com.suseoaa.projectoaa.student.network.ApiService
import com.suseoaa.projectoaa.student.network.ApplicationRequest

class StudentRepository {
    private val api = NetworkModule.createService(ApiService::class.java)

    suspend fun submitApplication(token: String, request: ApplicationRequest): Result<String> {
        val response = api.submitApplication(token, request)
        return if (response.isSuccessful) {
            Result.success("提交成功")
        } else {
            val errorBody = response.errorBody()?.string() ?: "未知错误"
            Result.failure(Exception("提交失败: $errorBody"))
        }
    }
}