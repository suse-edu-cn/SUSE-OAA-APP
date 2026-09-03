package com.suseoaa.projectoaa.shared.domain.engine

import com.suseoaa.projectoaa.shared.data.repository.GradeEntity

/**
 * 学业问答的文案生成。
 *
 * 分两类输出：
 * - `answer*`：可以直接回给用户的完整答案，命中高置信度意图时使用
 * - `summary*` / [formatGradeRecords]：喂给大模型的派生事实片段
 *
 * 本类只做「事实 → 文字」，不判断意图，也不做数值计算（计算见 GradeMath.kt）。
 */
internal class AcademicAnswerBuilder(
    private val grades: List<GradeEntity>,
    private val termIndex: TermIndex
) {

    private val graduationCredits = 165.0
    private val lowScoreLine = 70.0

    // ==================== 直接答案 ====================

    fun answerTeacherList(): String {
        val teacherCourses = grades.asSequence()
            .flatMap { grade -> splitTeachers(grade.teacher).map { it to grade.courseName } }
            .filter { (teacher, courseName) -> teacher.isNotBlank() && courseName.isNotBlank() }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, courseNames) -> courseNames.distinct().sorted() }

        if (teacherCourses.isEmpty()) return "当前成绩数据里没有记录任课教师信息。"

        val lines = teacherCourses.entries.sortedBy { it.key }.map { (teacher, courseNames) ->
            "- $teacher：${courseNames.joinToString("、")}"
        }
        return "你成绩记录中出现过 ${teacherCourses.size} 位任课教师：\n" + lines.joinToString("\n")
    }

    fun answerFailedCourses(): String {
        val failedCourses = grades.failedBestAttempts()
        if (failedCourses.isEmpty()) return "当前成绩数据中没有不及格、未通过或需要重修的课程。"

        val lines = failedCourses.map { it.toDetailLine() }
        return "当前成绩数据中有 ${failedCourses.size} 门课程未通过或需要重修：\n" +
            lines.joinToString("\n")
    }

    /** 估算把绩点提到目标值所需的重修表现 */
    fun answerGpaRepairPlan(query: String): String {
        val targetGpa = parseTargetGpa(query) ?: 2.0
        val failedCourses = grades.failedBestAttempts()
        val currentCourses = grades.bestAttemptsByCourse().values.toList()

        if (failedCourses.isEmpty()) {
            val (currentGpa, earnedCredits) = calculateWeightedGpa(currentCourses)
            return "当前没有检测到需要重修的课程。你目前绩点约为 ${currentGpa.formatDecimal(2)}，" +
                "已获学分 ${earnedCredits.formatDecimal(1)}。"
        }

        val currentCredits = currentCourses.sumOf { parseCredit(it.credit) }
        val passedPoints = currentCourses
            .filter { isPassingScore(it.score) }
            .sumOf { parseGpa(it.gpa) * parseCredit(it.credit) }
        val failedCredits = failedCourses.sumOf { parseCredit(it.credit) }
        val neededAverageGpa = if (failedCredits > 0) {
            ((targetGpa * currentCredits) - passedPoints) / failedCredits
        } else {
            0.0
        }.coerceAtLeast(0.0)

        val failedLines = failedCourses.map { grade ->
            "- ${grade.courseName}：当前 ${grade.score}，${formatCredit(grade.credit)}学分"
        }

        return """
            需要优先重修的课程：
            ${failedLines.joinToString("\n")}

            以当前本地成绩表粗略估算，要让绩点达到 ${targetGpa.formatDecimal(2)}：
            - 这些重修课平均绩点大约需要达到 ${neededAverageGpa.formatDecimal(2)}
            - 折算成绩大约需要 ${estimateScoreFromGpa(neededAverageGpa)}
            - 难度判断：${difficultyLabel(neededAverageGpa)}

            说明：这是基于本地成绩表的近似估算，实际学位绩点可能还受学位课程范围、重修替换规则、学校绩点换算规则影响。
        """.trimIndent()
    }

    fun answerCourseLookup(relevantGrades: List<GradeEntity>): String? {
        if (relevantGrades.isEmpty()) return null
        return "我在本地成绩表中找到这些相关课程：\n" + formatGradeRecords(relevantGrades.take(8))
    }

    fun answerLowScoreCourses(query: String): String {
        val threshold = parseScoreThreshold(query) ?: lowScoreLine
        val lowScoreCourses = grades.bestAttemptsByCourse().values
            .filter { parseScore(it.score) in 0.0..<threshold }
            .sortedWith(compareBy<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName })

        if (lowScoreCourses.isEmpty()) {
            return "基于本地成绩表，按每门课最高成绩计算，没有低于 ${threshold.formatScore()} 分的课程。"
        }

        val lines = lowScoreCourses.map { it.toDetailLine() }
        return "基于本地成绩表，按每门课最高成绩计算，低于 ${threshold.formatScore()} 分的课程有 " +
            "${lowScoreCourses.size} 门：\n" + lines.joinToString("\n")
    }

    fun answerTermGrades(query: String): String? {
        val termQuery = parseTermQuery(query, termIndex) ?: return null
        val termGrades = grades
            .filter { termQuery.matches(it) }
            .sortedWith(compareBy<GradeEntity> { it.courseName }.thenBy { it.courseId })

        if (termGrades.isEmpty()) {
            return "基于本地成绩表，没有找到 ${termQuery.displayName()} 的成绩记录。"
        }

        val earnedCredits = termGrades
            .filter { isPassingScore(it.score) }
            .sumOf { parseCredit(it.credit) }
        val (termGpa, totalCredits) = calculateWeightedGpa(termGrades)
        val failedCount = termGrades.count { !isPassingScore(it.score) }
        val lines = termGrades.map { grade ->
            "${grade.toDetailLine()}，教师 ${grade.teacher.ifBlank { "未记录" }}"
        }

        return """
            基于本地成绩表，${termQuery.displayName()} 共 ${termGrades.size} 门课：
            - 本学期加权绩点约 ${termGpa.formatDecimal(2)}
            - 已获学分 ${earnedCredits.formatDecimal(1)} / 记录学分 ${totalCredits.formatDecimal(1)}
            - 未通过课程 $failedCount 门

            ${lines.joinToString("\n")}
        """.trimIndent()
    }

    fun answerScoreRanking(query: String): String {
        val highest = !containsAny(query, AcademicTerms.lowest)
        val rankedCourses = grades.bestAttemptsByCourse().values
            .sortedWith(scoreComparator(descending = highest))
            .take(5)

        if (rankedCourses.isEmpty()) return "当前本地成绩表中没有可用于排序的课程成绩。"

        val title = if (highest) "成绩最高的课程" else "成绩最低的课程"
        val lines = rankedCourses.mapIndexed { index, grade ->
            "${index + 1}. ${grade.courseName}：${grade.score}，" +
                "${formatCredit(grade.credit)}学分，绩点 ${grade.gpa.ifBlank { "0" }}"
        }
        return "基于本地成绩表，按每门课最高成绩计算，$title 是：\n" + lines.joinToString("\n")
    }

    fun answerCreditGpaSummary(): String {
        val bestAttempts = grades.bestAttemptsByCourse().values.toList()
        val (gpa, totalCredits) = calculateWeightedGpa(bestAttempts)
        val earnedCredits = bestAttempts
            .filter { isPassingScore(it.score) }
            .sumOf { parseCredit(it.credit) }
        val missing = (graduationCredits - earnedCredits).coerceAtLeast(0.0)
        val lowScoreCount = bestAttempts.count { parseScore(it.score) in 0.0..<lowScoreLine }

        return """
            基于本地成绩表，按每门课最高成绩计算：
            - 已记录课程：${bestAttempts.size} 门
            - 已获学分：${earnedCredits.formatDecimal(1)}
            - 记录总学分：${totalCredits.formatDecimal(1)}
            - 加权绩点约：${gpa.formatDecimal(2)}
            - 距离 ${graduationCredits.formatScore()} 学分毕业要求还差约：${missing.formatDecimal(1)}
            - 未通过课程：${grades.failedBestAttempts().size} 门
            - 低于 ${lowScoreLine.formatScore()} 分课程：$lowScoreCount 门
        """.trimIndent()
    }

    // ==================== 供大模型使用的派生事实 ====================

    fun formatGradeRecords(records: List<GradeEntity>): String = records.joinToString("\n") { grade ->
        "- [${grade.xnm}-${grade.xqm}] ${grade.courseName}｜成绩:${grade.score}｜" +
            "学分:${formatCredit(grade.credit)}｜绩点:${grade.gpa.ifBlank { "0" }}｜" +
            "教师:${grade.teacher.ifBlank { "未记录" }}｜" +
            "课程性质:${grade.courseType.ifBlank { "未记录" }}｜" +
            "考试性质:${grade.examNature.ifBlank { "未记录" }}"
    }

    fun summaryFailedCourses(failedCourses: List<GradeEntity>): String {
        if (failedCourses.isEmpty()) return "无"
        return failedCourses.joinToString("；") { grade ->
            "${grade.courseName}(${grade.score}，${formatCredit(grade.credit)}学分)"
        }
    }

    fun summaryLowScores(maxItems: Int): String {
        val lowScoreCourses = grades.bestAttemptsByCourse().values
            .filter { parseScore(it.score) in 0.0..<lowScoreLine }
            .sortedWith(compareBy<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName })
            .take(maxItems)

        if (lowScoreCourses.isEmpty()) return "无"
        return lowScoreCourses.joinToString("；") { "${it.courseName}(${it.score})" }
    }

    fun summaryTerms(): String {
        if (termIndex.sortedTerms.isEmpty()) return "无学期记录"
        return termIndex.sortedTerms.joinToString("，") { term ->
            val count = grades.count { it.xnm == term.xnm && it.xqm == term.xqm }
            "${term.displayName()}(${count}门课)"
        }
    }

    /** 最高分或最低分的课程，同分时全部列出 */
    fun summaryScoreExtremes(highest: Boolean): String {
        val attempts = grades.bestAttemptsByCourse().values
        if (attempts.isEmpty()) return "无"
        val extremeScore = (
            if (highest) attempts.maxOfOrNull { parseScore(it.score) }
            else attempts.minOfOrNull { parseScore(it.score) }
            ) ?: return "无"

        return attempts
            .filter { parseScore(it.score) == extremeScore }
            .joinToString("；") { "${it.courseName}(${it.score})" }
    }

    fun summaryTeachers(): String {
        val teachers = grades.flatMap { splitTeachers(it.teacher) }.distinct().sorted()
        return if (teachers.isEmpty()) "无教师字段记录" else teachers.joinToString("、")
    }

    // ==================== 内部工具 ====================

    private fun GradeEntity.toDetailLine(): String =
        "- $courseName：$score，${formatCredit(credit)}学分，绩点 ${gpa.ifBlank { "0" }}"

    private fun scoreComparator(descending: Boolean): Comparator<GradeEntity> =
        if (descending) {
            compareByDescending<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName }
        } else {
            compareBy<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName }
        }
}
