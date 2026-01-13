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
        // 直接从内存读取，不再阻塞线程
        val token = tokenManager.cachedToken
        // 只有当内存为空时（极少情况），才回退到 runBlocking，或者你可以选择接受空 token
            ?: runBlocking { tokenManager.tokenFlow.first() }

        val requestBuilder = chain.request().newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}