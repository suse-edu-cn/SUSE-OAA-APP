package com.suseoaa.projectoaa.startHomeNavigation.network

import com.suseoaa.projectoaa.startHomeNavigation.model.* // 导入上面的所有模型
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 全局 API 接口定义
 */
interface ApiService {

    // ===========================
    // 👤 用户模块
    // ===========================
    @GET("user/info")
    suspend fun getUserInfo(
        @Header("Authorization") token: String
    ): Response<BaseResponse<UserInfoResponse>>

    // ===========================
    // 📅 打卡模块
    // ===========================
    @GET("checkin/status")
    suspend fun getCheckInStatus(
        @Query("userId") userId: String
    ): Response<BaseResponse<CheckInStatusResponse>>

    @POST("checkin/submit")
    suspend fun submitCheckIn(
        @Body request: CheckInRequest
    ): Response<BaseResponse<Unit>>

    // ===========================
    // 🔮 运势模块
    // ===========================
    @GET("fortune/daily")
    suspend fun getDailyFortune(
        @Query("date") date: String
    ): Response<BaseResponse<DailyFortuneResponse>>
}