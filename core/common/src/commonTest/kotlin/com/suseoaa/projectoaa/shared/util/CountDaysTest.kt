package com.suseoaa.projectoaa.shared.util

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseExamTimeRangeTest {

    @Test
    fun `解析括号格式`() {
        val range = parseExamTimeRange("2026-01-08(09:30-11:30)")
        assertEquals(LocalDateTime(2026, 1, 8, 9, 30) to LocalDateTime(2026, 1, 8, 11, 30), range)
    }

    @Test
    fun `解析空格格式`() {
        val range = parseExamTimeRange("2026-01-08 09:30-11:30")
        assertEquals(LocalDateTime(2026, 1, 8, 9, 30) to LocalDateTime(2026, 1, 8, 11, 30), range)
    }

    @Test
    fun `时间待定等无法解析的文案返回 null 而不是抛异常`() {
        assertNull(parseExamTimeRange("时间待定"))
        assertNull(parseExamTimeRange(""))
    }

    @Test
    fun `缺少时间段返回 null`() {
        assertNull(parseExamTimeRange("2026-01-08"))
        assertNull(parseExamTimeRange("2026-01-08(09:30)"))
    }

    @Test
    fun `日期非法返回 null`() {
        assertNull(parseExamTimeRange("2026-13-40(09:30-11:30)"))
    }
}

class TermForTest {

    @Test
    fun `8 月起算作新学年的第 1 学期`() {
        assertEquals("2025" to "3", termFor(2025, 8))
        assertEquals("2025" to "3", termFor(2025, 12))
    }

    @Test
    fun `1 月仍属上一学年的第 1 学期`() {
        // 寒假前的考试周还在上学期里，学年码要减 1；这是最容易算错的一档
        assertEquals("2025" to "3", termFor(2026, 1))
    }

    @Test
    fun `2 到 7 月为第 2 学期且学年码为上一年`() {
        assertEquals("2025" to "12", termFor(2026, 2))
        assertEquals("2025" to "12", termFor(2026, 7))
    }

    @Test
    fun `7 月与 8 月的交界正确切换学年`() {
        assertEquals("2025" to "12", termFor(2026, 7))
        assertEquals("2026" to "3", termFor(2026, 8))
    }
}
