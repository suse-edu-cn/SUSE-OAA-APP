package com.suseoaa.projectoaa.domain.exam

import com.suseoaa.projectoaa.shared.domain.model.exam.ExamApiItem
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamCacheEntity
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 2026-01-10 12:00，用作"现在" */
private val NOW = LocalDateTime(2026, 1, 10, 12, 0)

private fun apiItem(
    kcmc: String = "网络安全技术",
    kssj: String = "2026-01-15(09:30-11:30)",
    cdmc: String? = "LA5-322",
    cdxqmc: String? = "",
    xqm: String = "3",
    xqmmc: String = "1",
    xnmc: String = "2025-2026",
) = ExamApiItem(kcmc = kcmc, kssj = kssj, cdmc = cdmc, cdxqmc = cdxqmc,
    ksmc = "期末考试", xnmc = xnmc, xqm = xqm, xqmmc = xqmmc, khfs = "考试", xf = "3.0")

class ExamListBuilderTest {

    @Test
    fun `开考时间晚于当前时间的考试未结束`() {
        val list = ExamListBuilder.fromApiItems(listOf(apiItem(kssj = "2026-01-15(09:30-11:30)")), NOW)
        assertFalse(list.single().isEnded)
    }

    @Test
    fun `结束时间早于当前时间的考试标记为已结束`() {
        val list = ExamListBuilder.fromApiItems(listOf(apiItem(kssj = "2026-01-05(09:30-11:30)")), NOW)
        assertTrue(list.single().isEnded)
    }

    @Test
    fun `正在进行中的考试不算已结束`() {
        // 当前 12:00 落在 11:00-13:00 区间内，尚未到结束时间
        val list = ExamListBuilder.fromApiItems(listOf(apiItem(kssj = "2026-01-10(11:00-13:00)")), NOW)
        assertFalse(list.single().isEnded)
    }

    @Test
    fun `时间待定的考试按未结束处理`() {
        // 否则待定时间的考试会被当成考完了折叠到最后
        val list = ExamListBuilder.fromApiItems(listOf(apiItem(kssj = "时间待定")), NOW)
        assertFalse(list.single().isEnded)
    }

    @Test
    fun `校区非空时拼进地点`() {
        val item = ExamListBuilder.fromApiItems(
            listOf(apiItem(cdmc = "LA5-322", cdxqmc = "临港校区")), NOW).single()
        assertEquals("LA5-322(临港校区)", item.location)
    }

    @Test
    fun `没有教室时显示地点待定`() {
        val item = ExamListBuilder.fromApiItems(listOf(apiItem(cdmc = null, cdxqmc = "")), NOW).single()
        assertEquals("地点待定", item.location)
    }

    @Test
    fun `学期码映射为可读的学期名`() {
        assertEquals("2025-2026 第1学期",
            ExamListBuilder.fromApiItems(listOf(apiItem(xqm = "3")), NOW).single().yearSemester)
        assertEquals("2025-2026 第2学期",
            ExamListBuilder.fromApiItems(listOf(apiItem(xqm = "12")), NOW).single().yearSemester)
        assertEquals("2025-2026 第3学期",
            ExamListBuilder.fromApiItems(listOf(apiItem(xqm = "16")), NOW).single().yearSemester)
    }

    @Test
    fun `未知学期码回落到接口给的学期名称`() {
        assertEquals("2025-2026 第7学期",
            ExamListBuilder.fromApiItems(listOf(apiItem(xqm = "99", xqmmc = "7")), NOW).single().yearSemester)
    }

    @Test
    fun `未结束的考试按开考时间升序排在前面`() {
        val list = ExamListBuilder.fromApiItems(
            listOf(
                apiItem(kcmc = "晚考", kssj = "2026-01-20(09:30-11:30)"),
                apiItem(kcmc = "早考", kssj = "2026-01-12(09:30-11:30)"),
            ), NOW)
        assertEquals(listOf("早考", "晚考"), list.map { it.courseName })
    }

    @Test
    fun `已结束的考试沉到未结束的后面`() {
        val list = ExamListBuilder.fromApiItems(
            listOf(
                apiItem(kcmc = "已考完", kssj = "2026-01-05(09:30-11:30)"),
                apiItem(kcmc = "待考", kssj = "2026-01-20(09:30-11:30)"),
            ), NOW)
        assertEquals(listOf("待考", "已考完"), list.map { it.courseName })
    }

    @Test
    fun `时间无法解析的排在最末`() {
        val list = ExamListBuilder.fromApiItems(
            listOf(
                apiItem(kcmc = "待定", kssj = "时间待定"),
                apiItem(kcmc = "已考完", kssj = "2026-01-05(09:30-11:30)"),
                apiItem(kcmc = "待考", kssj = "2026-01-20(09:30-11:30)"),
            ), NOW)
        assertEquals(listOf("待考", "已考完", "待定"), list.map { it.courseName })
    }

    @Test
    fun `自建考试保留数据库 ID 并标记为自定义`() {
        val item = ExamListBuilder.fromCustomExams(
            listOf(ExamCacheEntity(
                id = 42, studentId = "2021001", courseName = "自建考试",
                time = "2026-01-05(09:30-11:30)", location = "自习室")),
            NOW).single()
        assertEquals(42L, item.id)
        assertTrue(item.isCustom)
        assertTrue(item.isEnded)
    }
}

class SemesterOptionsBuilderTest {

    @Test
    fun `从入学年份列到当前学年`() {
        // 2024 级学生，2026 年 3 月（第 2 学期）
        val options = SemesterOptionsBuilder.build("2024", currentYear = 2026, currentMonth = 3)
        assertEquals(listOf("2025", "2025", "2024", "2024"), options.map { it.year })
    }

    @Test
    fun `当前处于第 1 学期时不列出本学年的第 2 学期`() {
        // 2025 年 10 月：本学年第 2 学期还没开始
        val options = SemesterOptionsBuilder.build("2025", currentYear = 2025, currentMonth = 10)
        assertEquals(listOf("2025" to "3"), options.map { it.year to it.semester })
    }

    @Test
    fun `1 月仍算在上一学年的第 1 学期内`() {
        val options = SemesterOptionsBuilder.build("2025", currentYear = 2026, currentMonth = 1)
        assertEquals(listOf("2025" to "3"), options.map { it.year to it.semester })
    }

    @Test
    fun `年级代码非法时回落到当前年份往前四年`() {
        val options = SemesterOptionsBuilder.build("未知", currentYear = 2026, currentMonth = 9)
        assertEquals("2022", options.last().year)
    }

    @Test
    fun `同一学年内第 1 学期排在第 2 学期之前`() {
        // 注意：这与代码里"最新的在前面"的注释初衷相反，因为学期码是按字符串降序比较，
        // 而 "12" < "3"。此处固定住线上既有行为，将来若要调整需连同本用例一起改。
        val options = SemesterOptionsBuilder.build("2024", currentYear = 2026, currentMonth = 3)
        assertEquals(listOf("3", "12", "3", "12"), options.map { it.semester })
    }

    @Test
    fun `显示名称按入学年份折算成年级`() {
        // 2025 级学生的 2025 学年第 1 学期就是大一上
        val option = SemesterOptionsBuilder.build("2025", currentYear = 2025, currentMonth = 10).single()
        assertEquals("大一上", option.displayName)
    }

    @Test
    fun `年级代码非法时显示名称回落到学年区间`() {
        val options = SemesterOptionsBuilder.build("未知", currentYear = 2026, currentMonth = 9)
        assertEquals("2026-2027学年 第1学期", options.first().displayName)
    }
}
