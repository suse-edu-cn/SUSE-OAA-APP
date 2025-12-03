package com.suseoaa.projectoaa.startHomeNavigation

import android.content.Context
import android.content.SharedPreferences
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.startHomeNavigation.repository.*
import com.suseoaa.projectoaa.startHomeNavigation.repository.detail.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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

    // 注意: Json 和 Retrofit 实例由 NetworkModule 提供，避免重复绑定
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