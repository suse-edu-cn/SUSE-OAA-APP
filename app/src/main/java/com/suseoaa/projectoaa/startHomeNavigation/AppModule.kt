package com.suseoaa.projectoaa.navigation // (或者你的 di 包)

import android.content.Context
import android.content.SharedPreferences
// [修改] 导入 Retrofit、Kotlinx Serialization 和 ApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.suseoaa.projectoaa.common.network.DetailApiService
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.navigation.repository.*
import com.suseoaa.projectoaa.navigation.repository.detail.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Hilt 模块 - 用于 @Provides
 * 职责：提供 Hilt 无法自动构造的类的实例 (如: SharedPreferences, Retrofit)
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 提供应用的 SharedPreferences 单例
     */
    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(SessionManager.PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * [新增] 提供 Kotlinx JSON 解析器实例
     */
    @Provides
    @Singleton
    fun provideKotlinxJson(): Json {
        return Json {
            ignoreUnknownKeys = true // 增加 API 灵活性
        }
    }

    /**
     * [新增] 提供 Retrofit 实例
     */
    @Provides
    @Singleton
    fun provideRetrofit(json: Json): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            // [ACTION REQUIRED] 请将 "https://api.example.com/" 替换为你的真实 Base URL
            .baseUrl("https://api.example.com/")
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    /**
     * [新增] 提供 DetailApiService
     */
    @Provides
    @Singleton
    fun provideDetailApiService(retrofit: Retrofit): DetailApiService {
        return retrofit.create(DetailApiService::class.java)
    }
}

/**
 * Hilt 模块 - 用于 @Binds
 * 职责：将存储库 (Repository) 的接口 (Interface) 绑定到其具体实现 (Implementation)。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingModule {

    /**
     * 绑定详情页仓库 [开发开关]
     * [当前]: FakeDetailRepository (返回随机数据)
     * [未来]: 切换到 RealDetailRepository (对接后端)
     */
    @Binds
    @Singleton
    abstract fun bindDetailRepository(
        impl: FakeDetailRepository
    ): DetailRepository

    /*
    @Binds
    @Singleton
    abstract fun bindDetailRepository(
        impl: RealDetailRepository
    ): DetailRepository
    */

    @Singleton
    @Binds
    abstract fun bindUserDataRepository(
        impl: UserDataRepositoryImpl
    ): UserDataRepository

    @Singleton
    @Binds
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Singleton
    @Binds
    abstract fun bindFeedbackRepository(
        impl: FeedbackRepositoryImpl
    ): FeedbackRepository

    @Singleton
    @Binds
    abstract fun bindImageRepository(
        impl: ImageRepositoryImpl
    ): ImageRepository

    @Singleton
    @Binds
    abstract fun bindWallpaperRepository(
        impl: WallpaperRepositoryImpl
    ): WallpaperRepository
}