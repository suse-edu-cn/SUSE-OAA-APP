package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementData

/**
 * AnnouncementRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface AnnouncementRepository {

    val departments: List<String>

    suspend fun fetchAnnouncementInfo(department: String): Result<AnnouncementData>

    suspend fun updateAnnouncementInfo(department: String, content: String): Result<String>
}
