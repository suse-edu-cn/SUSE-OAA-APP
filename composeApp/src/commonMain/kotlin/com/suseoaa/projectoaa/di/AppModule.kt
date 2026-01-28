package com.suseoaa.projectoaa.di

import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.api.OaaApiService
import com.suseoaa.projectoaa.data.api.SchoolApiService
import com.suseoaa.projectoaa.data.database.CourseDatabaseDriverFactory
import com.suseoaa.projectoaa.data.network.OaaHttpClient
import com.suseoaa.projectoaa.data.network.SchoolHttpClient
import com.suseoaa.projectoaa.data.repository.AnnouncementRepository
import com.suseoaa.projectoaa.data.repository.AppUpdateRepository
import com.suseoaa.projectoaa.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.data.repository.OaaAuthRepository
import com.suseoaa.projectoaa.data.repository.OaaRegisterRepository
import com.suseoaa.projectoaa.data.repository.PersonRepository
import com.suseoaa.projectoaa.data.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.data.repository.SchoolCourseRepository
import com.suseoaa.projectoaa.data.repository.SchoolGradeRepository
import com.suseoaa.projectoaa.data.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.database.CourseDatabase
import com.suseoaa.projectoaa.presentation.MainViewModel
import com.suseoaa.projectoaa.presentation.academic.AcademicViewModel
import com.suseoaa.projectoaa.presentation.changepassword.ChangePasswordViewModel
import com.suseoaa.projectoaa.presentation.course.CourseViewModel
import com.suseoaa.projectoaa.presentation.gpa.GpaViewModel
import com.suseoaa.projectoaa.presentation.grades.GradesViewModel
import com.suseoaa.projectoaa.presentation.home.HomeViewModel
import com.suseoaa.projectoaa.presentation.login.LoginViewModel
import com.suseoaa.projectoaa.presentation.person.PersonViewModel
import com.suseoaa.projectoaa.presentation.register.RegisterViewModel
import com.suseoaa.projectoaa.presentation.update.AppUpdateViewModel
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

    // ==================== ViewModels ====================
    viewModel { MainViewModel(get()) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ChangePasswordViewModel(get()) }
    viewModel { CourseViewModel(get(), get(), get(), get()) }
    viewModel { AcademicViewModel(get(), get(), get(), get()) }
    viewModel { PersonViewModel(get()) }
    viewModel { GpaViewModel(get(), get()) }
    viewModel { GradesViewModel(get(), get(), get(), get()) }
    viewModel { AppUpdateViewModel(get()) }
}
