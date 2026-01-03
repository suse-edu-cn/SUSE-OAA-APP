package com.suseoaa.projectoaa.feature.academicPortal

/**
 * 统一管理教务板块的所有子页面
 * @param route 手机端导航需要的路由字符串
 * @param title 页面标题（可选，方便做TopBar标题）
 */

/**
 * 1、定义：在 AcademicDestinations 里加一行 data object Exams : ...
 *
 * 2、入口：在 AcademicMenuContent 里加一个按钮，点击传 Exams。
 *
 * 3、UI实现：
 *
 * 平板：在 AcademicScreen 的 when 里加一行 is Exams -> ExamsScreen()。
 *
 * 手机：在 AppNavHost 里加一个 composable(Destinations.Exams.route) { ... }。
 */
sealed class AcademicDestinations(val route: String, val title: String) {
    // 1. 菜单页（默认）
    data object Menu : AcademicDestinations("academic_menu", "教务系统")

    // 2. 成绩页
    data object Grades : AcademicDestinations("academic_grades", "成绩查询")

    // 3. 未来新增的考场页
    // data object Exams : AcademicDestinations("academic_exams", "考场查询")
}