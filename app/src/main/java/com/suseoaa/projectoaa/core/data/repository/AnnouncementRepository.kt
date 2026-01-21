package com.suseoaa.projectoaa.core.data.repository

import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.network.announcement.AnnouncementService
import com.suseoaa.projectoaa.core.network.model.announcement.FetchAnnouncementInfoResponse
import com.suseoaa.projectoaa.core.network.model.announcement.UpdateAnnouncementInfoRequest
import com.suseoaa.projectoaa.core.network.model.announcement.UpdateAnnouncementInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.http.Body
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AnnouncementRepository @Inject constructor(
    private val api: AnnouncementService
) {
    suspend fun fetchAnnouncementInfo(request: String): Result<FetchAnnouncementInfoResponse.Data> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getAnnouncementInfo(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.code == 200) {
                        Result.success(body.data)
                    } else {
                        Result.failure(Exception(body?.message ?: "加载失败"))
                    }
                } else {
                    val errorMessage =
                        response.errorBody()?.string() ?: "请求错误：${response.code()}"
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }


    suspend fun updateAnnouncementInfo(request: UpdateAnnouncementInfoRequest): Result<String> =
        withContext(
            Dispatchers.IO
        ) {
            try {
                val response = api.updateAnnouncement(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.code == 200) {
                        Result.success(body.message)
                    } else {
                        Result.failure(Exception(body?.message ?: "更新失败"))
                    }
                } else {
                    val errorMessage =
                        response.errorBody()?.string() ?: "更新失败:${response.code()}"
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}