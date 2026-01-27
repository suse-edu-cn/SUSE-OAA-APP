package com.suseoaa.projectoaa.data.model

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
    val totalHours: String = ""
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
