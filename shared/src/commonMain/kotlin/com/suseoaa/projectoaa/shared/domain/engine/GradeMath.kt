package com.suseoaa.projectoaa.shared.domain.engine

import com.suseoaa.projectoaa.shared.data.repository.GradeEntity

/**
 * 成绩数值语义与聚合计算。
 *
 * 教务系统的成绩字段既可能是数字，也可能是“优秀/良好/及格”这类等级词，
 * 学分与绩点还可能为空串，因此所有取值都要经过这里的解析函数，
 * 避免各处各写一套 `toDoubleOrNull()` 兜底逻辑。
 */

/** 及格线：数字按 60 分判定，等级词按通过语义判定 */
internal fun isPassingScore(score: String): Boolean {
    val numericScore = score.toDoubleOrNull()
    if (numericScore != null) return numericScore >= 60.0
    return score in listOf("优秀", "良好", "中等", "及格", "通过", "合格")
}

/** 把成绩字段折算成可比较的分数，等级词取该档的代表分 */
internal fun parseScore(score: String): Double {
    return score.toDoubleOrNull() ?: when (score) {
        "优秀" -> 95.0
        "良好" -> 85.0
        "中等" -> 75.0
        "及格", "通过", "合格" -> 65.0
        "不及格", "未通过", "不合格", "缓考", "缺考" -> 0.0
        else -> 0.0
    }
}

internal fun parseCredit(credit: String): Double = credit.toDoubleOrNull() ?: 0.0

internal fun parseGpa(gpa: String): Double = gpa.toDoubleOrNull() ?: 0.0

/** @return Pair<加权绩点, 总学分> */
internal fun calculateWeightedGpa(courseGrades: List<GradeEntity>): Pair<Double, Double> {
    val totalCredits = courseGrades.sumOf { parseCredit(it.credit) }
    val totalPoints = courseGrades.sumOf { parseGpa(it.gpa) * parseCredit(it.credit) }
    return (if (totalCredits > 0) totalPoints / totalCredits else 0.0) to totalCredits
}

/**
 * 同一门课可能有重修记录，统计时按课程名取最高分的那一次。
 * @return 课程名 -> 该课程的最高分记录
 */
internal fun List<GradeEntity>.bestAttemptsByCourse(): Map<String, GradeEntity> =
    filter { it.courseName.isNotBlank() }
        .groupBy { it.courseName }
        .mapValues { (_, attempts) -> attempts.maxBy { parseScore(it.score) } }

/** 按最高分口径仍未通过的课程，即真正需要重修的课 */
internal fun List<GradeEntity>.failedBestAttempts(): List<GradeEntity> =
    bestAttemptsByCourse().values
        .filter { !isPassingScore(it.score) }
        .sortedBy { it.courseName }

/** 教师字段可能是多人合并的字符串，按常见分隔符拆开并去掉“老师”后缀 */
internal fun splitTeachers(rawTeacher: String): List<String> =
    rawTeacher
        .split("、", ",", "，", ";", "；", "/", "／")
        .map { it.trim().removeSuffix("老师").trim() }
        .filter { it.isNotBlank() && it != "-" && it != "无" }

/** 整数学分不显示小数位 */
internal fun formatCredit(credit: String): String {
    val numericCredit = parseCredit(credit)
    return if (numericCredit % 1.0 == 0.0) {
        numericCredit.toInt().toString()
    } else {
        numericCredit.formatDecimal(1)
    }
}

/** 由目标绩点粗略反推需要的卷面分区间 */
internal fun estimateScoreFromGpa(gpa: Double): String = when {
    gpa <= 0.0 -> "60 分左右即可"
    gpa <= 1.0 -> "60-64 分"
    gpa <= 1.5 -> "65-69 分"
    gpa <= 2.0 -> "70-74 分"
    gpa <= 2.5 -> "75-79 分"
    gpa <= 3.0 -> "80-84 分"
    gpa <= 3.5 -> "85-89 分"
    gpa <= 4.0 -> "90 分以上"
    else -> "超过常规满绩点范围"
}

/** 对达成目标绩点的难度给一个直观描述 */
internal fun difficultyLabel(gpa: Double): String = when {
    gpa <= 1.0 -> "较低"
    gpa <= 2.0 -> "适中"
    gpa <= 3.0 -> "偏高"
    gpa <= 4.0 -> "较高"
    else -> "很高，单靠这些重修课可能不够"
}

/** 保留固定小数位；KMP 下没有 String.format，这里手动补位 */
internal fun Double.formatDecimal(decimals: Int): String {
    var factor = 1.0
    repeat(decimals) { factor *= 10.0 }
    val rounded = kotlin.math.round(this * factor) / factor
    val parts = rounded.toString().split(".")
    val intPart = parts[0]
    val decPart = if (parts.size > 1) parts[1] else ""
    return if (decimals > 0) "$intPart.${decPart.padEnd(decimals, '0').take(decimals)}" else intPart
}

/** 整数分数不显示小数位 */
internal fun Double.formatScore(): String =
    if (this % 1.0 == 0.0) this.toInt().toString() else this.formatDecimal(1)
