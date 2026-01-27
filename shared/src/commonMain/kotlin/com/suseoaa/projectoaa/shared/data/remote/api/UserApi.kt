package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.data.remote.ApiConfig
import com.suseoaa.projectoaa.shared.domain.model.person.PersonResponse
import com.suseoaa.projectoaa.shared.domain.model.person.UpdatePersonResponse
import com.suseoaa.projectoaa.shared.domain.model.person.UpdateUserRequest
import com.suseoaa.projectoaa.shared.domain.model.changePassword.ChangePasswordRequest
import com.suseoaa.projectoaa.shared.domain.model.changePassword.ChangePasswordResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * 用户相关 API
 */
class UserApi(private val client: HttpClient) {
    
    private val baseUrl = ApiConfig.BASE_URL

    suspend fun getUserInfo(): PersonResponse {
        return client.get("$baseUrl${ApiConfig.Endpoints.USER_INFO}").body()
    }

    suspend fun updateUser(request: UpdateUserRequest): UpdatePersonResponse {
        return client.post("$baseUrl${ApiConfig.Endpoints.UPDATE_USER}") {
            setBody(request)
        }.body()
    }

    suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse {
        return client.post("$baseUrl${ApiConfig.Endpoints.CHANGE_PASSWORD}") {
            setBody(request)
        }.body()
    }
}
