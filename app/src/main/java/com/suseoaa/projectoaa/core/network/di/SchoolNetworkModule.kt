package com.suseoaa.projectoaa.core.network.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.suseoaa.projectoaa.core.network.school.SchoolApiService
import com.suseoaa.projectoaa.core.network.school.SchoolCookieJar
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Qualifier
import javax.inject.Singleton

// 定义限定符，防止和 App 自己的 Retrofit 冲突
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SchoolNetwork

@Module
@InstallIn(SingletonComponent::class)
object SchoolNetworkModule {

    @SchoolNetwork
    @Provides
    @Singleton
    fun provideSchoolOkHttpClient(
        cookieJar: SchoolCookieJar
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(cookieJar)
            .followRedirects(false) // 禁止自动重定向，手动处理登录跳转
            .followSslRedirects(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideSchoolApiService(
        @SchoolNetwork okHttpClient: OkHttpClient, // 注入上面配置好的 Client
        json: Json
    ): SchoolApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://jwgl.suse.edu.cn")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(SchoolApiService::class.java)
    }
}