package com.suseoaa.projectoaa.shared.domain.course

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 用例里的周次 / 节次原文全部来自真实课表（2026-2027学年第1学期），不要随手改成"看起来更规范"的写法。
 */
class CourseScheduleParserTest {

    private fun activeWeeks(weeksStr: String, range: IntRange = 0..25): List<Int> =
        range.filter { CourseScheduleParser.isWeekActive(it, weeksStr) }

    // ==================== 周次：单周（历史 bug） ====================

    @Test
    fun `第N周 这种单周写法能被识别`() {
        // 教务系统对只上一周的课写作"第4周"，旧代码把"周"去掉后剩下"第4"，解析失败导致整门课不显示
        assertEquals(listOf(4), activeWeeks("第4周"))
        assertEquals(listOf(6), activeWeeks("第6周"))
        assertEquals(listOf(3), activeWeeks("第3周"))
    }

    @Test
    fun `不带第字的单周写法同样能识别`() {
        assertEquals(listOf(4), activeWeeks("4周"))
        assertEquals(listOf(4), activeWeeks("4"))
    }

    // ==================== 周次：区间与多段 ====================

    @Test
    fun `连续区间`() {
        assertEquals((10..17).toList(), activeWeeks("10-17周"))
        assertEquals(listOf(19, 20), activeWeeks("19-20周"))
    }

    @Test
    fun `逗号分隔的多段区间`() {
        assertEquals(listOf(1, 2, 3, 4, 5, 7, 8, 9), activeWeeks("1-5周,7-9周"))
        assertEquals(listOf(1, 2, 3, 4, 6, 7, 8, 9, 10, 11), activeWeeks("1-4周,6-11周"))
        assertEquals(listOf(1, 2, 3, 6, 7, 8, 9), activeWeeks("1-3周,6-9周"))
    }

    @Test
    fun `区间与单周混排`() {
        assertEquals(listOf(1, 2, 3, 6, 9, 10), activeWeeks("1-3周,第6周,9-10周"))
    }

    // ==================== 周次：单双周 ====================

    @Test
    fun `整串单双周标记`() {
        assertEquals(listOf(1, 3, 5, 7, 9, 11, 13, 15), activeWeeks("1-16周(单)"))
        assertEquals(listOf(2, 4, 6, 8, 10, 12, 14, 16), activeWeeks("2-16周（双）"))
    }

    @Test
    fun `分段自带单双周时不影响其它分段`() {
        // 回归用例：`4-6周(双)` 只上双周，但 `7-8周` 是每周都上，不能被前一段的"双"带偏
        assertEquals(listOf(4, 6, 7, 8), activeWeeks("4-6周(双),7-8周"))
    }

    @Test
    fun `只写单双周而没有具体周次时按整学期处理`() {
        // 旧代码会把"单"字直接删掉，剩下空串，导致这门课一周都不显示
        val weeks = activeWeeks("单周", range = 1..16)
        assertEquals(listOf(1, 3, 5, 7, 9, 11, 13, 15), weeks)
    }

    // ==================== 周次：边界与脏数据 ====================

    @Test
    fun `周次为空时按每周都上处理`() {
        assertTrue(CourseScheduleParser.isWeekActive(1, ""))
        assertTrue(CourseScheduleParser.isWeekActive(20, ""))
    }

    @Test
    fun `无法识别的周次不会让整门课消失`() {
        assertTrue(CourseScheduleParser.isWeekActive(5, "待定"))
    }

    @Test
    fun `第0周不会因为移位越界而误判`() {
        // 掩码用"第 N 周对应第 N 位"，第 0 周不会像 `1 shl (week-1)` 那样移位成第 63 位
        assertTrue(CourseScheduleParser.isWeekActive(0, "0-5周"))
        assertFalse(CourseScheduleParser.isWeekActive(0, "1-5周"))
    }

    @Test
    fun `超出掩码范围的周次返回false而不是崩溃`() {
        assertFalse(CourseScheduleParser.isWeekActive(99, "1-16周"))
        assertFalse(CourseScheduleParser.isWeekActive(-1, "1-16周"))
    }

    @Test
    fun `起止写反的区间按区间处理`() {
        assertEquals(listOf(3, 4, 5), activeWeeks("5-3周"))
    }

    @Test
    fun `周次原文为空时回退到掩码`() {
        val mask = CourseScheduleParser.weeksToMask("第4周")
        assertTrue(CourseScheduleParser.isWeekActive(4, "", mask))
        assertFalse(CourseScheduleParser.isWeekActive(5, "", mask))
    }

    // ==================== 节次 ====================

    @Test
    fun `节次区间`() {
        assertEquals(PeriodSpan(1, 2), CourseScheduleParser.parsePeriod("1-2"))
        assertEquals(PeriodSpan(3, 2), CourseScheduleParser.parsePeriod("3-4节"))
        assertEquals(PeriodSpan(5, 4), CourseScheduleParser.parsePeriod("5-8节"))
        assertEquals(PeriodSpan(9, 3), CourseScheduleParser.parsePeriod("9-11节"))
    }

    @Test
    fun `单节次`() {
        assertEquals(PeriodSpan(5, 1), CourseScheduleParser.parsePeriod("5"))
        assertEquals(PeriodSpan(5, 1), CourseScheduleParser.parsePeriod("5节"))
    }

    @Test
    fun `节次无法识别时按第1节处理`() {
        assertEquals(PeriodSpan(1, 1), CourseScheduleParser.parsePeriod(""))
    }

    // ==================== 星期 ====================

    @Test
    fun `数字星期`() {
        assertEquals(1, CourseScheduleParser.parseWeekday("1"))
        assertEquals(7, CourseScheduleParser.parseWeekday("7"))
    }

    @Test
    fun `中文星期`() {
        assertEquals(1, CourseScheduleParser.parseWeekday("星期一"))
        assertEquals(3, CourseScheduleParser.parseWeekday("周三"))
        assertEquals(6, CourseScheduleParser.parseWeekday("星期六"))
        assertEquals(7, CourseScheduleParser.parseWeekday("星期日"))
        assertEquals(7, CourseScheduleParser.parseWeekday("周天"))
    }

    @Test
    fun `星期无法识别时按周一处理`() {
        assertEquals(1, CourseScheduleParser.parseWeekday(""))
        assertEquals(1, CourseScheduleParser.parseWeekday("待定"))
    }

    // ==================== 全角数字 ====================

    @Test
    fun `全角数字能被识别`() {
        assertEquals(listOf(4), activeWeeks("第４周"))
        assertEquals(PeriodSpan(1, 2), CourseScheduleParser.parsePeriod("１-２节"))
    }
}
