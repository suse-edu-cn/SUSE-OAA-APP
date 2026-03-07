package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaApiService
import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementData
import com.suseoaa.projectoaa.shared.domain.model.announcement.UpdateAnnouncementInfoRequest

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
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "获取公告失败" }))
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
                Result.success(response.message.ifEmpty { "更新成功" })
            } else {
                Result.failure(Exception(response.message.ifEmpty { "更新公告失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
