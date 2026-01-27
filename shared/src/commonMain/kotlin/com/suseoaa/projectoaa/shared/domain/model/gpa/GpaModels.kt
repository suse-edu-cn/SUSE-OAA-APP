package com.suseoaa.projectoaa.shared.domain.model.gpa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MajorItem(
    @SerialName("id")
    val id: String = "",
    @SerialName("zymc")
    val zymc: String = "",        // 专业名称
    @SerialName("zyh")
    val zyh: String = "",         // 专业号
    @SerialName("jgId")
    val jgId: String = ""         // 学院ID
)

@Serializable
data class ProfessionInfoResponse(
    @SerialName("code")
    val code: Int = 0,
    @SerialName("data")
    val data: List<MajorItem>? = null
)

@Serializable
data class TeachingPlanResponse(
    @SerialName("items")
    val items: List<TeachingPlanItem>? = null
)

@Serializable
data class TeachingPlanItem(
    @SerialName("kcmc")
    val kcmc: String = "",        // 课程名称
    @SerialName("kch")
    val kch: String = "",         // 课程号
    @SerialName("xf")
    val xf: String = "",          // 学分
    @SerialName("kcxz")
    val kcxz: String = "",        // 课程性质
    @SerialName("kclb")
    val kclb: String = ""         // 课程类别
)
