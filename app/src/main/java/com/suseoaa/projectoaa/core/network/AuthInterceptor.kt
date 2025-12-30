package com.suseoaa.projectoaa.core.network

import com.suseoaa.projectoaa.core.dataStore.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 1. 同步读取当前的 Token (使用 runBlocking 阻塞当前线程直到读到数据)
        // first() 会获取 Flow 当前的最新值
        val token = runBlocking {
            tokenManager.tokenFlow.first()
        }

        val requestBuilder = chain.request().newBuilder()

        // 2. 如果有 Token，就塞进 Header
        if (!token.isNullOrBlank()) {
            // 注意：Bearer 后面有个空格
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}