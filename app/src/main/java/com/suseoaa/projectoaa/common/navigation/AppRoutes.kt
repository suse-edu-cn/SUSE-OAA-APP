package com.suseoaa.projectoaa.common.navigation

import androidx.compose.material.icons.Icons
// [修复] 添加所有必需的图标导入
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppRoutes(val route: String, val title: String, val icon: ImageVector) {
    object Splash : AppRoutes("splash", "Splash", Icons.Default.Home)
    object Login : AppRoutes("login", "Login", Icons.Default.Home)
    object Register : AppRoutes("register", "Register", Icons.Default.Home)
    object StudentEntry : AppRoutes("student_entry", "Student", Icons.Default.Home)
    //旧的入口

    object Home : AppRoutes("home", "首页", Icons.Default.Home)
    object Search : AppRoutes("search", "搜索", Icons.Default.Search)
    object Settings : AppRoutes("settings", "设置", Icons.Default.Settings)
    object Profile : AppRoutes("profile", "个人", Icons.Default.Person)

    object CourseList : AppRoutes("course_list", "课表查询", Icons.Default.DateRange)
    object StudentForm : AppRoutes("student_form", "招新/换届申请", Icons.Default.PersonAdd)
}