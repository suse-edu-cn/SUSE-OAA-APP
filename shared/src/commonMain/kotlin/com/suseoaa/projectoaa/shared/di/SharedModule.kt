package com.suseoaa.projectoaa.shared.di

import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.remote.HttpClientFactory
import com.suseoaa.projectoaa.shared.data.remote.api.AcademicApi
import com.suseoaa.projectoaa.shared.data.remote.api.AnnouncementApi
import com.suseoaa.projectoaa.shared.data.remote.api.AuthApi
import com.suseoaa.projectoaa.shared.data.remote.api.UserApi
import com.suseoaa.projectoaa.shared.data.repository.AcademicRepository
import com.suseoaa.projectoaa.shared.data.repository.AnnouncementRepository
import com.suseoaa.projectoaa.shared.data.repository.AuthRepository
import com.suseoaa.projectoaa.shared.data.repository.UserRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * 共享模块 - 通用
 */
val sharedModule = module {
    // Network - HttpClient
    single {
        val tokenManager: TokenManager = get()
        HttpClientFactory.create(
            enableLogging = true,
            tokenProvider = { tokenManager.cachedToken }
        )
    }

    // APIs
    single { AuthApi(get()) }
    single { UserApi(get()) }
    single { AcademicApi(get()) }
    single { AnnouncementApi(get()) }

    // Repositories
    single { AuthRepository(get(), get()) }
    single { UserRepository(get(), get()) }
    single { AcademicRepository(get()) }
    single { AnnouncementRepository(get()) }
}

/**
 * 平台特定模块
 */
expect fun platformModule(): Module

/**
 * 获取所有共享模块
 */
fun getSharedModules(): List<Module> = listOf(
    sharedModule,
    platformModule()
)
