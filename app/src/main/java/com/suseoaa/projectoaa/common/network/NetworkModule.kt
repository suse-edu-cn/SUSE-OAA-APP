package com.suseoaa.projectoaa.common.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.suseoaa.projectoaa.BuildConfig
import com.suseoaa.projectoaa.common.util.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
// 导入两个 ApiService
import com.suseoaa.projectoaa.login.api.ApiService as LoginApiService
import com.suseoaa.projectoaa.student.network.ApiService as StudentApiService
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = BuildConfig.API_BASE_URL

    @Singleton
    @Provides
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Singleton
    @Provides
    fun provideAuthInterceptor(sessionManager: SessionManager): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val path = originalRequest.url.encodedPath

            if (path.contains("/user/login") || path.contains("/user/register")) {
                return@Interceptor chain.proceed(originalRequest)
            }

            val token = sessionManager.jwtToken
            if (!token.isNullOrBlank()) {
                val newRequest = originalRequest.newBuilder()
                    .addHeader("Authorization", token)
                    .build()
                return@Interceptor chain.proceed(newRequest)
            }
            chain.proceed(originalRequest)
        }
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    // 提供用于登录/个人资料的 ApiService
    @Singleton
    @Provides
    fun provideLoginApiService(retrofit: Retrofit): LoginApiService {
        return retrofit.create(LoginApiService::class.java)
    }

    // 提供用于学生表单的 ApiService
    @Singleton
    @Provides
    fun provideStudentApiService(retrofit: Retrofit): StudentApiService {
        return retrofit.create(StudentApiService::class.java)
    }

    // 提供用于详情页的 ApiService
    @Singleton
    @Provides
    fun provideDetailApiService(retrofit: Retrofit): DetailApiService {
        return retrofit.create(DetailApiService::class.java)
    }
}