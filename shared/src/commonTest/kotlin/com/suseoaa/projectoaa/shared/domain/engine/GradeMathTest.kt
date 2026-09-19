package com.suseoaa.projectoaa.shared.domain.engine

import com.suseoaa.projectoaa.shared.domain.model.grade.GradeEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 构造一条成绩记录，只填测试关心的字段。 */
private fun grade(
    courseName: String = "高等数学",
    score: String = "80",
    credit: String = "4",
    gpa: String = "3.0",
) = GradeEntity(
    studentId = "2021001", xnm = "2023", xqm = "3", courseId = "C001",
    courseName = courseName, score = score, credit = credit, gpa = gpa,
    courseType = "必修", examType = "考试", teacher = "张三", examNature = "正常考试",
)

class GradeMathTest {

    // ---------- 绩点折算 ----------

    @Test
    fun `95 分及以上封顶 4_5`() {
        assertEquals(4.5, scoreToGpaPoint(95.0))
        assertEquals(4.5, scoreToGpaPoint(100.0))
    }

    @Test
    fun `60 分以下记 0`() {
        assertEquals(0.0, scoreToGpaPoint(59.9))
        assertEquals(0.0, scoreToGpaPoint(0.0))
    }

    @Test
    fun `60 到 95 分之间每满 5 分加 0_5 绩点`() {
        assertEquals(1.0, scoreToGpaPoint(60.0))
        assertEquals(1.0, scoreToGpaPoint(64.9))   // 不满 5 分不进位
        assertEquals(1.5, scoreToGpaPoint(65.0))
        assertEquals(2.5, scoreToGpaPoint(75.0))
        assertEquals(4.0, scoreToGpaPoint(90.0))
        assertEquals(4.0, scoreToGpaPoint(94.9))   // 未到 95 仍是 4.0，不会提前封顶
    }

    // ---------- 成绩语义 ----------

    @Test
    fun `等级词按通过语义判定及格`() {
        listOf("优秀", "良好", "中等", "及格", "通过", "合格").forEach {
            assertTrue(isPassingScore(it), "$it 应判为及格")
        }
        listOf("不及格", "缺考", "缓考").forEach {
            assertFalse(isPassingScore(it), "$it 不应判为及格")
        }
    }

    @Test
    fun `数字成绩按 60 分线判定`() {
        assertTrue(isPassingScore("60"))
        assertFalse(isPassingScore("59.5"))
    }

    @Test
    fun `等级词折算成该档代表分`() {
        assertEquals(95.0, parseScore("优秀"))
        assertEquals(65.0, parseScore("及格"))
        assertEquals(0.0, parseScore("缺考"))
        assertEquals(88.0, parseScore("88"))
    }

    @Test
    fun `无法解析的成绩与学分按 0 处理而不是抛异常`() {
        assertEquals(0.0, parseScore("待录入"))
        assertEquals(0.0, parseCredit(""))
        assertEquals(0.0, parseGpa("—"))
    }

    // ---------- 加权绩点 ----------

    @Test
    fun `加权绩点按学分加权而非简单平均`() {
        val (gpa, credits) = calculateWeightedGpa(
            listOf(
                grade(courseName = "高数", credit = "5", gpa = "4.0"),
                grade(courseName = "体育", credit = "1", gpa = "1.0"),
            )
        )
        assertEquals(6.0, credits)
        assertEquals(3.5, gpa)   // (4.0*5 + 1.0*1) / 6 —— 简单平均会得到 2.5
    }

    @Test
    fun `没有有效学分时加权绩点为 0 而不是除零`() {
        val (gpa, credits) = calculateWeightedGpa(listOf(grade(credit = "0")))
        assertEquals(0.0, credits)
        assertEquals(0.0, gpa)
    }

    // ---------- 重修取最高分 ----------

    @Test
    fun `同一门课有重修时取最高分那次`() {
        val best = listOf(
            grade(courseName = "大学物理", score = "45"),
            grade(courseName = "大学物理", score = "72"),
        ).bestAttemptsByCourse()
        assertEquals("72", best.getValue("大学物理").score)
    }

    @Test
    fun `重修已通过的课不再算作待重修`() {
        val failed = listOf(
            grade(courseName = "大学物理", score = "45"),
            grade(courseName = "大学物理", score = "72"),
            grade(courseName = "线性代数", score = "50"),
        ).failedBestAttempts()
        assertEquals(listOf("线性代数"), failed.map { it.courseName })
    }

    @Test
    fun `课程名为空的记录不参与统计`() {
        assertTrue(listOf(grade(courseName = "")).bestAttemptsByCourse().isEmpty())
    }

    // ---------- 教师字段 ----------

    @Test
    fun `合并的教师字段按常见分隔符拆开`() {
        val teachers = splitTeachers("张三,李四")
        assertTrue(teachers.containsAll(listOf("张三", "李四")), "实际: $teachers")
    }
}
