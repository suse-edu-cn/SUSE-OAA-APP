package com.suseoaa.projectoaa.ui.screen.exam

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 编辑自建考试时要把教务给的时间串拆回「年月日 + 起止时分」填进选择器。
 * 教务有两种写法（括号与空格），还常出现"时间待定"这类拆不动的值——
 * 任何一处解析出错都会让编辑框预填成错误时间。
 */
class ExamTimeParsingTest {

    private val bracket = "2025-01-15(09:00-11:00)"
    private val spaced = "2025-01-15 09:00-11:00"

    @Test
    fun `括号格式能解析出日期`() {
        assertEquals(LocalDate(2025, 1, 15), parseExamDate(bracket))
    }

    @Test
    fun `空格格式能解析出日期`() {
        assertEquals(LocalDate(2025, 1, 15), parseExamDate(spaced))
    }

    @Test
    fun `两种格式解析出的起止时分一致`() {
        listOf(bracket, spaced).forEach { s ->
            assertEquals(9, parseExamStartHour(s), s)
            assertEquals(0, parseExamStartMinute(s), s)
            assertEquals(11, parseExamEndHour(s), s)
            assertEquals(0, parseExamEndMinute(s), s)
        }
    }

    @Test
    fun `非整点时间正确解析分钟`() {
        val s = "2025-06-30(14:35-16:05)"
        assertEquals(14, parseExamStartHour(s))
        assertEquals(35, parseExamStartMinute(s))
        assertEquals(16, parseExamEndHour(s))
        assertEquals(5, parseExamEndMinute(s))
    }

    @Test
    fun `时间待定等无法解析的输入一律返回 null`() {
        listOf("时间待定", "", "待定").forEach { s ->
            assertNull(parseExamDate(s), s)
            assertNull(parseExamStartHour(s), s)
            assertNull(parseExamEndMinute(s), s)
        }
    }

    @Test
    fun `只有日期没有时间时时分为 null 但日期仍可解析`() {
        val s = "2025-01-15"
        assertEquals(LocalDate(2025, 1, 15), parseExamDate(s))
        assertNull(parseExamStartHour(s))
    }

    @Test
    fun `日期段不完整时返回 null`() {
        assertNull(parseExamDate("2025-01(09:00-11:00)"))
    }

    @Test
    fun `提取时间段时括号优先于空格`() {
        assertEquals("09:00-11:00", extractTimePart(bracket))
        assertEquals("09:00-11:00", extractTimePart(spaced))
        assertNull(extractTimePart("2025-01-15"))
    }
}
