package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.AnnouncementApi
import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementData
import com.suseoaa.projectoaa.shared.domain.model.announcement.UpdateAnnouncementInfoRequest

/**
 * 公告仓库
 */
class AnnouncementRepository(
    private val announcementApi: AnnouncementApi
) {
    // 部门列表
    val departments = listOf("协会", "算法竞赛部", "项目实践部", "组织宣传部", "秘书处")

    /**
     * 获取部门公告
     */
    suspend fun fetchAnnouncementInfo(department: String): Result<AnnouncementData> {
        return try {
            val response = announcementApi.fetchAnnouncementInfo(department)
            if (response.code == 200 && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message, response.code)
            }
        } catch (e: Exception) {
            Result.Error("获取公告失败: ${e.message}", exception = e)
        }
    }

    /**
     * 更新部门公告 (管理员功能)
     */
    suspend fun updateAnnouncementInfo(department: String, data: String): Result<Unit> {
        return try {
            val request = UpdateAnnouncementInfoRequest(department = department, data = data)
            val response = announcementApi.updateAnnouncementInfo(request)
            if (response.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(response.message, response.code)
            }
        } catch (e: Exception) {
            Result.Error("更新公告失败: ${e.message}", exception = e)
        }
    }

    /**
     * 获取所有部门公告
     */
    suspend fun fetchAllDepartmentAnnouncements(): Map<String, AnnouncementData?> {
        val result = mutableMapOf<String, AnnouncementData?>()
        for (department in departments) {
            when (val response = fetchAnnouncementInfo(department)) {
                is Result.Success -> result[department] = response.data
                is Result.Error -> result[department] = null
                is Result.Loading -> { /* 不会发生，因为这是挂起函数 */ }
            }
        }
        return result
    }
}
