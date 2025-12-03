package com.suseoaa.projectoaa.common.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * "标准信息块" (DTO - Data Transfer Object)
 */
@Serializable
data class DetailBlockDto(
    @SerialName("title")
    val title: String,

    @SerialName("content")
    val content: String
)

/**
 * "通用信息页面" 完整JSON响应 (DTO)
 */
@Serializable
data class GenericDetailResponse(
    @SerialName("taskId")
    val taskId: String,

    @SerialName("title")
    val title: String,

    @SerialName("blocks")
    val blocks: List<DetailBlockDto>
)

/**
 * 任务详情页的 Retrofit API 接口
 */
interface DetailApiService {

    /**
     * 获取通用任务详情 (GET api/task/{taskId})
     * @param token 认证Token (Bearer)
     * @param taskId 任务ID
     */
    @GET("api/task/{taskId}")
    suspend fun getTaskDetails(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: String
    ): Response<GenericDetailResponse>
}