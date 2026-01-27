package com.suseoaa.projectoaa.di

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
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * ComposeApp 模块的 Koin 依赖注入配置
 */
val appModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ChangePasswordViewModel(get()) }
    viewModel { CourseViewModel() }
    viewModel { AcademicViewModel(get()) }
    viewModel { PersonViewModel(get(), get()) }
    viewModel { GpaViewModel(get()) }
    viewModel { GradesViewModel(get()) }
}

// 重导出 shared 模块
val sharedModule = com.suseoaa.projectoaa.shared.di.sharedModule
val platformModule = com.suseoaa.projectoaa.shared.di.platformModule()
