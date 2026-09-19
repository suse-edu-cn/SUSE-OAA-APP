package com.suseoaa.projectoaa.domain.course

import kotlin.test.Test
import kotlin.test.assertEquals

private fun span(
    studentId: String = "A",
    dayIndex: Int = 0,
    start: Int = 1,
    end: Int = 2,
    accountName: String = "小明",
    courseName: String = "高等数学",
) = SectionSpan(studentId, dayIndex, start, end, accountName, courseName)

class CourseOverlapCalculatorTest {

    @Test
    fun `没有其它账号的课时不冲突`() {
        val detail = CourseOverlapCalculator.detail(span(), emptyList())
        assertEquals(CourseOverlapStatus.NO_OVERLAP, detail.status)
        assertEquals(emptyList(), detail.overlappedAccounts)
    }

    @Test
    fun `同一账号自己的课不算冲突`() {
        val detail = CourseOverlapCalculator.detail(
            span(studentId = "A", start = 1, end = 2),
            listOf(span(studentId = "A", start = 1, end = 2, courseName = "大学英语")),
        )
        assertEquals(CourseOverlapStatus.NO_OVERLAP, detail.status)
    }

    @Test
    fun `不同天的课不算冲突`() {
        val detail = CourseOverlapCalculator.detail(
            span(studentId = "A", dayIndex = 0),
            listOf(span(studentId = "B", dayIndex = 1)),
        )
        assertEquals(CourseOverlapStatus.NO_OVERLAP, detail.status)
    }

    @Test
    fun `节次完全一致记为完全重合`() {
        val detail = CourseOverlapCalculator.detail(
            span(studentId = "A", start = 3, end = 4),
            listOf(span(studentId = "B", start = 3, end = 4, accountName = "小红", courseName = "线性代数")),
        )
        assertEquals(CourseOverlapStatus.OVERLAP, detail.status)
        assertEquals(listOf("小红"), detail.overlappedAccounts)
        assertEquals(listOf("线性代数（小红）"), detail.overlappedCourses)
    }

    @Test
    fun `节次部分相交记为部分重合`() {
        val detail = CourseOverlapCalculator.detail(
            span(studentId = "A", start = 3, end = 6),
            listOf(span(studentId = "B", start = 5, end = 8, accountName = "小红", courseName = "大学物理")),
        )
        assertEquals(CourseOverlapStatus.PARTIAL_OVERLAP, detail.status)
    }

    @Test
    fun `仅在端点相接也算相交`() {
        // 3-4 节与 4-5 节共用第 4 节，属于冲突
        val detail = CourseOverlapCalculator.detail(
            span(studentId = "A", start = 3, end = 4),
            listOf(span(studentId = "B", start = 4, end = 5)),
        )
        assertEquals(CourseOverlapStatus.PARTIAL_OVERLAP, detail.status)
    }

    @Test
    fun `首尾相邻但不共节次不算冲突`() {
        // 3-4 节与 5-6 节完全错开
        val detail = CourseOverlapCalculator.detail(
            span(studentId = "A", start = 3, end = 4),
            listOf(span(studentId = "B", start = 5, end = 6)),
        )
        assertEquals(CourseOverlapStatus.NO_OVERLAP, detail.status)
    }

    @Test
    fun `同时存在完全重合与部分重合时按完全重合上报`() {
        val detail = CourseOverlapCalculator.detail(
            span(studentId = "A", start = 3, end = 4),
            listOf(
                span(studentId = "B", start = 4, end = 6, accountName = "小红", courseName = "大学物理"),
                span(studentId = "C", start = 3, end = 4, accountName = "小刚", courseName = "线性代数"),
            ),
        )
        assertEquals(CourseOverlapStatus.OVERLAP, detail.status)
        assertEquals(listOf("小刚", "小红"), detail.overlappedAccounts)
    }

    @Test
    fun `账号名为空时课程名不带括号后缀`() {
        val detail = CourseOverlapCalculator.detail(
            span(studentId = "A"),
            listOf(span(studentId = "B", accountName = "", courseName = "大学英语")),
        )
        assertEquals(emptyList(), detail.overlappedAccounts)
        assertEquals(listOf("大学英语"), detail.overlappedCourses)
    }

    @Test
    fun `多个账号撞同一节课时去重并排序`() {
        val detail = CourseOverlapCalculator.detail(
            span(studentId = "A"),
            listOf(
                span(studentId = "B", accountName = "小红", courseName = "体育"),
                span(studentId = "C", accountName = "小红", courseName = "体育"),
                span(studentId = "D", accountName = "阿强", courseName = "体育"),
            ),
        )
        assertEquals(listOf("小红", "阿强"), detail.overlappedAccounts)
        assertEquals(listOf("体育（小红）", "体育（阿强）"), detail.overlappedCourses)
    }
}
