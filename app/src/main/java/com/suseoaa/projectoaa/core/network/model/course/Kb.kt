package com.suseoaa.projectoaa.core.network.model.course


import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Kb(
    @SerialName("kcmc") val courseName: String? = null,         // 课程名
    @SerialName("cdmc") val location: String? = null,           // 地点
    @SerialName("xm") val teacher: String? = null,              // 教师
    @SerialName("xqjmc") val dayOfWeek: String? = null,         // 星期几
    @SerialName("jc") val period: String? = null,               // 节次 (如 1-2)
    @SerialName("zcd") val weeks: String? = null,               // 周次 (如 1-16周)
    @SerialName("kch_id") val courseId: String? = null,
    @SerialName("kcxz") val nature: String? = null,
    @SerialName("kcbj") val background: String? = null,
    @SerialName("kclb") val category: String? = null,
    @SerialName("khfsmc") val assessment: String? = null,
    @SerialName("kcxszc") val totalHours: String? = null,
    @SerialName("zcmc") val teacherTitle: String? = null,
    @SerialName("zzmm") val politicalStatus: String? = null,
    @SerialName("jxbzc") val classGroup: String? = null
)
