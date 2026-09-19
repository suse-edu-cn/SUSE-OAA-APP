package com.suseoaa.projectoaa.shared.di

import com.suseoaa.projectoaa.shared.data.local.store.AppSettingsStore
import com.suseoaa.projectoaa.shared.data.local.store.AppearanceStore
import com.suseoaa.projectoaa.shared.data.local.store.CredentialStore
import com.suseoaa.projectoaa.shared.data.local.store.SemesterStore
import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.shared.data.local.store.UserDataCleaner
import com.suseoaa.projectoaa.shared.data.local.store.UserProfileStore
import com.suseoaa.projectoaa.shared.data.local.database.CourseDatabaseDriverFactory
import com.suseoaa.projectoaa.shared.data.remote.api.CheckinApiService
import com.suseoaa.projectoaa.shared.data.remote.api.OaaApiService
import com.suseoaa.projectoaa.shared.data.remote.api.QrCodeCheckinApiService
import com.suseoaa.projectoaa.shared.data.remote.api.SchoolApiService
import com.suseoaa.projectoaa.shared.data.remote.network.ClearableCookieStorage
import com.suseoaa.projectoaa.shared.data.remote.network.OaaHttpClient
import com.suseoaa.projectoaa.shared.data.remote.network.SchoolHttpClient
import com.suseoaa.projectoaa.shared.domain.repository.AcademicStatusRepository
import com.suseoaa.projectoaa.shared.domain.repository.AnnouncementRepository
import com.suseoaa.projectoaa.shared.domain.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.data.repository.checkin.CheckinAccountStore
import com.suseoaa.projectoaa.shared.data.repository.checkin.CheckinTaskRepository
import com.suseoaa.projectoaa.shared.data.repository.checkin.CookieStorageTaskGateway
import com.suseoaa.projectoaa.shared.data.repository.checkin.SopSessionParser
import com.suseoaa.projectoaa.shared.data.repository.checkin.UiasLoginRepository
import com.suseoaa.projectoaa.shared.domain.repository.GpaRepository
import com.suseoaa.projectoaa.shared.domain.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.domain.repository.OaaAuthRepository
import com.suseoaa.projectoaa.shared.domain.repository.OaaRegisterRepository
import com.suseoaa.projectoaa.shared.domain.repository.PersonRepository
import com.suseoaa.projectoaa.shared.domain.repository.QrCodeCheckinRepository
import com.suseoaa.projectoaa.shared.domain.repository.RecruitmentRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolCourseRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolGradeRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.shared.domain.repository.TeachingPlanRepository
import com.suseoaa.projectoaa.shared.domain.repository.ValueCalculatorRepository
import com.suseoaa.projectoaa.shared.database.CourseDatabase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import com.suseoaa.projectoaa.shared.data.repository.CheckinRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.QrCodeCheckinRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.AcademicStatusRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.AnnouncementRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.GpaRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.LocalCourseRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.NearFieldCheckinRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.OaaAuthRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.OaaRegisterRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.PersonRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.RecruitmentRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.SchoolAuthRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.SchoolCourseRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.SchoolGradeRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.SchoolInfoRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.TeachingPlanRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.ValueCalculatorRepositoryImpl
import com.suseoaa.projectoaa.shared.domain.repository.NearFieldCheckinRepository
import com.suseoaa.projectoaa.shared.data.remote.network.SessionCleaner

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

    // ==================== 偏好存储（按域拆分，共用同一个 DataStore 文件）====================
    single { SessionStore(get()) }
    single { CredentialStore(get()) }
    single { UserProfileStore(get()) }
    single { AppearanceStore(get()) }
    single { SemesterStore(get()) }
    single { AppSettingsStore(get()) }
    // 网络会话清理由这里接上：datastore 模块本身不认识网络层
    single { UserDataCleaner(get(), get(), get()) { SessionCleaner.clearAllNetworkSessions() } }

    // 课程数据库
    single { CourseDatabase(get<CourseDatabaseDriverFactory>().createDriver()) }

    // ==================== GitHub API ====================
    // GitHub API HttpClient (不需要认证，AppUpdateRepository 也使用)
    // 匿名调用 api.github.com 必须带 User-Agent，否则 GitHub 直接返回 403
    single(qualifier = named("github")) {
        val jsonConfig = get<Json>()
        HttpClient {
            install(DefaultRequest) {
                header(HttpHeaders.Accept, "application/vnd.github+json")
                header(HttpHeaders.UserAgent, "SUSE-OAA-APP")
                header("X-GitHub-Api-Version", "2022-11-28")
            }
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
        val sessionStore: SessionStore = get()
        OaaHttpClient.create(get()) {
            sessionStore.cachedToken
        }
    }

    // OAA API 服务
    single { OaaApiService(get(qualifier = named("oaa")), get()) }

    // OAA 仓库
    single<OaaAuthRepository> { OaaAuthRepositoryImpl(get<OaaApiService>()) }
    single<OaaRegisterRepository> { OaaRegisterRepositoryImpl(get<OaaApiService>(), get()) }
    single<PersonRepository> { PersonRepositoryImpl(get<OaaApiService>(), get()) }
    single<AnnouncementRepository> { AnnouncementRepositoryImpl(get<OaaApiService>()) }


//    招新换届
    single { com.suseoaa.projectoaa.shared.data.remote.api.RecruitmentApiService() }
    single<RecruitmentRepository> { RecruitmentRepositoryImpl(get()) }
    // ==================== 教务系统 API ====================
    // 教务系统专用 HttpClient
    single(qualifier = named("school")) {
        SchoolHttpClient.create(get())
    }

    // 课程数据库相关
    single<LocalCourseRepository> { LocalCourseRepositoryImpl(get()) }

    // 教务系统 API
    single { SchoolApiService(get(qualifier = named("school")), get()) }
    single<SchoolAuthRepository> { SchoolAuthRepositoryImpl(get<SchoolApiService>()) }
    single<SchoolCourseRepository> { SchoolCourseRepositoryImpl(get<SchoolApiService>(), get()) }

    // 成绩和信息仓库
    single<SchoolGradeRepository> {
        SchoolGradeRepositoryImpl(
            get<SchoolApiService>(),
            get<CourseDatabase>(),
            get<Json>(),
            get<SchoolAuthRepository>(),
            get<LocalCourseRepository>(),
            get<UserProfileStore>()
        )
    }
    single<SchoolInfoRepository> {
        SchoolInfoRepositoryImpl(
            get<SchoolApiService>(),
            get<CourseDatabase>(),
            get<Json>(),
            get<SchoolAuthRepository>()
        )
    }

    // GPA 仓库
    single<GpaRepository> {
        GpaRepositoryImpl(
            get<SchoolApiService>(),
            get<SchoolGradeRepository>(),
            get<LocalCourseRepository>(),
            get<SchoolAuthRepository>(),
            get<UserProfileStore>(),
            get<Json>(),
            get<CourseDatabase>()
        )
    }

    // 教学计划仓库
    single<TeachingPlanRepository> {
        TeachingPlanRepositoryImpl(
            get<SchoolApiService>(),
            get<Json>(),
            get<SchoolAuthRepository>()
        )
    }

    // 学业情况仓库
    single<AcademicStatusRepository> {
        AcademicStatusRepositoryImpl(
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

    // 签到公共组件：账号存储、任务读写、_sop_session_ 解析，两条登录链路共用
    single { CheckinAccountStore(get<CourseDatabase>()) }
    single { CheckinTaskRepository(get<Json>()) }
    single { SopSessionParser(get<Json>()) }

    // 打卡 Repository - 密码登录 (使用 CourseDatabase)
    single<CheckinRepository> {
        val api = get<CheckinApiService>()
        CheckinRepositoryImpl(
            accountStore = get<CheckinAccountStore>(),
            loginRepository = UiasLoginRepository(
                api = api,
                cookieStorage = get<ClearableCookieStorage>(
                    qualifier = named("checkinCookieStorage")
                ),
                accountStore = get<CheckinAccountStore>()
            ),
            taskRepository = get<CheckinTaskRepository>(),
            taskGateway = CookieStorageTaskGateway(api)
        )
    }

    // 扫码签到 Repository (独立)
    single<QrCodeCheckinRepository> {
        QrCodeCheckinRepositoryImpl(
            api = get<QrCodeCheckinApiService>(),
            accountStore = get<CheckinAccountStore>(),
            taskRepository = get<CheckinTaskRepository>(),
            sopSessionParser = get<SopSessionParser>(),
            json = get<Json>(),
            cookieStorage = get<ClearableCookieStorage>(
                qualifier = named("qrCheckinCookieStorage")
            )
        )
    }

    // 近场签到 Repository
    single<NearFieldCheckinRepository> {
        NearFieldCheckinRepositoryImpl(get(), get())
    }

    // 物品价值计算 Repository
    single<ValueCalculatorRepository> { ValueCalculatorRepositoryImpl(get()) }
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
