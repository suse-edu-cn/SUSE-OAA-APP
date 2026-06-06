package com.suseoaa.projectoaa.shared.di

import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.local.database.CourseDatabaseDriverFactory
import com.suseoaa.projectoaa.shared.data.remote.api.CheckinApiService
import com.suseoaa.projectoaa.shared.data.remote.api.OaaApiService
import com.suseoaa.projectoaa.shared.data.remote.api.QrCodeCheckinApiService
import com.suseoaa.projectoaa.shared.data.remote.api.SchoolApiService
import com.suseoaa.projectoaa.shared.data.remote.network.ClearableCookieStorage
import com.suseoaa.projectoaa.shared.data.remote.network.OaaHttpClient
import com.suseoaa.projectoaa.shared.data.remote.network.SchoolHttpClient
import com.suseoaa.projectoaa.shared.data.repository.AcademicStatusRepository
import com.suseoaa.projectoaa.shared.data.repository.AnnouncementRepository
import com.suseoaa.projectoaa.shared.data.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.data.repository.GpaRepository
import com.suseoaa.projectoaa.shared.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.data.repository.OaaAuthRepository
import com.suseoaa.projectoaa.shared.data.repository.OaaRegisterRepository
import com.suseoaa.projectoaa.shared.data.repository.PersonRepository
import com.suseoaa.projectoaa.shared.data.repository.QrCodeCheckinRepository
import com.suseoaa.projectoaa.shared.data.repository.RecruitmentRepository
import com.suseoaa.projectoaa.shared.data.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.shared.data.repository.SchoolCourseRepository
import com.suseoaa.projectoaa.shared.data.repository.SchoolGradeRepository
import com.suseoaa.projectoaa.shared.data.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.shared.data.repository.TeachingPlanRepository
import com.suseoaa.projectoaa.shared.database.CourseDatabase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * 共享模块 - 所有数据层的 DI 注册
 * 包括 JSON、HttpClients、API 服务、数据库、Repository
 */
val sharedModule = module {
    // ==================== 基础设施 ====================
    // JSON 配置
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    // 课程数据库
    single { CourseDatabase(get<CourseDatabaseDriverFactory>().createDriver()) }

    // ==================== GitHub API ====================
    // GitHub API HttpClient (不需要认证，AppUpdateRepository 也使用)
    single(qualifier = named("github")) {
        val jsonConfig = get<Json>()
        HttpClient {
            install(ContentNegotiation) {
                json(jsonConfig)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
        }
    }

    // ==================== OAA 后端 API ====================
    // OAA 后端 HttpClient (需要 Token)
    single(qualifier = named("oaa")) {
        val tokenManager: TokenManager = get()
        OaaHttpClient.create(get()) {
            tokenManager.cachedToken
        }
    }

    // OAA API 服务
    single { OaaApiService(get(qualifier = named("oaa")), get()) }

    // OAA 仓库
    single { OaaAuthRepository(get<OaaApiService>()) }
    single { OaaRegisterRepository(get<OaaApiService>(), get()) }
    single { PersonRepository(get<OaaApiService>(), get()) }
    single { AnnouncementRepository(get<OaaApiService>()) }


//    招新换届
    single { com.suseoaa.projectoaa.shared.data.remote.api.RecruitmentApiService() }
    single { RecruitmentRepository(get()) }
    // ==================== 教务系统 API ====================
    // 教务系统专用 HttpClient
    single(qualifier = named("school")) {
        SchoolHttpClient.create(get())
    }

    // 课程数据库相关
    single { LocalCourseRepository(get()) }

    // 教务系统 API
    single { SchoolApiService(get(qualifier = named("school")), get()) }
    single { SchoolAuthRepository(get<SchoolApiService>()) }
    single { SchoolCourseRepository(get<SchoolApiService>(), get()) }

    // 成绩和信息仓库
    single {
        SchoolGradeRepository(
            get<SchoolApiService>(),
            get<CourseDatabase>(),
            get<Json>(),
            get<SchoolAuthRepository>(),
            get<LocalCourseRepository>(),
            get<TokenManager>()
        )
    }
    single {
        SchoolInfoRepository(
            get<SchoolApiService>(),
            get<CourseDatabase>(),
            get<Json>(),
            get<SchoolAuthRepository>()
        )
    }

    // GPA 仓库
    single {
        GpaRepository(
            get<SchoolApiService>(),
            get<SchoolGradeRepository>(),
            get<LocalCourseRepository>(),
            get<SchoolAuthRepository>(),
            get<TokenManager>(),
            get<Json>(),
            get<CourseDatabase>()
        )
    }

    // 教学计划仓库
    single {
        TeachingPlanRepository(
            get<SchoolApiService>(),
            get<Json>(),
            get<SchoolAuthRepository>()
        )
    }

    // 学业情况仓库
    single {
        AcademicStatusRepository(
            get<SchoolApiService>(),
            get<Json>()
        )
    }

    // ==================== 652打卡（隐藏功能）====================
    // 可清除的 Cookie 存储
    single(qualifier = named("checkinCookieStorage")) {
        ClearableCookieStorage()
    }

    // 扫码签到专用 Cookie 存储 (与密码登录隔离)
    single(qualifier = named("qrCheckinCookieStorage")) {
        ClearableCookieStorage()
    }

    // 打卡专用 HttpClient (使用可清除的 Cookie 存储) - 密码登录用
    single(qualifier = named("checkin")) {
        val jsonConfig = get<Json>()
        val cookieStorage = get<ClearableCookieStorage>(
            qualifier = named("checkinCookieStorage")
        )
        HttpClient {
            install(ContentNegotiation) {
                json(jsonConfig)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
            install(HttpCookies) {
                storage = cookieStorage
            }
            followRedirects = false
        }
    }

    // 扫码签到专用 HttpClient (独立的 Cookie 存储)
    single(qualifier = named("qrCheckin")) {
        val jsonConfig = get<Json>()
        val cookieStorage = get<ClearableCookieStorage>(
            qualifier = named("qrCheckinCookieStorage")
        )
        HttpClient {
            install(ContentNegotiation) {
                json(jsonConfig)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
            install(HttpCookies) {
                storage = cookieStorage
            }
            followRedirects = false
        }
    }

    // 打卡 API 服务 (密码登录)
    single { CheckinApiService(get(qualifier = named("checkin"))) }

    // 扫码签到 API 服务
    single { QrCodeCheckinApiService(get(qualifier = named("qrCheckin"))) }

    // 打卡 Repository - 密码登录 (使用 CourseDatabase)
    single {
        CheckinRepository(
            get<CheckinApiService>(),
            get<CourseDatabase>(),
            get<Json>(),
            get<ClearableCookieStorage>(qualifier = named("checkinCookieStorage"))
        )
    }

    // 扫码签到 Repository (独立)
    single {
        QrCodeCheckinRepository(
            get<QrCodeCheckinApiService>(),
            get<CourseDatabase>(),
            get<Json>(),
            get<ClearableCookieStorage>(qualifier = named("qrCheckinCookieStorage"))
        )
    }

    // 近场签到 Repository
    single {
        com.suseoaa.projectoaa.shared.data.repository.NearFieldCheckinRepository(get(), get())
    }
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
