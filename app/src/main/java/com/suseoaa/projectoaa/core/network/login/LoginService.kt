package com.suseoaa.projectoaa.core.network.login

import com.suseoaa.projectoaa.core.network.model.login.LoginRequest
import com.suseoaa.projectoaa.core.network.model.login.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginService {
    @POST("user/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}