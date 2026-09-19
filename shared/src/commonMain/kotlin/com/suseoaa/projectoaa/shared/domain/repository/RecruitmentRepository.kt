package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.recruitment.ChangeStatusRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ChangeTimeRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.RecruitmentApplication
import com.suseoaa.projectoaa.shared.domain.model.recruitment.RecruitmentResponse

/**
 * RecruitmentRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface RecruitmentRepository {

    suspend fun createApplication(application: RecruitmentApplication): Result<RecruitmentResponse<Unit>>

    suspend fun getApplications(): Result<RecruitmentResponse<List<RecruitmentApplication>>>

    suspend fun updateApplication(application: RecruitmentApplication): Result<RecruitmentResponse<RecruitmentApplication>>

    suspend fun uploadImage(imageBytes: ByteArray, filename: String): Result<String>

    suspend fun updateTime(request: ChangeTimeRequest): Result<String>

    suspend fun changeStatus(request: ChangeStatusRequest): Result<String>
}
