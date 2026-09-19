package com.suseoaa.projectoaa.domain.gpa

import com.suseoaa.projectoaa.shared.domain.model.gpa.GpaCourseWrapper
import com.suseoaa.projectoaa.shared.domain.model.grade.GradeEntity
import kotlin.test.Test
import kotlin.test.assertEquals

private fun course(
    courseName: String = "高等数学",
    xnm: String = "2023",
    xqm: String = "3",
    credit: String = "4",
    gpa: String = "3.0",
    isDegreeCourse: Boolean = false,
    included: Boolean = true,
    simulatedScore: Double? = null,
    simulatedGpa: Double? = null,
) = GpaCourseWrapper(
    originalEntity = GradeEntity(
        studentId = "2021001", xnm = xnm, xqm = xqm, courseId = courseName,
        courseName = courseName, score = "80", credit = credit, gpa = gpa,
        courseType = "必修", examType = "考试", teacher = "张三", examNature = "正常考试",
    ),
    isDegreeCourse = isDegreeCourse,
    simulatedScore = simulatedScore,
    simulatedGpa = simulatedGpa,
    isIncludedInCalculation = included,
)

class GpaCalculatorTest {

    // ---------- 统计 ----------

    @Test
    fun `总绩点按学分加权`() {
        val stats = GpaCalculator.stats(
            listOf(
                course(courseName = "高数", credit = "5", gpa = "4.0"),
                course(courseName = "体育", credit = "1", gpa = "1.0"),
            )
        )
        assertEquals("3.50", stats.totalGpa)   // (4.0*5 + 1.0*1) / 6
        assertEquals("6.0", stats.totalCredits)
    }

    @Test
    fun `学位课绩点只统计学位课`() {
        val stats = GpaCalculator.stats(
            listOf(
                course(courseName = "高数", credit = "4", gpa = "4.0", isDegreeCourse = true),
                course(courseName = "体育", credit = "2", gpa = "1.0", isDegreeCourse = false),
            )
        )
        assertEquals("3.00", stats.totalGpa)    // 全部课程
        assertEquals("4.00", stats.degreeGpa)   // 只有高数
        assertEquals("4.0", stats.degreeCredits)
    }

    @Test
    fun `被用户排除的课不计入统计`() {
        val stats = GpaCalculator.stats(
            listOf(
                course(courseName = "高数", credit = "4", gpa = "4.0"),
                course(courseName = "挂科课", credit = "4", gpa = "0.0", included = false),
            )
        )
        assertEquals("4.00", stats.totalGpa)
        assertEquals("4.0", stats.totalCredits)
    }

    @Test
    fun `学分为 0 的课不计入统计`() {
        val stats = GpaCalculator.stats(
            listOf(
                course(courseName = "高数", credit = "4", gpa = "4.0"),
                course(courseName = "讲座", credit = "0", gpa = "0.0"),
            )
        )
        assertEquals("4.00", stats.totalGpa)
        assertEquals("4.0", stats.totalCredits)
    }

    @Test
    fun `没有课时统计为零而不是除零`() {
        val stats = GpaCalculator.stats(emptyList())
        assertEquals("0.00", stats.totalGpa)
        assertEquals("0.0", stats.totalCredits)
        assertEquals("0.00", stats.degreeGpa)
    }

    @Test
    fun `模拟改分后的绩点参与统计`() {
        // 用户把一门课模拟改成 4.5，总绩点应随之变化
        val stats = GpaCalculator.stats(
            listOf(course(credit = "4", gpa = "2.0", simulatedScore = 96.0, simulatedGpa = 4.5))
        )
        assertEquals("4.50", stats.totalGpa)
    }

    // ---------- 筛选与排序 ----------

    @Test
    fun `按学期筛选匹配学年加学期`() {
        val all = listOf(
            course(courseName = "A", xnm = "2023", xqm = "3"),
            course(courseName = "B", xnm = "2023", xqm = "12"),
            course(courseName = "C", xnm = "2024", xqm = "3"),
        )
        assertEquals(listOf("A"), GpaCalculator.filterByTerm(all, "2023_3").map { it.originalEntity.courseName })
        assertEquals(3, GpaCalculator.filterByTerm(all, GpaCalculator.ALL_TERMS).size)
    }

    @Test
    fun `只看学位课时过滤掉非学位课`() {
        val all = listOf(
            course(courseName = "A", isDegreeCourse = true),
            course(courseName = "B", isDegreeCourse = false),
        )
        assertEquals(listOf("A"),
            GpaCalculator.filterByType(all, FilterType.DEGREE_ONLY).map { it.originalEntity.courseName })
        assertEquals(2, GpaCalculator.filterByType(all, FilterType.ALL).size)
    }

    @Test
    fun `按分数升序与降序排列`() {
        val all = listOf(
            course(courseName = "低", simulatedScore = 60.0),
            course(courseName = "高", simulatedScore = 95.0),
        )
        assertEquals(listOf("高", "低"),
            GpaCalculator.sort(all, SortOrder.DESCENDING).map { it.originalEntity.courseName })
        assertEquals(listOf("低", "高"),
            GpaCalculator.sort(all, SortOrder.ASCENDING).map { it.originalEntity.courseName })
    }

    @Test
    fun `切到只看学位课时总绩点仍是该学期全部课程的口径`() {
        // 这是页面刻意的口径：统计在类型筛选之前算，切换筛选只改列表不改顶部数字。
        // 写成用例是为了固定住它，避免以后被当成 bug 改掉。
        val all = listOf(
            course(courseName = "学位课", credit = "4", gpa = "4.0", isDegreeCourse = true),
            course(courseName = "选修课", credit = "4", gpa = "2.0", isDegreeCourse = false),
        )
        val termFiltered = GpaCalculator.filterByTerm(all, GpaCalculator.ALL_TERMS)
        val stats = GpaCalculator.stats(termFiltered)
        val visible = GpaCalculator.filterByType(termFiltered, FilterType.DEGREE_ONLY)

        assertEquals("3.00", stats.totalGpa)   // 两门课的加权，不是 4.00
        assertEquals(1, visible.size)
    }
}
