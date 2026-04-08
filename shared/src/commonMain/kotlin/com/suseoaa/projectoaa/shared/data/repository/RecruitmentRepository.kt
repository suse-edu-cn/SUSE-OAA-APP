package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.RecruitmentApiService
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ChangeStatusRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ChangeTimeRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.RecruitmentApplication
import com.suseoaa.projectoaa.shared.domain.model.recruitment.RecruitmentResponse
import com.suseoaa.projectoaa.shared.domain.model.recruitment.toSubmitRequest

class RecruitmentRepository(private val apiService: RecruitmentApiService) {

    suspend fun createApplication(application: RecruitmentApplication): Result<RecruitmentResponse<Unit>> {
        return try {
            val response = apiService.createApplication(application.toSubmitRequest())
            if (response.code == 200) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "提交申请失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplications(): Result<RecruitmentResponse<List<RecruitmentApplication>>> {
        return try {
            val response = apiService.getApplications()
            if (response.code == 200) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "获取申请失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateApplication(application: RecruitmentApplication): Result<RecruitmentResponse<RecruitmentApplication>> {
        return try {
            val response = apiService.updateApplication(application.toSubmitRequest())
            if (response.code == 200) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "更新失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImage(imageBytes: ByteArray, filename: String): Result<String> {
        return try {
            val response = apiService.uploadImage(imageBytes, filename)
            if (response.code == 200) {
                Result.success(response.data.orEmpty().ifBlank { response.message.ifEmpty { "上传成功" } })
            } else {
                Result.failure(Exception(response.message.ifEmpty { "上传失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTime(request: ChangeTimeRequest): Result<String> {
        return try {
            val response = apiService.updateTime(request)
            if (response.code == 200) {
                val payload = response.data
                val formatted = if (payload == null || payload.starttime.isBlank() || payload.endtime.isBlank()) {
                    response.message.ifEmpty { "修改时间成功" }
                } else {
                    "${response.message.ifEmpty { "修改时间成功" }}：${payload.starttime} 至 ${payload.endtime}"
                }
                Result.success(formatted)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "修改时间失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changeStatus(request: ChangeStatusRequest): Result<String> {
        return try {
            val raw = apiService.changeStatus(request)
            val normalized = normalizeRawResponse(raw)
            if (normalized == "更新成功") {
                Result.success(normalized)
            } else {
                Result.failure(Exception(extractErrorMessage(normalized)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeRawResponse(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length >= 2 && trimmed.first() == '"' && trimmed.last() == '"') {
            return trimmed.substring(1, trimmed.lastIndex)
        }
        return trimmed
    }

    private fun extractErrorMessage(raw: String): String {
        if (raw.isBlank()) return "更新失败"
        val regex = "\"message\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        val matched = regex.find(raw)?.groupValues?.getOrNull(1)
        return matched?.ifBlank { "更新失败" } ?: raw
    }
}
