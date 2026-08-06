package com.suseoaa.projectoaa.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.suseoaa.projectoaa.presentation.MainViewModel
import com.suseoaa.projectoaa.ui.screen.changepassword.ChangePasswordScreen
import com.suseoaa.projectoaa.ui.screen.checkin.CheckinScreen
import com.suseoaa.projectoaa.ui.screen.checkin.CheckinTaskScreen
import com.suseoaa.projectoaa.ui.screen.exam.ExamInfoScreen
import com.suseoaa.projectoaa.ui.screen.forgetpassword.ForgetPasswordScreen
import com.suseoaa.projectoaa.ui.screen.gpa.GpaScreen
import com.suseoaa.projectoaa.ui.screen.grades.GradesScreen
import com.suseoaa.projectoaa.ui.screen.academic.AcademicMessagesScreen
import com.suseoaa.projectoaa.ui.screen.home.DepartmentEditScreen
import com.suseoaa.projectoaa.ui.screen.home.DepartmentDetailScreen
import com.suseoaa.projectoaa.ui.screen.login.LoginScreen
import com.suseoaa.projectoaa.ui.screen.main.MainScreen
import com.suseoaa.projectoaa.ui.screen.recruitment.RecruitmentScreen
import com.suseoaa.projectoaa.ui.screen.register.RegisterScreen
import com.suseoaa.projectoaa.ui.screen.teachingplan.AcademicStatusScreen
import com.suseoaa.projectoaa.ui.screen.teachingplan.CourseInfoScreen
import com.suseoaa.projectoaa.ui.screen.teachingplan.StudyRequirementScreen
import com.suseoaa.projectoaa.ui.screen.update.UpdateScreen
import com.suseoaa.projectoaa.presentation.course.CourseStatisticsScreen
import com.suseoaa.projectoaa.ui.screen.ailab.AiChatScreen
import com.suseoaa.projectoaa.ui.screen.ailab.AiLabScreen
import com.suseoaa.projectoaa.ui.screen.ailab.AcademicAnalysisScreen
import com.suseoaa.projectoaa.ui.screen.home.ValueCalculatorScreen
import com.suseoaa.projectoaa.util.DeepLinkManager

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Login.route,
    mainViewModel: MainViewModel
) {
    val pendingDeepLink by DeepLinkManager.pendingDeepLink.collectAsState()

    LaunchedEffect(pendingDeepLink) {
        val link = pendingDeepLink ?: return@LaunchedEffect
        DeepLinkManager.consume()

        if (link.startsWith("app://suseoaa/main")) {
            val query = if (link.contains("tab=")) link.substringAfter("tab=") else "0"
            val tabIndex = query.toIntOrNull() ?: 0
            mainViewModel.updateSelectedMainTab(tabIndex)
            
            // 安全回退到主页
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != Screen.Main.route) {
                val popped = navController.popBackStack(Screen.Main.route, inclusive = false)
                if (!popped) {
                    navController.navigate(Screen.Main.route) {
                        launchSingleTop = true
                    }
                }
            }
        } else if (link.startsWith("app://suseoaa/exams")) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != Screen.Exams.route) {
                navController.navigate(Screen.Exams.route) {
                    launchSingleTop = true
                }
            }
        }
    }

    SharedNavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgetPassword = {
                    navController.navigate(Screen.ForgetPassword.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.ForgetPassword.route) {
            ForgetPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Recruitment.route) {
            RecruitmentScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.UserManagement.route) {
            com.suseoaa.projectoaa.ui.screen.usermanagement.UserManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Main.route
        ) {
            MainScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToRecruitment = {
                    navController.navigate(Screen.Recruitment.route)
                },
                onNavigateToUserQuery = {
                    navController.navigate(Screen.UserManagement.route)
                },
                onNavigateToChangePassword = {
                    navController.navigate(Screen.ChangePassword.route)
                },
                onNavigateToGrades = {
                    navController.navigate(Screen.Grades.route)
                },
                onNavigateToGpa = {
                    navController.navigate(Screen.Gpa.route)
                },
                onNavigateToExams = {
                    navController.navigate(Screen.Exams.route)
                },
                onNavigateToAcademicMessages = {
                    navController.navigate(Screen.AcademicMessages.route)
                },
                onNavigateToDepartmentDetail = { department ->
                    navController.navigate(Screen.DepartmentDetail.createRoute(department))
                },
                onNavigateToStudyRequirement = {
                    navController.navigate(Screen.StudyRequirement.route)
                },
                onNavigateToCourseInfo = {
                    navController.navigate(Screen.CourseInfo.route)
                },
                onNavigateToAcademicStatus = {
                    navController.navigate(Screen.AcademicStatus.route)
                },
                onNavigateToCheckin = {
                    navController.navigate(Screen.Checkin.route)
                },
                onNavigateToActivityCheckin = {
                    navController.navigate(Screen.ActivityCheckin.route)
                },
                onNavigateToUpdate = {
                    navController.navigate(Screen.Update.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToCourseStatistics = {
                    navController.navigate(Screen.CourseStatistics.route)
                },
                onNavigateToAiLab = {
                    navController.navigate(Screen.AiLab.route)
                },
                onNavigateToValueCalculator = {
                    navController.navigate(Screen.ValueCalculator.route)
                },
                mainViewModel = mainViewModel
            )
        }

        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Grades.route) {
            GradesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Gpa.route) {
            GpaScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Exams.route
        ) {
            ExamInfoScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AcademicMessages.route) {
            AcademicMessagesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DepartmentDetail.route,
            arguments = Screen.DepartmentDetail.arguments
        ) { backStackEntry ->
            val department = backStackEntry.savedStateHandle.get<String>("department") ?: ""
            DepartmentDetailScreen(
                departmentName = department,
                onBack = { navController.popBackStack() },
                onNavigateToEdit = {
                    navController.navigate(Screen.DepartmentEdit.createRoute(department))
                }
            )
        }

        composable(
            route = Screen.DepartmentEdit.route,
            arguments = Screen.DepartmentEdit.arguments
        ) { backStackEntry ->
            val department = backStackEntry.savedStateHandle.get<String>("department") ?: ""
            DepartmentEditScreen(
                departmentName = department,
                onBack = { navController.popBackStack() }
            )
        }

        // 教学计划相关
        composable(Screen.StudyRequirement.route) {
            StudyRequirementScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CourseInfo.route) {
            CourseInfoScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AcademicStatus.route) {
            AcademicStatusScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 隐藏功能 - 652打卡
        composable(Screen.Checkin.route) {
            CheckinScreen(
                onBack = { navController.popBackStack() },
                onNavigateToTasks = { account ->
                    navController.navigate(Screen.CheckinTasks.createRoute(account.id))
                },
                showInlineTasks = false
            )
        }

        composable(
            route = Screen.CheckinTasks.route,
            arguments = Screen.CheckinTasks.arguments
        ) { backStackEntry ->
            val accountId = backStackEntry.savedStateHandle.get<Long>("accountId") ?: -1L
            CheckinTaskScreen(
                accountId = accountId,
                onBack = { navController.popBackStack() }
            )
        }

        // 活动签到
        composable(Screen.ActivityCheckin.route) {
            com.suseoaa.projectoaa.ui.screen.checkin.ActivityCheckinScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 检查更新
        composable(Screen.Update.route) {
            UpdateScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // 设置页面
        composable(Screen.Settings.route) {
            com.suseoaa.projectoaa.ui.screen.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = org.koin.compose.viewmodel.koinViewModel()
            )
        }

        composable(Screen.CourseStatistics.route) {
            CourseStatisticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AiLab.route) {
            AiLabScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAcademicAnalysis = { navController.navigate(Screen.AcademicAnalysis.route) },
                onNavigateToAiChat = { navController.navigate(Screen.AiChat.route) }
            )
        }

        composable(Screen.AiChat.route) {
            AiChatScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AcademicAnalysis.route) {
            AcademicAnalysisScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ValueCalculator.route) {
            ValueCalculatorScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
