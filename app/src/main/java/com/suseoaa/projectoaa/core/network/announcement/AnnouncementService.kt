package com.suseoaa.projectoaa.core.network.announcement

import com.suseoaa.projectoaa.core.network.model.announcement.FetchAnnouncementInfoResponse
import com.suseoaa.projectoaa.core.network.model.announcement.UpdateAnnouncementInfoRequest
import com.suseoaa.projectoaa.core.network.model.announcement.UpdateAnnouncementInfoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AnnouncementService {
    //获取公告信息
    @GET("/announcement/GetAnnouncement")
    suspend fun getAnnouncementInfo(@Query("department") department: String): Response<FetchAnnouncementInfoResponse>

    //更新公告信息
    @POST("/announcement/UpdateAnnouncement")
    suspend fun updateAnnouncement(@Body request: UpdateAnnouncementInfoRequest): Response<UpdateAnnouncementInfoResponse>
}