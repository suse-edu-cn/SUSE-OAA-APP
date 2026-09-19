package com.suseoaa.projectoaa.domain.course

import androidx.compose.runtime.Immutable

/**
 * 多账号课表的冲突判定。
 *
 * 应用允许同时挂多个学号的课表（比如帮同学代查），同一时间格里就可能出现
 * 分属不同账号的课。这里判断"当前这门课"和"其它账号的课"在节次上是否相撞，
 * 以及撞的是完全重合还是部分重合。
 *
 * 原本埋在 CourseViewModel 里当私有方法，既是业务规则又无从测试；提到这里后
 * 是纯函数，可以直接对边界情形写用例。
 */

/** 一门课在某一天占据的连续节次区间，[startSection] 与 [endSection] 均为闭区间。 */
data class SectionSpan(
    val studentId: String,
    /** 星期几，0 = 周一 */
    val dayIndex: Int,
    val startSection: Int,
    val endSection: Int,
    val accountName: String,
    val courseName: String,
)

enum class CourseOverlapStatus {
    NO_OVERLAP,
    OVERLAP,
    PARTIAL_OVERLAP,
}

@Immutable
data class CourseOverlapDetail(
    val status: CourseOverlapStatus,
    val overlappedAccounts: List<String> = emptyList(),
    val overlappedCourses: List<String> = emptyList(),
)

object CourseOverlapCalculator {

    /**
     * 判断 [current] 与 [others] 中其它账号的课是否冲突。
     *
     * 同一个账号自己的课不算冲突（同账号的重叠是排课数据问题，另有展示逻辑）；
     * 节次区间只要相交即算冲突，起止完全一致则记为完全重合。
     */
    fun detail(current: SectionSpan, others: List<SectionSpan>): CourseOverlapDetail {
        val overlaps = others.filter { other ->
            other.studentId != current.studentId &&
                other.dayIndex == current.dayIndex &&
                other.startSection <= current.endSection &&
                other.endSection >= current.startSection
        }

        if (overlaps.isEmpty()) {
            return CourseOverlapDetail(status = CourseOverlapStatus.NO_OVERLAP)
        }

        val hasExactOverlap = overlaps.any { other ->
            other.startSection == current.startSection && other.endSection == current.endSection
        }

        return CourseOverlapDetail(
            status = if (hasExactOverlap) CourseOverlapStatus.OVERLAP
            else CourseOverlapStatus.PARTIAL_OVERLAP,
            overlappedAccounts = overlaps
                .mapNotNull { it.accountName.ifBlank { null } }
                .distinct()
                .sorted(),
            overlappedCourses = overlaps
                .map { other ->
                    if (other.accountName.isBlank()) other.courseName
                    else "${other.courseName}（${other.accountName}）"
                }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted(),
        )
    }
}
