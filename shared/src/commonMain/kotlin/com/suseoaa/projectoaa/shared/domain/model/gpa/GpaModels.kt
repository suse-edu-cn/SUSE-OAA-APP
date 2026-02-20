package com.suseoaa.projectoaa.shared.domain.model.gpa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 专业列表项
 */
@Serializable
data class MajorItem(
    @SerialName("id") val majorId: String = "",      // 专业ID (zyh_id)
    @SerialName("zymc") val majorName: String = ""     // 专业名称
)

/**
 * 培养计划信息响应
 */
@Serializable
data class ProfessionInfoResponse(
    @SerialName("items") val items: List<PlanInfo>? = null
)

@Serializable
data class PlanInfo(
    @SerialName("jxzxjhxx_id") val planId: String = ""        // 培养计划ID
)

/**
 * 培养计划课程列表响应
 */
@Serializable
data class TeachingPlanResponse(
    @SerialName("items") val items: List<TeachingPlanItem>? = null
)

@Serializable
data class TeachingPlanItem(
    @SerialName("kch") val courseNumber: String? = null,      // 课程号
    @SerialName("kcmc") val courseName: String? = null,        // 课程名
    @SerialName("xf") val credit: String? = null,            // 学分
    @SerialName("zyzgkcbj") val degreeCourseFlag: String? = null,  // 学位课程标记 ("是"/"否")
    @SerialName("kcxzmc") val courseNature: String? = null       // 课程性质
)
