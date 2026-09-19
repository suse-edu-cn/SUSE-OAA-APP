package com.suseoaa.projectoaa.domain.exam

import androidx.compose.runtime.Immutable
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamCacheEntity
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamApiItem
import com.suseoaa.projectoaa.shared.util.SemesterNaming
import com.suseoaa.projectoaa.shared.util.parseExamTimeRange
import kotlinx.datetime.LocalDateTime

/**
 * 考试列表的组装规则。
 *
 * 教务接口返回的考试项和用户自建的考试项要合成同一份列表，还要按"是否已考完 +
 * 开考时间"排序。这些规则原本是 ExamViewModel 的私有方法，依赖 OaaClock 直接取
 * 当前时间，既无从测试也说不清边界。这里把"现在几点"改成显式入参，整体变成纯函数。
 */

data class ExamUiItem(
    val id: Long = 0,              // 数据库 ID，用于编辑和删除
    val courseName: String,
    val examName: String,
    val time: String,
    val location: String,
    val credit: String,
    val examType: String,
    val yearSemester: String,      // 学年学期: "2025-2026 第1学期"
    val isEnded: Boolean = false,
    val isCustom: Boolean = false,
)

@Immutable
data class SemesterOption(
    val year: String,              // 学年码: "2025"
    val semester: String,          // 学期码: "3" / "12"
    val displayName: String,       // 显示名称: "2025-2026 第1学期"
)

object ExamListBuilder {

    /** 教务接口返回的考试项。[now] 用于判定是否已考完。 */
    fun fromApiItems(items: List<ExamApiItem>, now: LocalDateTime): List<ExamUiItem> =
        items.map { item ->
            var location = item.cdmc ?: "地点待定"
            if (!item.cdxqmc.isNullOrBlank()) location += "(${item.cdxqmc})"

            ExamUiItem(
                id = 0,                     // 接口返回的没有本地 ID
                courseName = item.kcmc ?: "未知课程",
                examName = item.ksmc ?: "",
                time = item.kssj ?: "时间待定",
                location = location,
                credit = item.xf ?: "",
                examType = item.khfs ?: "考试",
                yearSemester = "${item.xnmc ?: ""} ${semesterName(item.xqm, item.xqmmc)}",
                isEnded = hasEnded(item.kssj ?: "", now),
                isCustom = false,
            )
        }.sortedWith(byEndedThenStartTime)

    /** 用户自建的考试项。 */
    fun fromCustomExams(exams: List<ExamCacheEntity>, now: LocalDateTime): List<ExamUiItem> =
        exams.map { exam ->
            ExamUiItem(
                id = exam.id,
                courseName = exam.courseName,
                examName = exam.examName,
                time = exam.time,
                location = exam.location,
                credit = exam.credit,
                examType = exam.examType,
                yearSemester = exam.yearSemester,
                isEnded = hasEnded(exam.time, now),
                isCustom = true,
            )
        }

    /** 时间解析不出来时一律按"未结束"处理，免得待定时间的考试被折叠到最后。 */
    private fun hasEnded(rawTime: String, now: LocalDateTime): Boolean =
        parseExamTimeRange(rawTime)?.let { now > it.second } ?: false

    private fun semesterName(xqm: String?, xqmmc: String?): String = when (xqm) {
        "3" -> "第1学期"
        "12" -> "第2学期"
        "16" -> "第3学期"
        else -> "第${xqmmc ?: "?"}学期"
    }

    /** 已考完的沉到最后；其余按开考时间升序；时间无法解析的排在最末。 */
    internal val byEndedThenStartTime = Comparator<ExamUiItem> { a, b ->
        val timesA = parseExamTimeRange(a.time)
        val timesB = parseExamTimeRange(b.time)
        when {
            timesA == null && timesB == null -> 0
            timesA == null -> 1
            timesB == null -> -1
            a.isEnded != b.isEnded -> if (a.isEnded) 1 else -1
            else -> timesA.first.compareTo(timesB.first)
        }
    }
}

object SemesterOptionsBuilder {

    /**
     * 生成学期下拉项，从入学年份排到当前学期。
     *
     * @param njdmId 年级代码，通常就是入学年份
     * @param currentYear / [currentMonth] 当前日期，决定最新一个可选学期
     */
    fun build(njdmId: String, currentYear: Int, currentMonth: Int): List<SemesterOption> {
        // 真实入学年份，取不到时为 null——它只用于年级命名，不能拿回退值冒充，
        // 否则「大一上」会被算成「大五上」
        val enrollmentYear = njdmId.take(4).toIntOrNull()
        // 生成可选学年区间时才用回退值
        val listFromYear = enrollmentYear ?: (currentYear - 4)

        // 学年从 8 月起算；8 月-次年 1 月为第 1 学期(3)，2-7 月为第 2 学期(12)
        val currentAcademicYear = if (currentMonth >= 8) currentYear else currentYear - 1
        val currentSemesterCode = if (currentMonth >= 8 || currentMonth <= 1) "3" else "12"

        val semesters = mutableListOf<SemesterOption>()
        for (academicYear in currentAcademicYear downTo listFromYear) {
            val isCurrentAcademicYear = academicYear == currentAcademicYear

            val showFirstSemester = !isCurrentAcademicYear ||
                currentMonth >= 8 || currentMonth <= 1 || currentSemesterCode == "12"
            // 当前学年若还处在第 1 学期，第 2 学期尚未开始，不列出
            val showSecondSemester =
                if (isCurrentAcademicYear) currentSemesterCode == "12" else true

            if (showFirstSemester) {
                semesters += SemesterOption(
                    academicYear.toString(), "3",
                    SemesterNaming.short(enrollmentYear, academicYear.toString(), "3"))
            }
            if (showSecondSemester) {
                semesters += SemesterOption(
                    academicYear.toString(), "12",
                    SemesterNaming.short(enrollmentYear, academicYear.toString(), "12"))
            }
        }

        // 学年降序。同一学年内按学期码字符串降序，由于 "12" < "3"，实际是第1学期排在
        // 第2学期之前——与"最新的在前面"的初衷不一致，但这是线上既有行为，此处照搬未改。
        return semesters.sortedWith { a, b ->
            val yearCompare = b.year.compareTo(a.year)
            if (yearCompare != 0) yearCompare else b.semester.compareTo(a.semester)
        }
    }
}
