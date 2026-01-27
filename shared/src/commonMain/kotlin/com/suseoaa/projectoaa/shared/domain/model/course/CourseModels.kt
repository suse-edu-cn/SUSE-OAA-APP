package com.suseoaa.projectoaa.shared.domain.model.course

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
 * 课程与时间组合
 */
data class CourseWithTimes(
    val course: CourseInfo,
    val times: List<ClassTimeInfo>
)

/**
 * 学期选项
 */
data class TermOption(
    val xnm: String,  // 学年码
    val xqm: String,  // 学期码
    val label: String // 显示标签
)

// ==================== 网络响应模型 ====================

@Serializable
data class CourseResponseJson(
    @SerialName("kbList")
    val kbList: List<Kb>? = null,
    @SerialName("xsxx")
    val xsxx: Xsxx? = null
)

@Serializable
data class Kb(
    @SerialName("kcmc")
    val kcmc: String = "",        // 课程名称
    @SerialName("xm")
    val xm: String = "",          // 教师姓名
    @SerialName("cdmc")
    val cdmc: String = "",        // 教室
    @SerialName("xqj")
    val xqj: String = "",         // 星期几 (1-7)
    @SerialName("jcs")
    val jcs: String = "",         // 节次 (如 "1-2")
    @SerialName("zcd")
    val zcd: String = "",         // 周次
    @SerialName("kch_id")
    val kchId: String = "",       // 课程号
    @SerialName("xnm")
    val xnm: String = "",         // 学年
    @SerialName("xqm")
    val xqm: String = "",         // 学期
    @SerialName("kcxzmc")
    val kcxzmc: String = "",      // 课程性质名称
    @SerialName("kclbmc")
    val kclbmc: String = "",      // 课程类别名称
    @SerialName("khfsmc")
    val khfsmc: String = "",      // 考核方式名称
    @SerialName("zxs")
    val zxs: String = ""          // 总学时
)

@Serializable
data class Xsxx(
    @SerialName("XM")
    val xm: String = "",          // 学生姓名
    @SerialName("XH")
    val xh: String = "",          // 学号
    @SerialName("BJMC")
    val bjmc: String = "",        // 班级名称
    @SerialName("ZYMC")
    val zymc: String = "",        // 专业名称
    @SerialName("NJDM_ID")
    val njdmId: String = "",      // 年级代码ID
    @SerialName("JG_ID")
    val jgId: String = "",        // 学院ID
    @SerialName("ZYH_ID")
    val zyhId: String = ""        // 专业ID
)

@Serializable
data class QueryModel(
    val xnm: String,              // 学年
    val xqm: String               // 学期
)

@Serializable
data class UserModel(
    val username: String,
    val password: String
)

@Serializable
data class RSAKey(
    val modulus: String,
    val exponent: String
)
