package com.suseoaa.projectoaa.shared.data.remote.network

import io.ktor.client.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * OAA 后端 API HttpClient。
 *
 * v2 后端用「短期 access token + refresh token」保活：请求统一带上当前 access token，
 * 收到 HTTP 401 时交给 [OaaAuthenticator] 用 refresh token 换一个新的，再原样重放一次。
 * 以前是把明文密码存在本地、每 10 天重新登录一次，这套机制上线后就不再需要了。
 */
object OaaHttpClient {

    /**
     * 不带鉴权、不做 401 重试的裸客户端。
     *
     * 刷新 token 的请求必须走它：如果刷新请求本身也经过 401 拦截，
     * refresh token 失效时会递归触发刷新。
     */
    fun createBare(
        json: Json,
        engine: HttpClientEngine? = null,
        followRedirects: Boolean = true
    ): HttpClient = newClient(engine) {
        installCommon(json)
        this.followRedirects = followRedirects
    }

    /** [engine] 仅供测试注入 MockEngine，正常使用走平台默认引擎。 */
    fun create(json: Json, authenticator: OaaAuthenticator, engine: HttpClientEngine? = null): HttpClient {
        val client = newClient(engine) {
            installCommon(json)

            defaultRequest {
                // 不在这里统一设置 Content-Type：multipart 上传要用自己的边界值，JSON 请求在 OaaRequester 里单独声明
                // defaultRequest 的代码块每个请求都会执行一次，所以这里总能拿到最新 token
                val token = authenticator.currentAccessToken()
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }

        client.plugin(HttpSend).intercept { request ->
            val call = execute(request)
            if (call.response.status != HttpStatusCode.Unauthorized) return@intercept call

            // 没带 token 的请求（登录、注册等）返回 401 就是业务结果，不是过期
            val usedToken = request.headers[HttpHeaders.Authorization]
                ?.removePrefix("Bearer")
                ?.trim()
            if (usedToken.isNullOrEmpty()) return@intercept call

            val newToken = authenticator.refreshAccessToken(usedToken) ?: return@intercept call
            request.headers.remove(HttpHeaders.Authorization)
            request.headers.append(HttpHeaders.Authorization, "Bearer $newToken")
            execute(request)
        }

        return client
    }

    private fun newClient(engine: HttpClientEngine?, block: HttpClientConfig<*>.() -> Unit): HttpClient =
        if (engine != null) HttpClient(engine, block) else HttpClient(block)

    private fun HttpClientConfig<*>.installCommon(json: Json) {
        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
            // 请求头里有 access token，日志里不要明文输出
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }
}
