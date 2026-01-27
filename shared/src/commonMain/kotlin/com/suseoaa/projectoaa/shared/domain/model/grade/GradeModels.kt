package com.suseoaa.projectoaa.shared.domain.model.grade

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StudentGradeResponse(
    @SerialName("items")
    val items: List<GradeItem>? = emptyList(),
    @SerialName("totalResult")
    val totalResult: Int? = 0,
    @SerialName("currentPage")
    val currentPage: Int? = 1
)

@Serializable
data class GradeItem(
    @SerialName("bfzcj")
    val bfzcj: String? = "",       // 百分制成绩
    @SerialName("bh")
    val bh: String? = "",          // 班号
    @SerialName("bj")
    val bj: String? = "",          // 班级
    @SerialName("cj")
    val cj: String? = "",          // 成绩
    @SerialName("jd")
    val jd: String? = "",          // 绩点
    @SerialName("jgmc")
    val jgmc: String? = "",        // 学院名称
    @SerialName("jsxm")
    val jsxm: String? = "",        // 教师姓名
    @SerialName("jxbmc")
    val jxbmc: String? = "",       // 教学班名称
    @SerialName("kcbj")
    val kcbj: String? = "",        // 课程标记
    @SerialName("kch")
    val kch: String? = "",         // 课程号
    @SerialName("kclbmc")
    val kclbmc: String? = "",      // 课程类别名称
    @SerialName("kcmc")
    val kcmc: String? = "",        // 课程名称
    @SerialName("kcxzmc")
    val kcxzmc: String? = "",      // 课程性质名称 (专业基础必修等)
    @SerialName("khfsmc")
    val khfsmc: String? = "",      // 考核方式名称
    @SerialName("kkbmmc")
    val kkbmmc: String? = "",      // 开课部门名称
    @SerialName("ksxz")
    val ksxz: String? = "",        // 考试性质 (正常考试/补考)
    @SerialName("njmc")
    val njmc: String? = "",        // 年级名称
    @SerialName("sfxwkc")
    val sfxwkc: String? = "",      // 是否学位课程
    @SerialName("xf")
    val xf: String? = "",          // 学分
    @SerialName("xfjd")
    val xfjd: String? = "",        // 学分绩点
    @SerialName("xh")
    val xh: String? = "",          // 学号
    @SerialName("xm")
    val xm: String? = "",          // 姓名
    @SerialName("xnm")
    val xnm: String? = "",         // 学年码
    @SerialName("xnmmc")
    val xnmmc: String? = "",       // 学年名称
    @SerialName("xqm")
    val xqm: String? = "",         // 学期码
    @SerialName("xqmmc")
    val xqmmc: String? = "",       // 学期名称
    @SerialName("zymc")
    val zymc: String? = ""         // 专业名称
)

// GPA 计算相关模型
data class GpaStats(
    val totalGpa: String = "0.00",
    val totalCredits: String = "0.0",
    val degreeGpa: String = "0.00",
    val degreeCredits: String = "0.0"
)

data class GpaCourseWrapper(
    val originalEntity: GradeItem,
    val simulatedScore: Double? = null,
    val isDegreeCourse: Boolean = false
) {
    val displayScore: String
        get() = simulatedScore?.toString() ?: originalEntity.cj ?: "0"
    
    val displayGpa: String
        get() {
            val score = simulatedScore ?: (originalEntity.cj?.toDoubleOrNull() ?: 0.0)
            return calculateGpa(score)
        }
    
    val credit: Double
        get() = originalEntity.xf?.toDoubleOrNull() ?: 0.0
    
    private fun calculateGpa(score: Double): String {
        val gpa = when {
            score >= 90 -> 4.0
            score >= 85 -> 3.7
            score >= 82 -> 3.3
            score >= 78 -> 3.0
            score >= 75 -> 2.7
            score >= 72 -> 2.3
            score >= 68 -> 2.0
            score >= 64 -> 1.5
            score >= 60 -> 1.0
            else -> 0.0
        }
        return gpa.formatDecimal(2)
    }
    
    private fun Double.formatDecimal(decimals: Int): String {
        var factor = 1.0
        repeat(decimals) { factor *= 10.0 }
        val rounded = kotlin.math.round(this * factor) / factor
        val parts = rounded.toString().split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else ""
        return if (decimals > 0) {
            "$intPart.${decPart.padEnd(decimals, '0').take(decimals)}"
        } else {
            intPart
        }
    }
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

enum class FilterType {
    ALL,
    DEGREE_ONLY
}
