package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.data.api.OaaApiService
import com.suseoaa.projectoaa.shared.domain.model.recruitment.SubmitApplicationRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.SubmitApplicationResponse

class RecruitmentRepository(private val api: OaaApiService) {
    //    提交申请表
    suspend fun submitApplication(
        request: SubmitApplicationRequest
    ): Result<String> {
        return try {
            val response = api.submitApplication(request)
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