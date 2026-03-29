package com.suseoaa.projectoaa.di

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
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * ComposeApp 模块 - 仅 ViewModel 注册
 * 数据层 DI 已迁移至 shared/di/SharedModule.kt
 */
val appModule = module {
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
    single { AppUpdateViewModel(get(), get()) }
    viewModel { RegisterViewModel(get()) }
    // 教学计划 ViewModels
    viewModel { StudyRequirementViewModel(get()) }
    viewModel { CourseInfoViewModel(get(), get(), get(), get()) }
    viewModel { AcademicStatusViewModel(get(), get()) }

    // 打卡 ViewModel (同时注入两个 Repository)
    viewModel { CheckinViewModel(get(), get()) }
}
