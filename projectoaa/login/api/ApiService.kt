package com.suseoaa.projectoaa.login.api

import com.suseoaa.projectoaa.login.model.ApiResponse
import com.suseoaa.projectoaa.login.model.LoginData
import com.suseoaa.projectoaa.login.model.LoginRequest
import com.suseoaa.projectoaa.login.model.RegisterRequest
import com.suseoaa.projectoaa.login.model.UpdatePasswordRequest
import com.suseoaa.projectoaa.login.model.UserInfoResponse
import com.suseoaa.projectoaa.login.model.UpdateUserInfoRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("user/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginData>>

    @POST("user/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<Any>>

    @GET("user/Info")
    suspend fun getUserInfo(
        @Header("Authorization") token: String
    ): Response<UserInfoResponse>

    @POST("user/update")
    suspend fun updateUserInfo(
        @Header("Authorization") token: String,
        @Body request: UpdateUserInfoRequest
    ): Response<ApiResponse<Any>>

    @POST("user/password")
    suspend fun updatePassword(
        @Header("Authorization") token: String,
        @Body request: UpdatePasswordRequest
    ): Response<ApiResponse<Any>>
}