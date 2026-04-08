package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.domain.model.recruitment.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class RecruitmentApiService : KoinComponent {
    private val client: HttpClient by inject(named("oaa"))

    private val baseUrl = "https://api.suseoaa.com"

    suspend fun createApplication(request: RecruitmentSubmitRequest): RecruitmentResponse<Unit> {
        return client.post("$baseUrl/application/create") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getApplications(): RecruitmentResponse<List<RecruitmentApplication>> {
        return client.get("$baseUrl/application/get").body()
    }

    suspend fun updateApplication(request: RecruitmentSubmitRequest): RecruitmentResponse<RecruitmentApplication> {
        return client.post("$baseUrl/application/update") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun uploadImage(imageBytes: ByteArray, filename: String): RecruitmentResponse<String> {
        return client.post("$baseUrl/application/uploadimg") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("Image", imageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                        })
                    }
                )
            )
        }.body()
    }

    suspend fun updateTime(request: ChangeTimeRequest): RecruitmentResponse<RecruitmentTimeWindow> {
        return client.post("$baseUrl/application/updatetime") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun changeStatus(request: ChangeStatusRequest): String {
        return client.post("$baseUrl/application/changestatus") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
