package com.suseoaa.projectoaa.shared.domain.course

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * 学期周次的换算：由开学日期推出"今天是第几周"。
 *
 * 课表页、上课提醒、桌面小组件都要算这个，以前三处各写了一遍且并不一致——提醒服务少了范围钳制，
 * 放假期间会算出第 0 周甚至负数周，导致那天整天不提醒。现在统一到这里。
 */
object SemesterCalendar {

    /** 课表最多显示到第几周。 */
    const val MAX_WEEK: Int = 25

    /**
     * 学期的第一周是第几周：教务系统有的学期存在"第 0 周"（军训/报到周）。
     */
    fun minWeek(hasWeekZero: Boolean): Int = if (hasWeekZero) 0 else 1

    /**
     * 算出 [today] 属于第几周，结果一定落在 [minWeek]..[MAX_WEEK] 内。
     *
     * @param semesterStart 开学第一周的周一
     */
    fun currentWeek(
        semesterStart: LocalDate,
        today: LocalDate,
        hasWeekZero: Boolean
    ): Int {
        val minWeek = minWeek(hasWeekZero)
        val days = semesterStart.daysUntil(today)
        // 用向下取整而不是整数除法：开学前 1 天的 -1/7 会截断成 0，被误算成第一周
        val weeksElapsed = days.floorDiv(7)
        return (weeksElapsed + minWeek).coerceIn(minWeek, MAX_WEEK)
    }
}
