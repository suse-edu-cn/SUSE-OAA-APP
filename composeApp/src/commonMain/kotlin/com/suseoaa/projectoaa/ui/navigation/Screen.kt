package com.suseoaa.projectoaa.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * 应用导航路由定义
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Main : Screen("main")
    data object ChangePassword : Screen("changePassword")
    data object Grades : Screen("grades")
    data object Gpa : Screen("gpa")
    data object Exams : Screen("exams")
    data object Recruitment : Screen("recruitment")

    // 教学计划相关
    data object StudyRequirement : Screen("studyRequirement")
    data object CourseInfo : Screen("courseInfo")
    data object AcademicStatus : Screen("academicStatus")

    // 隐藏功能
    data object Checkin : Screen("checkin")

    data object DepartmentDetail : Screen("department/{department}") {
        fun createRoute(department: String) = "department/$department"
        val arguments = listOf(
            navArgument("department") { type = NavType.StringType }
        )
    }
}
