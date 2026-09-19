package com.suseoaa.projectoaa.shared.domain.model.gpa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.suseoaa.projectoaa.shared.domain.model.grade.GradeEntity
import com.suseoaa.projectoaa.shared.domain.engine.formatDecimal
import com.suseoaa.projectoaa.shared.domain.engine.scoreToGpaPoint

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

/**
 * 原先声明在 data/repository 包里，导致 UI 层必须 import 数据层才能拿到模型。
 * 它是纯领域模型，归位到 domain/model。
 */
/**
 * 绩点课程包装类
 */
data class GpaCourseWrapper(
    val originalEntity: GradeEntity,
    val isDegreeCourse: Boolean,
    val simulatedScore: Double?,
    val simulatedGpa: Double? = null,  // 模拟绩点（用户修改后的值）
    val isExcluded: Boolean = false,
    val isPassOnly: Boolean = false,   // 仅通过类成绩（合格/通过/免修），用于显示标记
    val originalScoreText: String = "", // 原始成绩文本（用于显示优/良/中/差）
    val isIncludedInCalculation: Boolean = true // 是否纳入当前绩点计算（供用户排除特定课程）
) {
    val credit: Double
        get() = originalEntity.credit.toDoubleOrNull() ?: 0.0

    val scoreValue: Double
        get() = simulatedScore ?: 0.0

    /**
     * 是否是等级制成绩（优/良/中/差等，不包括合格/通过/免修）
     */
    val isGradeLevel: Boolean
        get() = originalScoreText.isNotEmpty() &&
                originalScoreText.toDoubleOrNull() == null &&
                !isPassOnly &&
                simulatedGpa == null

    /**
     * 获取绩点值
     * 所有成绩都参与绩点计算
     * 优先级：模拟绩点 > 数据库绩点 > 计算绩点
     */
    val gpaValue: Double
        get() = simulatedGpa
            ?: originalEntity.gpa.toDoubleOrNull()
            ?: scoreToGpaPoint(scoreValue)

    /**
     * 显示成绩：如果是等级制成绩或仅通过类成绩且未被修改，显示原始等级文本
     */
    val displayScore: String
        get() = if (isGradeLevel || (isPassOnly && simulatedGpa == null)) originalScoreText else scoreValue.formatDecimal(1)

    val displayGpa: String
        get() = gpaValue.formatDecimal(2)

}
