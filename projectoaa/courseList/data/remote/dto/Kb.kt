package com.suseoaa.projectoaa.courseList.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Kb(
    @Json(name = "kcmc") val courseName: String?,         // 课程名
    @Json(name = "cdmc") val location: String?,           // 地点
    @Json(name = "xm") val teacher: String?,              // 教师
    @Json(name = "xqjmc") val dayOfWeek: String?,         // 星期几
    @Json(name = "jc") val period: String?,               // 节次 (如 1-2)
    @Json(name = "zcd") val weeks: String?,               // 周次 (如 1-16周)
    @Json(name = "kch_id") val courseId: String?,
    @Json(name = "kcxz") val nature: String?,
    @Json(name = "kcbj") val background: String?,
    @Json(name = "kclb") val category: String?,
    @Json(name = "khfsmc") val assessment: String?,
    @Json(name = "kcxszc") val totalHours: String?,
    @Json(name = "zcmc") val teacherTitle: String?,
    @Json(name = "zzmm") val politicalStatus: String?,
    @Json(name = "jxbzc") val classGroup: String?
)