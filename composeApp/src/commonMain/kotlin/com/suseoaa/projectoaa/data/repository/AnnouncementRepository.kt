package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.data.api.OaaApiService
import com.suseoaa.projectoaa.data.model.AnnouncementData
import com.suseoaa.projectoaa.data.model.UpdateAnnouncementInfoRequest

/**
 * 公告信息仓库
 */
class AnnouncementRepository(
    private val api: OaaApiService
) {
    val departments = listOf("协会", "算法竞赛部", "项目实践部", "组织宣传部", "秘书处")

    suspend fun fetchAnnouncementInfo(department: String): Result<AnnouncementData> {
        return try {
            val response = api.getAnnouncementInfo(department)
            if (response.code == 200) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAnnouncementInfo(department: String, content: String): Result<String> {
        return try {
            val request = UpdateAnnouncementInfoRequest(
                department = department,
                updateinfo = content
            )
            val response = api.updateAnnouncement(request)
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
