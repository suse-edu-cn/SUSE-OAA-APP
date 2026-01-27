package com.suseoaa.projectoaa.shared.domain.model.academic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 成绩响应
 */
@Serializable
data class StudentGradeResponse(
    @SerialName("items")
    val items: List<GradeItem>? = null,
    @SerialName("totalCount")
    val totalCount: Int = 0
)

@Serializable
data class GradeItem(
    @SerialName("kcmc")
    val kcmc: String = "",        // 课程名称
    @SerialName("kch")
    val kch: String = "",         // 课程号
    @SerialName("xf")
    val xf: String = "",          // 学分
    @SerialName("cj")
    val cj: String = "",          // 成绩
    @SerialName("jd")
    val jd: String = "",          // 绩点
    @SerialName("ksxz")
    val ksxz: String = "",        // 考试性质
    @SerialName("xnmmc")
    val xnmmc: String = "",       // 学年名称
    @SerialName("xqmmc")
    val xqmmc: String = ""        // 学期名称
)

/**
 * 考试响应
 */
@Serializable
data class ExamResponse(
    @SerialName("items")
    val items: List<ExamItem>? = null
)

@Serializable
data class ExamItem(
    @SerialName("kcmc")
    val kcmc: String = "",        // 课程名称
    @SerialName("kssj")
    val kssj: String = "",        // 考试时间
    @SerialName("cdmc")
    val cdmc: String = "",        // 考试地点
    @SerialName("zwh")
    val zwh: String = "",         // 座位号
    @SerialName("ksmc")
    val ksmc: String = ""         // 考试名称
)
