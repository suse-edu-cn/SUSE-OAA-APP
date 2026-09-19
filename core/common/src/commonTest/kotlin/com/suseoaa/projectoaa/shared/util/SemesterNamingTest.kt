package com.suseoaa.projectoaa.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SemesterNamingTest {

    // 2024 级学生
    private val enrolled = 2024

    @Test
    fun `入学当年的两个学期是大一上与大一下`() {
        assertEquals("大一上学期", SemesterNaming.full(enrolled, "2024", "3"))
        assertEquals("大一下学期", SemesterNaming.full(enrolled, "2024", "12"))
    }

    @Test
    fun `简写去掉学期二字`() {
        assertEquals("大一上", SemesterNaming.short(enrolled, "2024", "3"))
        assertEquals("大一下", SemesterNaming.short(enrolled, "2024", "12"))
    }

    @Test
    fun `逐年递增到大四`() {
        assertEquals("大二上", SemesterNaming.short(enrolled, "2025", "3"))
        assertEquals("大三下", SemesterNaming.short(enrolled, "2026", "12"))
        assertEquals("大四上", SemesterNaming.short(enrolled, "2027", "3"))
    }

    @Test
    fun `五年制也覆盖`() {
        assertEquals("大五下", SemesterNaming.short(enrolled, "2028", "12"))
    }

    @Test
    fun `超出学制范围时回落到学年写法而不是硬编个大六`() {
        assertEquals("2029-2030学年 第1学期", SemesterNaming.short(enrolled, "2029", "3"))
    }

    @Test
    fun `早于入学年份的学期同样回落`() {
        assertEquals("2023-2024学年 第1学期", SemesterNaming.short(enrolled, "2023", "3"))
    }

    @Test
    fun `没有年级代码时回落到学年写法`() {
        assertEquals("2024-2025学年 第1学期", SemesterNaming.full(null as Int?, "2024", "3"))
        assertEquals("2024-2025学年 第2学期", SemesterNaming.short(null as String?, "2024", "12"))
    }

    @Test
    fun `短学期没有通行的上下叫法，回落到第三学期`() {
        assertEquals("2024-2025学年 第3学期", SemesterNaming.short(enrolled, "2024", "16"))
    }

    @Test
    fun `未知学期码原样带出，不猜`() {
        assertEquals("2024-2025学年 第99学期", SemesterNaming.short(enrolled, "2024", "99"))
    }

    @Test
    fun `学年码非法时不崩`() {
        assertEquals("第1学期", SemesterNaming.short(enrolled, "", "3"))
        assertEquals("第1学期", SemesterNaming.full(enrolled, "abcd", "3"))
    }

    @Test
    fun `年级代码带后缀时只取前四位`() {
        assertEquals("大一上", SemesterNaming.short("2024001", "2024", "3"))
    }

    @Test
    fun `gradeName 单独可用`() {
        assertEquals("大一", SemesterNaming.gradeName(2024, 2024))
        assertEquals("大四", SemesterNaming.gradeName(2024, 2027))
        assertNull(SemesterNaming.gradeName(null, 2024))
        assertNull(SemesterNaming.gradeName(2024, 2030))
    }
}
