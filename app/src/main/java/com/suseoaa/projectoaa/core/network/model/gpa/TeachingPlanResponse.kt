package com.suseoaa.projectoaa.core.network.model.gpa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeachingPlanResponse(
    val items: List<TeachingPlanItem>? = null
)

@Serializable
data class TeachingPlanItem(
    @SerialName("kch") val courseNumber: String? = null,      // 课程号，用于匹配成绩
    @SerialName("kcmc") val courseName: String? = null,       // 课程名
    @SerialName("xf") val credit: String? = null,             // 学分
    @SerialName("zyzgkcbj") val degreeCourseFlag: String? = null, // 关键：学位课程标记 ("是"/"否")
    @SerialName("kcxzmc") val courseNature: String? = null    // 课程性质
)