package com.suseoaa.projectoaa.di

import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.api.CheckinApiService
import com.suseoaa.projectoaa.data.api.OaaApiService
import com.suseoaa.projectoaa.data.api.SchoolApiService
import com.suseoaa.projectoaa.data.database.CourseDatabaseDriverFactory
import com.suseoaa.projectoaa.data.network.OaaHttpClient
import com.suseoaa.projectoaa.data.network.SchoolHttpClient
import com.suseoaa.projectoaa.data.repository.AnnouncementRepository
import com.suseoaa.projectoaa.data.repository.AppUpdateRepository
import com.suseoaa.projectoaa.data.repository.CheckinRepository
import com.suseoaa.projectoaa.data.repository.GpaRepository
import com.suseoaa.projectoaa.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.data.repository.OaaAuthRepository
import com.suseoaa.projectoaa.data.repository.OaaRegisterRepository
import com.suseoaa.projectoaa.data.repository.PersonRepository
import com.suseoaa.projectoaa.data.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.data.repository.SchoolCourseRepository
import com.suseoaa.projectoaa.data.repository.SchoolGradeRepository
import com.suseoaa.projectoaa.data.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.data.repository.AcademicStatusRepository
import com.suseoaa.projectoaa.data.repository.TeachingPlanRepository
import com.suseoaa.projectoaa.database.CourseDatabase
import com.suseoaa.projectoaa.presentation.MainViewModel
import com.suseoaa.projectoaa.presentation.academic.AcademicViewModel
import com.suseoaa.projectoaa.presentation.changepassword.ChangePasswordViewModel
import com.suseoaa.projectoaa.presentation.checkin.CheckinViewModel
import com.suseoaa.projectoaa.presentation.course.CourseViewModel
import com.suseoaa.projectoaa.presentation.gpa.GpaViewModel
import com.suseoaa.projectoaa.presentation.grades.GradesViewModel
import com.suseoaa.projectoaa.presentation.home.HomeViewModel
import com.suseoaa.projectoaa.presentation.login.LoginViewModel
import com.suseoaa.projectoaa.presentation.person.PersonViewModel
import com.suseoaa.projectoaa.presentation.register.RegisterViewModel
import com.suseoaa.projectoaa.presentation.exam.ExamViewModel
import com.suseoaa.projectoaa.presentation.teachingplan.AcademicStatusViewModel
import com.suseoaa.projectoaa.presentation.teachingplan.CourseInfoViewModel
import com.suseoaa.projectoaa.presentation.teachingplan.StudyRequirementViewModel
import com.suseoaa.projectoaa.presentation.update.AppUpdateViewModel
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * ComposeApp 模块的 Koin 依赖注入配置
 */
val appModule = module {
    // JSON 配置
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    // TokenManager (需要从 PlatformModule 获取 DataStore)
    single { TokenManager(get()) }

    // ==================== GitHub API ====================
    // GitHub API HttpClient (不需要认证)
    single(qualifier = org.koin.core.qualifier.named("github")) {
        val jsonConfig = get<Json>()
        io.ktor.client.HttpClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(jsonConfig)
            }
            install(io.ktor.client.plugins.HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
        }
    }

    // ==================== OAA 后端 API ====================
    // OAA 后端 HttpClient (需要 Token)
    single(qualifier = org.koin.core.qualifier.named("oaa")) {
        val tokenManager: TokenManager = get()
        OaaHttpClient.create(get()) {
            tokenManager.cachedToken
        }
    }

    // OAA API 服务
    single { OaaApiService(get(qualifier = org.koin.core.qualifier.named("oaa")), get()) }

    // OAA Repositories
    single { OaaAuthRepository(get<OaaApiService>()) }
    single { OaaRegisterRepository(get<OaaApiService>(), get()) }
    single { PersonRepository(get<OaaApiService>(), get()) }
    single { AnnouncementRepository(get<OaaApiService>()) }

    // ==================== 教务系统 API ====================
    // 教务系统专用 HttpClient
    single(qualifier = org.koin.core.qualifier.named("school")) {
        SchoolHttpClient.create(get())
    }

    // 课程数据库
    single { CourseDatabase(get<CourseDatabaseDriverFactory>().createDriver()) }
    single { LocalCourseRepository(get()) }

    // 教务系统 API
    single { SchoolApiService(get(qualifier = org.koin.core.qualifier.named("school")), get()) }
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

    // ==================== ViewModels ====================
    viewModel { MainViewModel(get()) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ChangePasswordViewModel(get()) }
    viewModel { CourseViewModel(get(), get(), get(), get()) }
    viewModel { AcademicViewModel(get(), get(), get(), get()) }
    viewModel { ExamViewModel(get(), get(), get(), get()) }
    viewModel { PersonViewModel(get(), get()) }
    viewModel { GpaViewModel(get(), get()) }
    viewModel { GradesViewModel(get(), get(), get(), get()) }
    viewModel { AppUpdateViewModel(get(), get()) }
    
    // 教学计划 ViewModels
    viewModel { StudyRequirementViewModel(get()) }
    viewModel { CourseInfoViewModel(get(), get(), get(), get()) }
    viewModel { AcademicStatusViewModel(get(), get()) }
    
    // ==================== 652打卡（隐藏功能）====================
    // 打卡专用 HttpClient (使用独立的 Cookie 存储)
    single(qualifier = org.koin.core.qualifier.named("checkin")) {
        val jsonConfig = get<Json>()
        io.ktor.client.HttpClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(jsonConfig)
            }
            install(io.ktor.client.plugins.HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
            install(io.ktor.client.plugins.cookies.HttpCookies) {
                storage = io.ktor.client.plugins.cookies.AcceptAllCookiesStorage()
            }
            followRedirects = false
        }
    }
    
    // 打卡 API 服务
    single { CheckinApiService(get(qualifier = org.koin.core.qualifier.named("checkin"))) }
    
    // 打卡 Repository (使用 CourseDatabase)
    single { 
        CheckinRepository(
            get<CheckinApiService>(),
            get<CourseDatabase>(),
            get<Json>()
        )
    }
    
    // 打卡 ViewModel
    viewModel { CheckinViewModel(get()) }
}
