package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.data.remote.ApiConfig
import com.suseoaa.projectoaa.shared.domain.model.announcement.FetchAnnouncementInfoResponse
import com.suseoaa.projectoaa.shared.domain.model.announcement.UpdateAnnouncementInfoRequest
import com.suseoaa.projectoaa.shared.domain.model.announcement.UpdateAnnouncementInfoResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * 公告相关 API
 */
class AnnouncementApi(private val client: HttpClient) {
    
    private val baseUrl = ApiConfig.BASE_URL

    suspend fun fetchAnnouncementInfo(department: String): FetchAnnouncementInfoResponse {
        return client.get("${baseUrl}${ApiConfig.Endpoints.GET_ANNOUNCEMENT}") {
            parameter("department", department)
        }.body()
    }

    suspend fun updateAnnouncementInfo(request: UpdateAnnouncementInfoRequest): UpdateAnnouncementInfoResponse {
        return client.post("${baseUrl}${ApiConfig.Endpoints.UPDATE_ANNOUNCEMENT}") {
            setBody(request)
        }.body()
    }
}
