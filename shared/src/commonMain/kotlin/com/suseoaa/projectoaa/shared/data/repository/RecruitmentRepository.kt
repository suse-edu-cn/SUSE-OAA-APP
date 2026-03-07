package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaApiService
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
                Result.success(response.message.ifEmpty { "提交成功" })
            } else {
                Result.failure(Exception(response.message.ifEmpty { "提交申请失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}