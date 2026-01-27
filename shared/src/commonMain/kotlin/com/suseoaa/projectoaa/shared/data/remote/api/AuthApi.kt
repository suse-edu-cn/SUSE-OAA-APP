package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.data.remote.ApiConfig
import com.suseoaa.projectoaa.shared.domain.model.login.LoginRequest
import com.suseoaa.projectoaa.shared.domain.model.login.LoginResponse
import com.suseoaa.projectoaa.shared.domain.model.register.RegisterRequest
import com.suseoaa.projectoaa.shared.domain.model.register.RegisterResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * 认证相关 API
 */
class AuthApi(private val client: HttpClient) {
    
    private val baseUrl = ApiConfig.BASE_URL

    suspend fun login(request: LoginRequest): LoginResponse {
        return client.post("$baseUrl${ApiConfig.Endpoints.LOGIN}") {
            setBody(request)
        }.body()
    }

    suspend fun register(request: RegisterRequest): RegisterResponse {
        return client.post("$baseUrl${ApiConfig.Endpoints.REGISTER}") {
            setBody(request)
        }.body()
    }
}
