package com.suseoaa.projectoaa.domain.gpa

import com.suseoaa.projectoaa.shared.domain.model.gpa.GpaCourseWrapper
import com.suseoaa.projectoaa.util.format

/**
 * 绩点页的筛选、排序与统计。
 *
 * 这页支持"模拟改分"——用户改一门课的分数，实时看总绩点怎么变，所以统计口径
 * 必须精确：按学分加权、可逐门排除、学位课单独算一份。逻辑原本埋在
 * GpaViewModel 的私有方法里无从验证，提到这里后是纯函数。
 */

enum class SortOrder { ASCENDING, DESCENDING }

enum class FilterType { ALL, DEGREE_ONLY }

/** 统计结果直接以展示用的字符串给出，与页面上的小数位一致。 */
data class GpaStats(
    val totalGpa: String,
    val totalCredits: String,
    val degreeGpa: String,
    val degreeCredits: String,
)

object GpaCalculator {

    const val ALL_TERMS = "ALL"

    /** 学期筛选。[term] 形如 "2023_3"，[ALL_TERMS] 表示不筛。 */
    fun filterByTerm(courses: List<GpaCourseWrapper>, term: String): List<GpaCourseWrapper> =
        if (term == ALL_TERMS) courses
        else courses.filter { "${it.originalEntity.xnm}_${it.originalEntity.xqm}" == term }

    fun filterByType(courses: List<GpaCourseWrapper>, type: FilterType): List<GpaCourseWrapper> =
        if (type == FilterType.DEGREE_ONLY) courses.filter { it.isDegreeCourse } else courses

    fun sort(courses: List<GpaCourseWrapper>, order: SortOrder): List<GpaCourseWrapper> =
        when (order) {
            SortOrder.DESCENDING -> courses.sortedByDescending { it.scoreValue }
            SortOrder.ASCENDING -> courses.sortedBy { it.scoreValue }
        }

    /**
     * 加权平均绩点。
     *
     * 注意统计口径：先按学期筛，再算统计，最后才按课程类型筛——所以切换到
     * "只看学位课"时，顶部的总绩点仍是该学期全部课程的绩点，只有列表变短。
     * 被用户手动排除（[GpaCourseWrapper.isIncludedInCalculation] 为 false）
     * 和学分为 0 的课都不计入。
     */
    fun stats(courses: List<GpaCourseWrapper>): GpaStats {
        var totalPoints = 0.0
        var totalCredits = 0.0
        var degreePoints = 0.0
        var degreeCredits = 0.0

        courses.forEach { item ->
            if (!item.isIncludedInCalculation) return@forEach
            val credit = item.credit
            if (credit <= 0.0) return@forEach

            totalPoints += item.gpaValue * credit
            totalCredits += credit
            if (item.isDegreeCourse) {
                degreePoints += item.gpaValue * credit
                degreeCredits += credit
            }
        }

        return GpaStats(
            totalGpa = (if (totalCredits > 0) totalPoints / totalCredits else 0.0).format(2),
            totalCredits = totalCredits.format(1),
            degreeGpa = (if (degreeCredits > 0) degreePoints / degreeCredits else 0.0).format(2),
            degreeCredits = degreeCredits.format(1),
        )
    }
}
