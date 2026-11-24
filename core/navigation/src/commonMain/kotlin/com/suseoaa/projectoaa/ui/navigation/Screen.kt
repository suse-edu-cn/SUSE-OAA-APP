package com.suseoaa.projectoaa.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * 应用导航路由定义
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgetPassword : Screen("forgetPassword")
    data object Main : Screen("main")
    data object ChangePassword : Screen("changePassword")
    data object Grades : Screen("grades")
    data object Gpa : Screen("gpa")
    data object Exams : Screen("exams")
    data object Recruitment : Screen("recruitment")
    data object UserManagement : Screen("userManagement")
    data object Organization : Screen("organization")
    data object AppManagement : Screen("appManagement")
    data object Update : Screen("update")
    data object Settings : Screen("settings")

    // 教学计划相关
    data object StudyRequirement : Screen("studyRequirement")
    data object CourseInfo : Screen("courseInfo")
    data object AcademicStatus : Screen("academicStatus")
    data object AcademicMessages : Screen("academicMessages")
    data object CourseStatistics : Screen("courseStatistics")
    data object Announcement : Screen("announcement")
    
    data object ValueCalculator : Screen("value_calculator")

    // 签到相关
    data object Checkin : Screen("checkin")
    data object CheckinTasks : Screen("checkin/tasks/{accountId}") {
        fun createRoute(accountId: Long) = "checkin/tasks/$accountId"
        val arguments = listOf(
            navArgument("accountId") { type = NavType.LongType }
        )
    }

    data object DepartmentDetail : Screen("department/{department}") {
        fun createRoute(department: String) = "department/$department"
        val arguments = listOf(
            navArgument("department") { type = NavType.StringType }
        )
    }

    /** 公告编辑器；announcementId 为 -1 表示新建。 */
    data object DepartmentEdit : Screen("department/{department}/edit?announcementId={announcementId}") {
        fun createRoute(department: String, announcementId: Int? = null) =
            "department/$department/edit?announcementId=${announcementId ?: -1}"
        val arguments = listOf(
            navArgument("department") { type = NavType.StringType },
            navArgument("announcementId") {
                type = NavType.IntType
                defaultValue = -1
            }
        )
    }
}
