package com.suseoaa.projectoaa.data.api

import com.suseoaa.projectoaa.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

/**
 * OAA 后端 API 服务 (非教务系统)
 * 用于登录、注册、公告、用户信息等
 */
class OaaApiService(
    private val client: HttpClient,
    private val json: Json
) {
    private val baseUrl = "https://api.suseoaa.com"

    // ==================== 登录 ====================
    suspend fun login(request: LoginRequest): LoginResponse {
        val response = client.post("$baseUrl/user/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }

    // ==================== 注册 ====================
    suspend fun register(request: RegisterRequest): RegisterResponse {
        val response = client.post("$baseUrl/user/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }

    // ==================== 用户信息 ====================
    suspend fun getPersonInfo(): PersonResponse {
        val response = client.get("$baseUrl/user/Info")
        return response.body()
    }

    suspend fun updateUserInfo(request: UpdateUserRequest): UpdatePersonResponse {
        val response = client.post("$baseUrl/user/update") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }
    
    suspend fun uploadAvatar(imageData: ByteArray): UploadAvatarResponse {
        val response = client.submitFormWithBinaryData(
            url = "$baseUrl/user/uploadimg",
            formData = formData {
                append("Image", imageData, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"avatar.jpg\"")
                })
            }
        )
        return response.body()
    }

    // ==================== 修改密码 ====================
    suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse {
        val response = client.post("$baseUrl/user/updatePassword") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }

    // ==================== 公告 ====================
    suspend fun getAnnouncementInfo(department: String): FetchAnnouncementInfoResponse {
        val response = client.get("$baseUrl/announcement/GetAnnouncement") {
            parameter("department", department)
        }
        return response.body()
    }

    suspend fun updateAnnouncement(request: UpdateAnnouncementInfoRequest): UpdateAnnouncementInfoResponse {
        val response = client.post("$baseUrl/announcement/UpdateAnnouncement") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }
}
