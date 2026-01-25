package com.suseoaa.projectoaa.core.data.repository

import androidx.lifecycle.ViewModel
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.network.application.ApplicationService
import com.suseoaa.projectoaa.core.network.model.application.SubmitApplicationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SubmitApplicationRepository @Inject constructor(
    private val api: ApplicationService
) {
    suspend fun submitApplication(request: SubmitApplicationRequest): Result<String> = withContext(
        Dispatchers.IO
    ) {
        try {
            val response = api.submitApplication(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 200) {
                    Result.success(body.message)
                } else {
                    val msg = body?.message ?: "提交失败"
                    Result.failure(Exception(msg))
                }
            } else {
                val errorBody = response.errorBody()?.toString()
                Result.failure(Exception("服务器错误:${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}