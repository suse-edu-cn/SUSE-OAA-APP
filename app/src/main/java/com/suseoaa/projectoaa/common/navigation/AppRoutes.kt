package com.suseoaa.projectoaa.common.navigation

/**
 * 全局路由表。
 * 新增页面时，请务必在此处添加对象，严禁在代码中硬编码路由字符串。
 */
sealed class AppRoutes(val route: String) {
    // 启动页/过渡页
    object Splash : AppRoutes("splash")

    // 认证模块
    object Login : AppRoutes("auth/login")
    object Register : AppRoutes("auth/register")
    object Profile : AppRoutes("auth/profile")

//    主目录模块
    object Home : AppRoutes("home")
    // 学生业务模块
    object StudentEntry : AppRoutes("student/entry")
    object StudentForm : AppRoutes("student/form")
}