package com.suseoaa.projectoaa.data.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * OAA 后端 API HttpClient (需要 JWT Token)
 */
object OaaHttpClient {
    fun create(json: Json, tokenProvider: () -> String?): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
            
            defaultRequest {
                contentType(ContentType.Application.Json)
                // 添加 Auth Token
                val token = tokenProvider()
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }
}
