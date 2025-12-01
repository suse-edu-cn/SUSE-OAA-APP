package com.suseoaa.projectoaa.common.network

import com.suseoaa.projectoaa.common.util.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.suseoaa.projectoaa.BuildConfig
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = BuildConfig.API_BASE_URL
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        if (path.contains("/user/login") || path.contains("/user/register")) {
            return@Interceptor chain.proceed(originalRequest)
        }

        val token = SessionManager.jwtToken
        if (!token.isNullOrBlank()) {
            val newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", token)
                .build()
            return@Interceptor chain.proceed(newRequest)
        }

        chain.proceed(originalRequest)
    }

    // 使用一个带认证拦截器的 OkHttpClient，避免重复定义未使用的 `client` 变量
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}