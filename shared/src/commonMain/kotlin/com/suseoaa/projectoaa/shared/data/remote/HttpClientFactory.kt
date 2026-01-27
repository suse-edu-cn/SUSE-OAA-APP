package com.suseoaa.projectoaa.shared.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Ktor HttpClient 工厂
 */
object HttpClientFactory {

    /**
     * 创建配置好的 HttpClient
     * @param enableLogging 是否启用日志
     * @param tokenProvider 同步获取 Token 的函数（返回缓存的 Token）
     */
    fun create(
        enableLogging: Boolean = true,
        tokenProvider: (() -> String?)? = null
    ): HttpClient {
        return HttpClient {
            // JSON 序列化
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    coerceInputValues = true
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

            // 默认请求配置 + Token 拦截器
            defaultRequest {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }

            // 使用 HttpRequestInterceptor 在每次请求时添加 Token
            install("AuthInterceptor") {
                requestPipeline.intercept(HttpRequestPipeline.State) {
                    tokenProvider?.invoke()?.takeIf { it.isNotBlank() }?.let { token ->
                        context.headers.append(HttpHeaders.Authorization, "Bearer $token")
                        Napier.d("AuthInterceptor: 添加 Token 到请求 - ${context.url}")
                    }
                }
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
