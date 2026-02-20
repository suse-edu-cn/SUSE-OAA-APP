package com.suseoaa.projectoaa.shared.domain.model.course

// ==================== 本地数据模型 ====================

/**
 * 账号信息
 */
data class CourseAccountInfo(
    val studentId: String,
    val password: String = "",
    val name: String = "",
    val className: String = "",
    val njdmId: String = "",
    val major: String = "",
    val sortIndex: Int = 0,
    val jgId: String? = null,  // 学院ID
    val zyhId: String? = null  // 专业ID
)

/**
 * 课程信息
 */
data class CourseInfo(
    val studentId: String,
    val courseName: String,
    val xnm: String = "",       // 学年码
    val xqm: String = "",       // 学期码
    val isCustom: Boolean = false,
    val remoteCourseId: String = "",
    val nature: String = "",     // 课程性质
    val background: String = "", // 背景色
    val category: String = "",   // 课程类别
    val assessment: String = "", // 考核方式
    val totalHours: String = ""  // 总学时
)

/**
 * 课程时间信息
 */
data class ClassTimeInfo(
    val studentId: String = "",
    val courseOwnerName: String = "",
    val xnm: String = "",
    val xqm: String = "",
    val isCustom: Boolean = false,
    val weekday: String = "",      // 星期几 (1-7)
    val period: String = "",       // 节次
    val weeks: String = "",        // 周次文本
    val weeksMask: Long = 0L,      // 周次掩码
    val location: String = "",     // 上课地点
    val teacher: String = "",      // 教师
    val duration: String = "",     // 课程时长
    val teacherTitle: String = "", // 教师职称
    val classGroup: String = ""    // 班级组
)

/**
 * 学期选项
 */
data class TermOption(
    val xnm: String,  // 学年码
    val xqm: String,  // 学期码
    val label: String // 显示标签
)
