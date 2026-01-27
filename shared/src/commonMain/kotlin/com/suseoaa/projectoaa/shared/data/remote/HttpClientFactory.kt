package com.suseoaa.projectoaa.shared.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Ktor HttpClient 工厂
 */
object HttpClientFactory {

    /**
     * 创建配置好的 HttpClient
     */
    fun create(
        enableLogging: Boolean = true,
        tokenProvider: (suspend () -> String?)? = null
    ): HttpClient {
        return HttpClient {
            // JSON 序列化
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }

            // 日志
            if (enableLogging) {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            Napier.d("HTTP: $message")
                        }
                    }
                    level = LogLevel.ALL
                }
            }

            // Bearer Token 认证
            tokenProvider?.let { provider ->
                install(Auth) {
                    bearer {
                        loadTokens {
                            provider()?.let { token ->
                                BearerTokens(token, "")
                            }
                        }
                        refreshTokens {
                            provider()?.let { token ->
                                BearerTokens(token, "")
                            }
                        }
                    }
                }
            }

            // 默认请求配置
            defaultRequest {
                contentType(ContentType.Application.Json)
            }

            // 超时配置
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
        }
    }
}
