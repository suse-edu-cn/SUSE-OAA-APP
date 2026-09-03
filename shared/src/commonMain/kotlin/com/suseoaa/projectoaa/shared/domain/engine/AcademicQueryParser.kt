package com.suseoaa.projectoaa.shared.domain.engine

import com.suseoaa.projectoaa.shared.data.repository.GradeEntity

/**
 * 学业问答的查询解析：分词、归一化、相关度打分与学期表达式解析。
 *
 * 这一层只理解“用户说了什么”，不产出任何回答文案。
 */

/** 意图识别与相关度打分共用的词表 */
internal object AcademicTerms {
    val teacher = listOf("老师", "教师", "任课", "授课", "教过", "导师")
    val list = listOf("哪些", "谁", "列表", "上过", "教过", "都有", "全部", "所有")
    val failed = listOf(
        "挂科", "挂过", "挂了", "没过", "不及格", "未通过", "不合格",
        "低于60", "低于 60", "补考", "重修"
    )
    val course = listOf("课程", "科目", "课", "成绩", "分数")
    val gpa = listOf("绩点", "GPA", "gpa", "学位绩点")
    val target = listOf("达到", "达标", "到", "多少分", "考多少", "提升", "提高")
    val summary = listOf("总结", "概况", "情况", "分析", "怎么样", "毕业", "学分")
    val lowScore = listOf(
        "低分", "考得差", "分低", "差的课", "垫底", "最差", "低成绩", "分数较低", "成绩较低"
    )
    val term = listOf(
        "学期", "学年", "上学期", "下学期", "第一学期", "第二学期",
        "大一", "大二", "大三", "大四", "大五", "最新", "最近", "秋", "春"
    )
    val ranking = listOf(
        "最高", "最低", "排行", "排名", "第一", "最好", "最差", "垫底",
        "前几", "后几", "前五", "后五", "最棒", "最优秀"
    )
    val score = listOf("分数", "成绩", "分")
    val lowest = listOf("最低", "最差", "垫底", "最差的", "倒数第一", "分数最低", "成绩最低")
    val credit = listOf("学分")
}

internal fun containsAny(text: String, terms: List<String>): Boolean =
    terms.any { text.contains(it, ignoreCase = true) }

/** 统一大小写与全半角，便于后续匹配 */
internal fun normalizeForSearch(text: String): String =
    text.lowercase()
        .replace(" ", "")
        .replace("　", "")
        .replace("，", ",")
        .replace("。", ".")
        .replace("（", "(")
        .replace("）", ")")

/** 切出英文数字串与长度 ≥2 的中文串；过长的中文串再按窗口切细 */
internal fun searchTerms(normalizedQuery: String): List<String> =
    Regex("""[a-z0-9]+|[一-龥]{2,}""")
        .findAll(normalizedQuery)
        .map { it.value }
        .flatMap { term ->
            if (term.length <= 8) {
                listOf(term)
            } else {
                term.windowed(4, 2, partialWindows = true).filter { it.length >= 2 }
            }
        }
        .distinct()
        .toList()

/** 中文二元组，用于模糊匹配course名 */
internal fun chineseBigrams(text: String): List<String> {
    val chineseOnly = text.filter { it in '一'..'龥' }
    return if (chineseOnly.length < 2) emptyList() else chineseOnly.windowed(2).distinct()
}

/** 从“低于 70”这类表达中取出分数阈值 */
internal fun parseScoreThreshold(query: String): Double? {
    val pattern = Regex("""(?:低于|小于|不到|分[数<]|低于分)\s*([0-9]+)""")
    return pattern.find(query)?.groupValues?.get(1)?.toDoubleOrNull()
}

/** 从“绩点要到 3.0”这类表达中取出目标绩点，只接受 0~5 的取值 */
internal fun parseTargetGpa(query: String): Double? {
    if (!containsAny(query, AcademicTerms.gpa)) return null
    return Regex("""([0-9]+(?:[.。][0-9]+)?)""")
        .findAll(query)
        .mapNotNull { it.value.replace("。", ".").toDoubleOrNull() }
        .firstOrNull { it in 0.0..5.0 }
}

/** 单条成绩记录与查询的相关度，分值越高越相关 */
internal fun relevanceScore(query: String, grade: GradeEntity): Int {
    if (query.isBlank()) return 1

    val normalizedQuery = normalizeForSearch(query)
    val recordText = normalizeForSearch(
        listOf(
            grade.courseName, grade.teacher, grade.score, grade.credit, grade.gpa,
            grade.courseType, grade.examType, grade.examNature, grade.xnm, grade.xqm
        ).joinToString(" ")
    )

    var score = 0
    val courseName = normalizeForSearch(grade.courseName)
    val teacher = normalizeForSearch(grade.teacher)

    if (courseName.isNotBlank() && normalizedQuery.contains(courseName)) score += 80
    if (teacher.isNotBlank() && normalizedQuery.contains(teacher)) score += 50

    searchTerms(normalizedQuery).forEach { term ->
        if (term.length >= 2 && recordText.contains(term)) score += 12
    }
    chineseBigrams(normalizedQuery).forEach { bigram ->
        if (recordText.contains(bigram)) score += 2
    }

    if (containsAny(query, AcademicTerms.failed) && !isPassingScore(grade.score)) score += 100
    if (containsAny(query, AcademicTerms.gpa) && grade.gpa.isNotBlank()) score += 12
    if ((query.contains("学分") || query.contains("多少分")) && grade.credit.isNotBlank()) score += 8
    if (containsAny(query, AcademicTerms.teacher) && grade.teacher.isNotBlank()) score += 10

    return score
}

// ==================== 学期 ====================

/** 学期标识：xnm=学年，xqm="3" 表示第一学期、"12" 表示第二学期 */
internal data class Term(val xnm: String, val xqm: String) : Comparable<Term> {
    val yearInt = xnm.toIntOrNull() ?: 0
    val termVal = if (xqm == "3") 1 else 2

    fun displayName(): String =
        "${xnm}学年${if (xqm == "3") "第一学期" else "第二学期"}"

    override fun compareTo(other: Term): Int =
        if (yearInt != other.yearInt) yearInt.compareTo(other.yearInt) else termVal.compareTo(other.termVal)
}

/**
 * 从成绩记录中派生的学期索引：入学年份与按时间排好序的学期列表。
 * “上学期 / 本学期 / 大二下”这类相对表达都要靠它来定位。
 */
internal class TermIndex(grades: List<GradeEntity>) {
    val enrollmentYear: Int = grades.asSequence()
        .mapNotNull { it.xnm.toIntOrNull() }
        .minOrNull() ?: 2023

    val sortedTerms: List<Term> = grades.asSequence()
        .filter { it.xnm.isNotBlank() && it.xqm.isNotBlank() }
        .map { Term(it.xnm, it.xqm) }
        .distinct()
        .sorted()
        .toList()
}

internal interface TermQuery {
    fun matches(grade: GradeEntity): Boolean
    fun displayName(): String
}

internal class ExactTermQuery(val xnm: String, val xqm: String) : TermQuery {
    override fun matches(grade: GradeEntity): Boolean = grade.xnm == xnm && grade.xqm == xqm
    override fun displayName(): String = Term(xnm, xqm).displayName()
}

internal class YearOnlyTermQuery(val xnm: String) : TermQuery {
    override fun matches(grade: GradeEntity): Boolean = grade.xnm == xnm
    override fun displayName(): String = "$xnm-${(xnm.toIntOrNull() ?: 0) + 1} 学年"
}

internal class GradeTermQuery(
    val gradeName: String,
    val termVal: Int?,
    val gradeNum: Int,
    val enrollmentYear: Int
) : TermQuery {
    override fun matches(grade: GradeEntity): Boolean {
        val targetXnm = (enrollmentYear + (gradeNum - 1)).toString()
        val xqmMatch = when (termVal) {
            1 -> grade.xqm == "3"
            2 -> grade.xqm == "12"
            else -> true
        }
        return grade.xnm == targetXnm && xqmMatch
    }

    override fun displayName(): String =
        gradeName + when (termVal) {
            1 -> "第一学期"
            2 -> "第二学期"
            else -> ""
        }
}

internal class RelativeTermQuery(val relativeType: String, val resolvedTerm: Term?) : TermQuery {
    override fun matches(grade: GradeEntity): Boolean =
        resolvedTerm != null && grade.xnm == resolvedTerm.xnm && grade.xqm == resolvedTerm.xqm

    override fun displayName(): String =
        "$relativeType（${resolvedTerm?.displayName() ?: "未知学期"}）"
}

/**
 * 解析查询里的学期表达式，按“相对学期 → 学年区间 → 单个年份 → 年级”的优先级依次尝试。
 * 无法识别时返回 null，调用方据此退回到不限学期的检索。
 */
internal fun parseTermQuery(query: String, termIndex: TermIndex): TermQuery? {
    // 1. 相对学期
    if (query.contains("上学期") || query.contains("前一学期") || query.contains("上个学期")) {
        val resolved = termIndex.sortedTerms.getOrNull(termIndex.sortedTerms.size - 2)
            ?: termIndex.sortedTerms.lastOrNull()
        return RelativeTermQuery("上学期", resolved)
    }
    if (query.contains("本学期") || query.contains("这学期") || query.contains("最近学期") ||
        query.contains("最新学期") || query.contains("最近一个学期")
    ) {
        return RelativeTermQuery("最近学期", termIndex.sortedTerms.lastOrNull())
    }

    // 2. 学年区间，如 2024-2025-1 / 2024-2025学年第一学期
    Regex("""(20[0-9]{2})[-~和/及与至]*(20[0-9]{2})""").find(query)?.let { match ->
        val xnm = match.groupValues[1]
        val rest = query.substring(match.range.last + 1)
        return termQueryOf(xnm, semesterOf(rest))
    }

    // 3. 单个年份，如 2024年第一学期 / 2024秋
    Regex("""(20[0-9]{2})""").find(query)?.let { match ->
        val xnm = match.groupValues[1]
        val rest = query.replace(xnm, "")
        return termQueryOf(xnm, semesterOf(rest, allowSeason = true))
    }

    // 4. 年级，如 大二下
    Regex("""大([一二三四五12345])""").find(query)?.let { match ->
        val gradeChar = match.groupValues[1]
        val gradeNum = when (gradeChar) {
            "一", "1" -> 1
            "二", "2" -> 2
            "三", "3" -> 3
            "四", "4" -> 4
            "五", "5" -> 5
            else -> 1
        }
        val termVal = when {
            query.contains("上") || query.contains("一") || query.contains("1") || query.contains("3") -> 1
            query.contains("下") || query.contains("二") || query.contains("2") || query.contains("12") -> 2
            else -> null
        }
        return GradeTermQuery("大$gradeChar", termVal, gradeNum, termIndex.enrollmentYear)
    }

    return null
}

/** 从年份之外的残余文本里判断是第一学期还是第二学期 */
private fun semesterOf(rest: String, allowSeason: Boolean = false): String? = when {
    rest.contains("一") || rest.contains("1") || rest.contains("3") || rest.contains("上") ||
        (allowSeason && rest.contains("秋")) -> "3"
    rest.contains("二") || rest.contains("2") || rest.contains("12") || rest.contains("下") ||
        (allowSeason && rest.contains("春")) -> "12"
    else -> null
}

private fun termQueryOf(xnm: String, xqm: String?): TermQuery =
    if (xqm != null) ExactTermQuery(xnm, xqm) else YearOnlyTermQuery(xnm)
