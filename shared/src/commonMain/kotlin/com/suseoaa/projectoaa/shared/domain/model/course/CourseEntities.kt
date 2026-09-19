package com.suseoaa.projectoaa.shared.domain.model.course

data class CourseAccountEntity(
    val studentId: String,
    val password: String,
    val name: String,
    val className: String,
    val njdmId: String,
    val major: String,
    val sortIndex: Int = 0,
    val jgId: String? = null,
    val zyhId: String? = null
)

data class CourseEntity(
    val studentId: String,
    val courseName: String,
    val xnm: String,
    val xqm: String,
    val isCustom: Boolean = false,
    val remoteCourseId: String = "",
    val nature: String = "",
    val background: String = "",
    val category: String = "",
    val assessment: String = "",
    /** 学分。教务系统的 `xf` 字段；数据库列因历史原因仍叫 totalHours。 */
    val credit: String = ""
)

data class ClassTimeEntity(
    val uniqueId: Long = 0,
    val studentId: String,
    val courseOwnerName: String,
    val xnm: String,
    val xqm: String,
    val isCustom: Boolean,
    val weekday: String = "",
    val period: String = "",
    val weeks: String = "",
    val weeksMask: Long = 0L,
    val location: String = "",
    val teacher: String = "",
    val duration: String = "",
    val teacherTitle: String = "",
    val politicalStatus: String = "",
    val classGroup: String = ""
)

data class CourseWithTimes(
    val course: CourseEntity,
    val times: List<ClassTimeEntity>
)

/**
 * 整周实践课（实习、集中实践环节）。没有星期和节次，只有周次范围，因此不进课表格子。
 */
data class PracticeCourseEntity(
    val studentId: String,
    val xnm: String,
    val xqm: String,
    val courseName: String,
    val teacher: String = "",
    val classGroup: String = "",
    val weeks: String = "",
    val weeksMask: Long = 0L,
    val credit: String = "",
    val assessment: String = "",
    val campus: String = ""
)

/**
 * 原先声明在 data/repository 包里，是纯领域模型，归位到 domain/model。
 */
/**
 * 校历解析结果
 * @param startDate 学期最早的周一日期（如果有第0周则是第0周的周一，否则是第1周的周一）
 * @param hasWeekZero 是否存在第0周
 */
data class SemesterCalendarInfo(
    val startDate: String,
    val hasWeekZero: Boolean
)
