package com.suseoaa.projectoaa.shared.domain.course

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SemesterCalendarTest {

    // 2026-2027学年第1学期的开学第一周周一
    private val start = LocalDate.parse("2026-09-07")

    private fun week(date: String, hasWeekZero: Boolean = false) =
        SemesterCalendar.currentWeek(start, LocalDate.parse(date), hasWeekZero)

    @Test
    fun `开学当天是第一周`() {
        assertEquals(1, week("2026-09-07"))
    }

    @Test
    fun `第一周的周日仍然是第一周`() {
        assertEquals(1, week("2026-09-13"))
    }

    @Test
    fun `跨过周一进入下一周`() {
        assertEquals(2, week("2026-09-14"))
        assertEquals(4, week("2026-09-28"))
    }

    @Test
    fun `存在第0周时整体前移一周`() {
        assertEquals(0, week("2026-09-07", hasWeekZero = true))
        assertEquals(1, week("2026-09-14", hasWeekZero = true))
    }

    @Test
    fun `开学前不会算出第0周或负数周`() {
        // 整数除法会把 -1/7 截断成 0，开学前一天被误算成第一周；开学前 8 天更会算出第 0 周
        assertEquals(1, week("2026-09-06"))
        assertEquals(1, week("2026-08-30"))
        assertEquals(1, week("2026-06-01"))
    }

    @Test
    fun `放假之后钳制在最大周次`() {
        assertEquals(SemesterCalendar.MAX_WEEK, week("2027-06-01"))
    }

    @Test
    fun `第一周的每一天都是第一周`() {
        val days = (7..13).map { week("2026-09-${it.toString().padStart(2, '0')}") }
        assertEquals(List(7) { 1 }, days)
    }
}
