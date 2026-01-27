package com.suseoaa.projectoaa.data.api

import com.suseoaa.projectoaa.data.model.CourseResponseJson
import com.suseoaa.projectoaa.data.model.RSAKey
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

class SchoolApiService(
    private val client: HttpClient,
    private val json: Json
) {
    private val baseUrl = "https://jwgl.suse.edu.cn"

    suspend fun getCSRFToken(): HttpResponse {
        return client.get("$baseUrl/xtgl/login_slogin.html")
    }

    suspend fun getRSAKey(): RSAKey {
        val response = client.post("$baseUrl/xtgl/login_getPublicKey.html") {
            parameter("time", kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
        }
        return response.body()
    }

    suspend fun login(
        timestamp: String,
        username: String,
        encryptedPassword: String,
        csrfToken: String
    ): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/xtgl/login_slogin.html",
            formParameters = parameters {
                append("csrftoken", csrfToken)
                append("yhm", username)
                append("mm", encryptedPassword)
                append("mm", timestamp)
            }
        ) {
            header("Referer", "$baseUrl/xtgl/login_slogin.html")
        }
    }

    suspend fun visitUrl(url: String): HttpResponse {
        return client.get(url)
    }

    suspend fun querySchedule(year: String, semester: String): HttpResponse {
        return client.post("$baseUrl/kbcx/xskbcx_cxXsKb.html") {
            parameter("xnm", year)
            parameter("xqm", semester)
        }
    }

    suspend fun getCalendar(): HttpResponse {
        return client.get("$baseUrl/xtgl/index_cxshjxjl.html") {
            parameter("gnmkdm", "index")
            parameter("su", "")
        }
    }
}
