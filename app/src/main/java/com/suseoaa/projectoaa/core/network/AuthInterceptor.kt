package com.suseoaa.projectoaa.core.network

import com.suseoaa.projectoaa.core.dataStore.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 优先使用内存缓存 (Volatile)，这是非阻塞的
        val token = tokenManager.cachedToken

        // 仅当内存缓存为空时，才尝试同步读取。
        // 注意：DataStore 首次读取需要磁盘 IO。在 App 启动页 (MainActivity)
        // 最好预先调用一次 tokenManager.tokenFlow.first() 来预热缓存。
        val finalToken = token ?: runBlocking {
            // 这里依然使用 runBlocking 作为兜底，但因为有 cachedToken 存在，
            // 只有 App 冷启动后的前几个毫秒会走到这里。
            try {
                tokenManager.getTokenSynchronously()
            } catch (e: Exception) {
                null
            }
        }

        val requestBuilder = chain.request().newBuilder()
        /*        if (!finalToken.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $finalToken")
                }*/
        if (!finalToken.isNullOrBlank()) {
            android.util.Log.d("AuthInterceptor", "正在为请求注入 Token: $finalToken")
            requestBuilder.addHeader("Authorization", "Bearer $finalToken")
        } else {
            android.util.Log.e("AuthInterceptor", "警告：Token 为空，请求将不携带认证信息")
        }
        return chain.proceed(requestBuilder.build())
    }
}