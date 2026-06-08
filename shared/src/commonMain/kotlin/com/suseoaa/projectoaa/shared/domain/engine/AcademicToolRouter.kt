package com.suseoaa.projectoaa.shared.domain.engine

import com.suseoaa.projectoaa.shared.data.repository.GradeEntity

enum class AcademicToolIntent {
    TEACHER_LIST,
    FAILED_COURSES,
    GPA_REPAIR_PLAN,
    LOW_SCORE_COURSES,
    TERM_GRADES,
    SCORE_RANKING,
    CREDIT_GPA_SUMMARY,
    COURSE_LOOKUP,
    ACADEMIC_SUMMARY,
    GENERAL
}

data class AcademicToolRouteResult(
    val intent: AcademicToolIntent,
    val confidence: Int,
    val toolsUsed: List<String>,
    val directAnswer: String?,
    val llmContext: String
)

class AcademicToolRouter(private val grades: List<GradeEntity>) {
    private val calculator = AcademicCalculator(grades)

    private val enrollmentYear: Int by lazy {
        grades.asSequence()
            .mapNotNull { it.xnm.toIntOrNull() }
            .minOrNull() ?: 2023
    }

    private val sortedTerms: List<Term> by lazy {
        grades.asSequence()
            .filter { it.xnm.isNotBlank() && it.xqm.isNotBlank() }
            .map { Term(it.xnm, it.xqm) }
            .distinct()
            .sorted()
            .toList()
    }


    fun route(query: String): AcademicToolRouteResult {
        val trimmedQuery = query.trim()
        val route = detectIntent(trimmedQuery)
        val relevantGrades = selectRelevantGrades(trimmedQuery, route.intent)
        val directAnswer = buildDirectAnswer(route.intent, route.confidence, trimmedQuery, relevantGrades)
        val context = buildLlmContext(trimmedQuery, route.intent, route.confidence, relevantGrades)

        return AcademicToolRouteResult(
            intent = route.intent,
            confidence = route.confidence,
            toolsUsed = toolsFor(route.intent),
            directAnswer = directAnswer,
            llmContext = context
        )
    }

    private fun detectIntent(query: String): RouteCandidate {
        if (query.isBlank()) return RouteCandidate(AcademicToolIntent.ACADEMIC_SUMMARY, 20)

        val scores = mutableMapOf<AcademicToolIntent, Int>()
        fun add(intent: AcademicToolIntent, points: Int) {
            scores[intent] = (scores[intent] ?: 0) + points
        }

        if (containsAny(query, teacherTerms)) add(AcademicToolIntent.TEACHER_LIST, 35)
        if (containsAny(query, listTerms)) add(AcademicToolIntent.TEACHER_LIST, 15)

        if (containsAny(query, failedTerms)) add(AcademicToolIntent.FAILED_COURSES, 45)
        if (containsAny(query, courseTerms)) add(AcademicToolIntent.FAILED_COURSES, 10)

        if (containsAny(query, gpaTerms)) add(AcademicToolIntent.GPA_REPAIR_PLAN, 35)
        if (containsAny(query, targetTerms)) add(AcademicToolIntent.GPA_REPAIR_PLAN, 20)
        if (containsAny(query, failedTerms)) add(AcademicToolIntent.GPA_REPAIR_PLAN, 10)

        if (containsAny(query, lowScoreTerms)) add(AcademicToolIntent.LOW_SCORE_COURSES, 35)
        if (containsAny(query, courseTerms)) add(AcademicToolIntent.LOW_SCORE_COURSES, 10)

        if (containsAny(query, termTerms) || parseTermQuery(query) != null) add(AcademicToolIntent.TERM_GRADES, 40)
        if (containsAny(query, courseTerms)) add(AcademicToolIntent.TERM_GRADES, 8)

        if (containsAny(query, rankingTerms)) add(AcademicToolIntent.SCORE_RANKING, 35)
        if (containsAny(query, scoreTerms)) add(AcademicToolIntent.SCORE_RANKING, 10)

        if (containsAny(query, summaryTerms) || containsAny(query, creditTerms) || containsAny(query, gpaTerms)) {
            add(AcademicToolIntent.CREDIT_GPA_SUMMARY, 30)
        }

        val matchedCourseScore = grades.maxOfOrNull { grade ->
            relevanceScore(query, grade)
        } ?: 0
        if (matchedCourseScore >= 45) add(AcademicToolIntent.COURSE_LOOKUP, matchedCourseScore)

        if (containsAny(query, summaryTerms)) add(AcademicToolIntent.ACADEMIC_SUMMARY, 25)

        val best = scores.maxByOrNull { it.value }
        return if (best == null || best.value < 20) {
            RouteCandidate(AcademicToolIntent.GENERAL, best?.value ?: 0)
        } else {
            RouteCandidate(best.key, best.value)
        }
    }

    private fun buildDirectAnswer(
        intent: AcademicToolIntent,
        confidence: Int,
        query: String,
        relevantGrades: List<GradeEntity>
    ): String? {
        return when (intent) {
            AcademicToolIntent.TEACHER_LIST -> if (confidence >= 45) buildTeacherListAnswer() else null
            AcademicToolIntent.FAILED_COURSES -> if (confidence >= 40) buildFailedCourseAnswer() else null
            AcademicToolIntent.GPA_REPAIR_PLAN -> if (confidence >= 50) buildGpaRepairPlanAnswer(query) else null
            AcademicToolIntent.LOW_SCORE_COURSES -> if (confidence >= 40) buildLowScoreCourseAnswer(query) else null
            AcademicToolIntent.TERM_GRADES -> if (confidence >= 40) buildTermGradesAnswer(query) else null
            AcademicToolIntent.SCORE_RANKING -> if (confidence >= 40) buildScoreRankingAnswer(query) else null
            AcademicToolIntent.CREDIT_GPA_SUMMARY -> if (confidence >= 35) buildCreditGpaSummaryAnswer() else null
            AcademicToolIntent.COURSE_LOOKUP -> if (confidence >= 70) buildCourseLookupAnswer(relevantGrades) else null
            AcademicToolIntent.ACADEMIC_SUMMARY,
            AcademicToolIntent.GENERAL -> null
        }
    }

    private fun buildLlmContext(
        query: String,
        intent: AcademicToolIntent,
        confidence: Int,
        relevantGrades: List<GradeEntity>
    ): String {
        val (gpa, credits) = calculator.calculateTotalGpa()
        val missing = calculator.calculateMissingCredits()
        val failedCourses = failedBestAttempts()

        return """
            【AcademicToolRouter 路由结果】
            - 识别意图: $intent
            - 置信度: $confidence
            - 已调用工具: ${toolsFor(intent).joinToString("、")}

            【回答规则】
            - 你是校园本地 AI 学业助手，只能基于下面的本地数据库事实回答。
            - 如果用户换说法，例如“挂过科、没过、未通过、不及格、需要重修”，都按“未通过课程”理解。
            - 如果用户问老师、课程、成绩、学分、绩点，应优先引用课程记录。
            - 不要说“当前本地数据没有记录”，除非相关课程记录和派生事实确实没有对应字段。
            - 数学计算以“派生事实”和“工具结果”为准，你只负责解释和整理。

            【用户问题】
            $query

            【总体派生事实】
            - 当前成绩记录数: ${grades.size}
            - 按课程取最高分后的课程数: ${bestAttemptsByCourse().size}
            - 总绩点(GPA): ${gpa.formatDecimal(2)}
            - 已获总学分: ${credits.formatDecimal(1)}
            - 距离毕业还差学分: ${missing.formatDecimal(1)}
            - 学期分布: ${buildTermSummary()}
            - 未通过/可能需要重修课程: ${buildFailedCourseSummary(failedCourses)}
            - 低于70分课程: ${buildLowScoreSummary(maxItems = 16)}
            - 最高分课程: ${buildScoreExtremes(highest = true)}
            - 最低分课程: ${buildScoreExtremes(highest = false)}
            - 已记录任课教师: ${buildTeacherSummary()}

            【相关课程记录】
            ${formatGradeRecords(relevantGrades).ifBlank { "未检索到相关课程记录。" }}
        """.trimIndent()
    }

    private fun toolsFor(intent: AcademicToolIntent): List<String> {
        return when (intent) {
            AcademicToolIntent.TEACHER_LIST -> listOf("Grade.teacher 分组", "Course evidence retrieval")
            AcademicToolIntent.FAILED_COURSES -> listOf("Best-attempt course aggregation", "Pass/fail classifier")
            AcademicToolIntent.GPA_REPAIR_PLAN -> listOf("Best-attempt course aggregation", "Weighted GPA estimator", "Pass/fail classifier")
            AcademicToolIntent.LOW_SCORE_COURSES -> listOf("Score threshold filter", "Best-attempt course aggregation")
            AcademicToolIntent.TERM_GRADES -> listOf("Term parser", "Term grade filter")
            AcademicToolIntent.SCORE_RANKING -> listOf("Score ranking", "Best-attempt course aggregation")
            AcademicToolIntent.CREDIT_GPA_SUMMARY -> listOf("Academic summary calculator", "Credit/GPA aggregator")
            AcademicToolIntent.COURSE_LOOKUP -> listOf("Course fuzzy retrieval")
            AcademicToolIntent.ACADEMIC_SUMMARY -> listOf("Academic summary calculator", "Course evidence retrieval")
            AcademicToolIntent.GENERAL -> listOf("Structured grade RAG")
        }
    }

    private fun buildTeacherListAnswer(): String {
        val teacherCourses = grades
            .asSequence()
            .flatMap { grade -> splitTeachers(grade.teacher).map { teacher -> teacher to grade.courseName } }
            .filter { (teacher, courseName) -> teacher.isNotBlank() && courseName.isNotBlank() }
            .groupBy(
                keySelector = { (teacher, _) -> teacher },
                valueTransform = { (_, courseName) -> courseName }
            )
            .mapValues { (_, courseNames) -> courseNames.distinct().sorted() }

        if (teacherCourses.isEmpty()) return "当前成绩数据里没有记录任课教师信息。"

        val lines = teacherCourses.entries.sortedBy { it.key }.map { (teacher, courseNames) ->
            "- $teacher：${courseNames.joinToString("、")}"
        }
        return "你成绩记录中出现过 ${teacherCourses.size} 位任课教师：\n" + lines.joinToString("\n")
    }

    private fun buildFailedCourseAnswer(): String {
        val failedCourses = failedBestAttempts()
        if (failedCourses.isEmpty()) return "当前成绩数据中没有不及格、未通过或需要重修的课程。"

        val lines = failedCourses.map { grade ->
            "- ${grade.courseName}：${grade.score}，${formatCredit(grade.credit)}学分，绩点 ${grade.gpa.ifBlank { "0" }}"
        }
        return "当前成绩数据中有 ${failedCourses.size} 门课程未通过或需要重修：\n" + lines.joinToString("\n")
    }

    private fun buildGpaRepairPlanAnswer(query: String): String {
        val targetGpa = parseTargetGpa(query) ?: 2.0
        val failedCourses = failedBestAttempts()
        val currentCourses = bestAttemptsByCourse().values.toList()
        val currentCredits = currentCourses.sumOf { parseCredit(it.credit) }
        val passedPoints = currentCourses
            .filter { isPassingScore(it.score) }
            .sumOf { parseGpa(it.gpa) * parseCredit(it.credit) }
        val failedCredits = failedCourses.sumOf { parseCredit(it.credit) }

        if (failedCourses.isEmpty()) {
            val (currentGpa, earnedCredits) = calculateWeightedGpa(currentCourses)
            return "当前没有检测到需要重修的课程。你目前绩点约为 ${currentGpa.formatDecimal(2)}，已获学分 ${earnedCredits.formatDecimal(1)}。"
        }

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

    private fun buildCourseLookupAnswer(relevantGrades: List<GradeEntity>): String? {
        if (relevantGrades.isEmpty()) return null
        val records = relevantGrades.take(8)
        return "我在本地成绩表中找到这些相关课程：\n" + formatGradeRecords(records)
    }

    private fun buildLowScoreCourseAnswer(query: String): String {
        val threshold = parseScoreThreshold(query) ?: 70.0
        val lowScoreCourses = bestAttemptsByCourse().values
            .filter { parseScore(it.score) in 0.0..<threshold }
            .sortedWith(compareBy<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName })

        if (lowScoreCourses.isEmpty()) {
            return "基于本地成绩表，按每门课最高成绩计算，没有低于 ${threshold.formatScore()} 分的课程。"
        }

        val lines = lowScoreCourses.map { grade ->
            "- ${grade.courseName}：${grade.score}，${formatCredit(grade.credit)}学分，绩点 ${grade.gpa.ifBlank { "0" }}"
        }
        return "基于本地成绩表，按每门课最高成绩计算，低于 ${threshold.formatScore()} 分的课程有 ${lowScoreCourses.size} 门：\n" +
            lines.joinToString("\n")
    }

    private fun buildTermGradesAnswer(query: String): String? {
        val termQuery = parseTermQuery(query) ?: return null
        val termGrades = grades
            .filter { grade -> termQuery.matches(grade) }
            .sortedWith(compareBy<GradeEntity> { it.courseName }.thenBy { it.courseId })

        if (termGrades.isEmpty()) {
            return "基于本地成绩表，没有找到 ${termQuery.displayName()} 的成绩记录。"
        }

        val earnedCredits = termGrades
            .filter { isPassingScore(it.score) }
            .sumOf { parseCredit(it.credit) }
        val totalCredits = termGrades.sumOf { parseCredit(it.credit) }
        val totalPoints = termGrades.sumOf { parseGpa(it.gpa) * parseCredit(it.credit) }
        val termGpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
        val failedCount = termGrades.count { !isPassingScore(it.score) }
        val lines = termGrades.map { grade ->
            "- ${grade.courseName}：${grade.score}，${formatCredit(grade.credit)}学分，绩点 ${grade.gpa.ifBlank { "0" }}，教师 ${grade.teacher.ifBlank { "未记录" }}"
        }

        return """
            基于本地成绩表，${termQuery.displayName()} 共 ${termGrades.size} 门课：
            - 本学期加权绩点约 ${termGpa.formatDecimal(2)}
            - 已获学分 ${earnedCredits.formatDecimal(1)} / 记录学分 ${totalCredits.formatDecimal(1)}
            - 未通过课程 ${failedCount} 门

            ${lines.joinToString("\n")}
        """.trimIndent()
    }

    private fun buildScoreRankingAnswer(query: String): String {
        val highest = !containsAny(query, lowestTerms)
        val rankedCourses = bestAttemptsByCourse().values
            .sortedWith(
                if (highest) {
                    compareByDescending<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName }
                } else {
                    compareBy<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName }
                }
            )
            .take(5)

        if (rankedCourses.isEmpty()) return "当前本地成绩表中没有可用于排序的课程成绩。"

        val title = if (highest) "成绩最高的课程" else "成绩最低的课程"
        val lines = rankedCourses.mapIndexed { index, grade ->
            "${index + 1}. ${grade.courseName}：${grade.score}，${formatCredit(grade.credit)}学分，绩点 ${grade.gpa.ifBlank { "0" }}"
        }
        return "基于本地成绩表，按每门课最高成绩计算，$title 是：\n" + lines.joinToString("\n")
    }

    private fun buildCreditGpaSummaryAnswer(): String {
        val bestAttempts = bestAttemptsByCourse().values.toList()
        val totalCredits = bestAttempts.sumOf { parseCredit(it.credit) }
        val earnedCredits = bestAttempts
            .filter { isPassingScore(it.score) }
            .sumOf { parseCredit(it.credit) }
        val totalPoints = bestAttempts.sumOf { parseGpa(it.gpa) * parseCredit(it.credit) }
        val gpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
        val missing = (165.0 - earnedCredits).coerceAtLeast(0.0)
        val failedCourses = failedBestAttempts()
        val lowScoreCount = bestAttempts.count { parseScore(it.score) in 0.0..<70.0 }

        return """
            基于本地成绩表，按每门课最高成绩计算：
            - 已记录课程：${bestAttempts.size} 门
            - 已获学分：${earnedCredits.formatDecimal(1)}
            - 记录总学分：${totalCredits.formatDecimal(1)}
            - 加权绩点约：${gpa.formatDecimal(2)}
            - 距离 165 学分毕业要求还差约：${missing.formatDecimal(1)}
            - 未通过课程：${failedCourses.size} 门
            - 低于 70 分课程：${lowScoreCount} 门
        """.trimIndent()
    }

    private fun selectRelevantGrades(query: String, intent: AcademicToolIntent): List<GradeEntity> {
        val bestAttempts = bestAttemptsByCourse().values.toList()
        if (bestAttempts.isEmpty()) return emptyList()
        val scoreThreshold = parseScoreThreshold(query) ?: 70.0
        val termQuery = parseTermQuery(query)
        val wantsLowest = containsAny(query, lowestTerms)

        val maxRecords = when (intent) {
            AcademicToolIntent.TEACHER_LIST -> 64
            AcademicToolIntent.FAILED_COURSES -> 32
            AcademicToolIntent.GPA_REPAIR_PLAN -> 40
            AcademicToolIntent.LOW_SCORE_COURSES -> 40
            AcademicToolIntent.TERM_GRADES -> 64
            AcademicToolIntent.SCORE_RANKING -> 32
            AcademicToolIntent.CREDIT_GPA_SUMMARY -> 48
            AcademicToolIntent.COURSE_LOOKUP -> 24
            AcademicToolIntent.ACADEMIC_SUMMARY,
            AcademicToolIntent.GENERAL -> 48
        }

        val primary = when (intent) {
            AcademicToolIntent.FAILED_COURSES,
            AcademicToolIntent.GPA_REPAIR_PLAN -> failedBestAttempts()
            AcademicToolIntent.TEACHER_LIST -> bestAttempts.filter { it.teacher.isNotBlank() }
            AcademicToolIntent.LOW_SCORE_COURSES -> bestAttempts
                .filter { parseScore(it.score) in 0.0..<scoreThreshold }
                .sortedWith(compareBy<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName })
            AcademicToolIntent.TERM_GRADES -> if (termQuery != null) {
                grades.filter { termQuery.matches(it) }
            } else {
                emptyList()
            }
            AcademicToolIntent.SCORE_RANKING -> bestAttempts.sortedWith(
                if (wantsLowest) {
                    compareBy<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName }
                } else {
                    compareByDescending<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName }
                }
            )
            AcademicToolIntent.CREDIT_GPA_SUMMARY,
            AcademicToolIntent.ACADEMIC_SUMMARY -> bestAttempts
            else -> emptyList()
        }

        val scored = bestAttempts
            .map { grade -> grade to relevanceScore(query, grade) }
            .sortedWith(
                compareByDescending<Pair<GradeEntity, Int>> { it.second }
                    .thenBy { it.first.xnm }
                    .thenBy { it.first.xqm }
                    .thenBy { it.first.courseName }
            )
            .map { it.first }

        return (primary + scored)
            .distinctBy { "${it.courseName}|${it.xnm}|${it.xqm}" }
            .take(maxRecords)
            .ifEmpty { bestAttempts.sortedBy { it.courseName }.take(maxRecords) }
    }

    private fun formatGradeRecords(records: List<GradeEntity>): String {
        return records.joinToString("\n") { grade ->
            val teacher = grade.teacher.ifBlank { "未记录" }
            val courseType = grade.courseType.ifBlank { "未记录" }
            val examNature = grade.examNature.ifBlank { "未记录" }
            "- [${grade.xnm}-${grade.xqm}] ${grade.courseName}｜成绩:${grade.score}｜学分:${formatCredit(grade.credit)}｜绩点:${grade.gpa.ifBlank { "0" }}｜教师:$teacher｜课程性质:$courseType｜考试性质:$examNature"
        }
    }

    private fun relevanceScore(query: String, grade: GradeEntity): Int {
        if (query.isBlank()) return 1

        val normalizedQuery = normalizeForSearch(query)
        val recordText = normalizeForSearch(
            listOf(
                grade.courseName,
                grade.teacher,
                grade.score,
                grade.credit,
                grade.gpa,
                grade.courseType,
                grade.examType,
                grade.examNature,
                grade.xnm,
                grade.xqm
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

        if (containsAny(query, failedTerms) && !isPassingScore(grade.score)) score += 100
        if (containsAny(query, gpaTerms) && grade.gpa.isNotBlank()) score += 12
        if ((query.contains("学分") || query.contains("多少分")) && grade.credit.isNotBlank()) score += 8
        if (containsAny(query, teacherTerms) && grade.teacher.isNotBlank()) score += 10

        return score
    }

    private fun bestAttemptsByCourse(): Map<String, GradeEntity> {
        return grades
            .filter { it.courseName.isNotBlank() }
            .groupBy { it.courseName }
            .mapValues { (_, attempts) -> attempts.maxBy { parseScore(it.score) } }
    }

    private fun failedBestAttempts(): List<GradeEntity> {
        return bestAttemptsByCourse().values
            .filter { !isPassingScore(it.score) }
            .sortedBy { it.courseName }
    }

    private fun buildFailedCourseSummary(failedCourses: List<GradeEntity>): String {
        if (failedCourses.isEmpty()) return "无"
        return failedCourses.joinToString("；") { grade ->
            "${grade.courseName}(${grade.score}，${formatCredit(grade.credit)}学分)"
        }
    }

    private fun buildLowScoreSummary(maxItems: Int): String {
        val lowScoreCourses = bestAttemptsByCourse().values
            .filter { parseScore(it.score) in 0.0..<70.0 }
            .sortedWith(compareBy<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName })
            .take(maxItems)

        if (lowScoreCourses.isEmpty()) return "无"
        return lowScoreCourses.joinToString("；") { grade ->
            "${grade.courseName}(${grade.score})"
        }
    }

    private fun buildTermSummary(): String {
        if (sortedTerms.isEmpty()) return "无学期记录"
        return sortedTerms.joinToString("，") { term ->
            val termGrades = grades.filter { it.xnm == term.xnm && it.xqm == term.xqm }
            val displayName = term.let { "${it.xnm}学年${if (it.xqm == "3") "第一学期" else "第二学期"}" }
            "$displayName(${termGrades.size}门课)"
        }
    }

    private fun buildScoreExtremes(highest: Boolean): String {
        val attempts = bestAttemptsByCourse().values
        if (attempts.isEmpty()) return "无"
        val extremeScore = if (highest) {
            attempts.maxOfOrNull { parseScore(it.score) }
        } else {
            attempts.minOfOrNull { parseScore(it.score) }
        } ?: return "无"

        val extremeGrades = attempts.filter { parseScore(it.score) == extremeScore }
        return extremeGrades.joinToString("；") { grade ->
            "${grade.courseName}(${grade.score})"
        }
    }

    private fun buildTeacherSummary(): String {
        val teachers = grades
            .flatMap { splitTeachers(it.teacher) }
            .distinct()
            .sorted()
        return if (teachers.isEmpty()) "无教师字段记录" else teachers.joinToString("、")
    }

    private fun splitTeachers(rawTeacher: String): List<String> {
        return rawTeacher
            .split("、", ",", "，", ";", "；", "/", "／")
            .map { it.trim().removeSuffix("老师").trim() }
            .filter { it.isNotBlank() && it != "-" && it != "无" }
    }

    private fun calculateWeightedGpa(courseGrades: List<GradeEntity>): Pair<Double, Double> {
        val totalCredits = courseGrades.sumOf { parseCredit(it.credit) }
        val totalPoints = courseGrades.sumOf { parseGpa(it.gpa) * parseCredit(it.credit) }
        return (if (totalCredits > 0) totalPoints / totalCredits else 0.0) to totalCredits
    }

    private fun isPassingScore(score: String): Boolean {
        val numericScore = score.toDoubleOrNull()
        if (numericScore != null) return numericScore >= 60.0
        return score in listOf("优秀", "良好", "中等", "及格", "通过", "合格")
    }

    private fun parseScore(score: String): Double {
        return score.toDoubleOrNull() ?: when (score) {
            "优秀" -> 95.0
            "良好" -> 85.0
            "中等" -> 75.0
            "及格", "通过", "合格" -> 65.0
            "不及格", "未通过", "不合格", "缓考", "缺考" -> 0.0
            else -> 0.0
        }
    }

    private fun parseCredit(credit: String): Double = credit.toDoubleOrNull() ?: 0.0

    private fun parseGpa(gpa: String): Double = gpa.toDoubleOrNull() ?: 0.0

    private fun parseTargetGpa(query: String): Double? {
        if (!containsAny(query, gpaTerms)) return null
        return Regex("""([0-9]+(?:[.。][0-9]+)?)""")
            .findAll(query)
            .mapNotNull { it.value.replace("。", ".").toDoubleOrNull() }
            .firstOrNull { it in 0.0..5.0 }
    }

    private fun estimateScoreFromGpa(gpa: Double): String {
        return when {
            gpa <= 0.0 -> "60 分左右即可"
            gpa <= 1.0 -> "60-64 分"
            gpa <= 1.5 -> "65-69 分"
            gpa <= 2.0 -> "70-74 分"
            gpa <= 2.5 -> "75-79 分"
            gpa <= 3.0 -> "80-84 分"
            gpa <= 3.5 -> "85-89 分"
            gpa <= 4.0 -> "90 分以上"
            else -> "超过常规满绩点范围"
        }
    }

    private fun difficultyLabel(gpa: Double): String {
        return when {
            gpa <= 1.0 -> "较低"
            gpa <= 2.0 -> "适中"
            gpa <= 3.0 -> "偏高"
            gpa <= 4.0 -> "较高"
            else -> "很高，单靠这些重修课可能不够"
        }
    }

    private fun formatCredit(credit: String): String {
        val numericCredit = parseCredit(credit)
        return if (numericCredit % 1.0 == 0.0) numericCredit.toInt().toString() else numericCredit.formatDecimal(1)
    }

    private fun searchTerms(normalizedQuery: String): List<String> {
        return Regex("""[a-z0-9]+|[\u4e00-\u9fa5]{2,}""")
            .findAll(normalizedQuery)
            .map { it.value }
            .flatMap { term ->
                if (term.length <= 8) listOf(term) else term.windowed(4, 2, partialWindows = true).filter { it.length >= 2 }
            }
            .distinct()
            .toList()
    }

    private fun chineseBigrams(text: String): List<String> {
        val chineseOnly = text.filter { it in '\u4e00'..'\u9fa5' }
        return if (chineseOnly.length < 2) emptyList() else chineseOnly.windowed(2).distinct()
    }

    private fun normalizeForSearch(text: String): String {
        return text
            .lowercase()
            .replace(" ", "")
            .replace("　", "")
            .replace("，", ",")
            .replace("。", ".")
            .replace("（", "(")
            .replace("）", ")")
    }

    private fun containsAny(text: String, terms: List<String>): Boolean {
        return terms.any { text.contains(it, ignoreCase = true) }
    }

    private fun Double.formatDecimal(decimals: Int): String {
        var factor = 1.0
        repeat(decimals) { factor *= 10.0 }
        val rounded = kotlin.math.round(this * factor) / factor
        val parts = rounded.toString().split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else ""
        return if (decimals > 0) "$intPart.${decPart.padEnd(decimals, '0').take(decimals)}" else intPart
    }

    private fun parseScoreThreshold(query: String): Double? {
        val pattern = Regex("""(?:低于|小于|不到|分[数<]|低于分)\s*([0-9]+)""")
        val match = pattern.find(query)
        if (match != null) {
            return match.groupValues[1].toDoubleOrNull()
        }
        return null
    }

    private fun parseTermQuery(query: String): TermQuery? {
        // 1. Relative Terms
        if (query.contains("上学期") || query.contains("前一学期") || query.contains("上个学期")) {
            val resolved = sortedTerms.getOrNull(sortedTerms.size - 2) ?: sortedTerms.lastOrNull()
            return RelativeTermQuery("上学期", resolved)
        }
        if (query.contains("本学期") || query.contains("这学期") || query.contains("最近学期") || query.contains("最新学期") || query.contains("最近一个学期")) {
            val resolved = sortedTerms.lastOrNull()
            return RelativeTermQuery("最近学期", resolved)
        }

        // 2. Academic Years with Semester (e.g. 2024-2025-1, 2024-2025-3, 2024-2025学年第一学期)
        val yearRangePattern = Regex("""(20[0-9]{2})[-~和/及与至]*(20[0-9]{2})""")
        val rangeMatch = yearRangePattern.find(query)
        if (rangeMatch != null) {
            val xnm = rangeMatch.groupValues[1]
            val restOfQuery = query.substring(rangeMatch.range.last + 1)
            val termVal = when {
                restOfQuery.contains("一") || restOfQuery.contains("1") || restOfQuery.contains("3") || restOfQuery.contains("上") -> "3"
                restOfQuery.contains("二") || restOfQuery.contains("2") || restOfQuery.contains("12") || restOfQuery.contains("下") -> "12"
                else -> null
            }
            return if (termVal != null) {
                ExactTermQuery(xnm, termVal)
            } else {
                YearOnlyTermQuery(xnm)
            }
        }

        // 3. Single Years (e.g. 2024年第一学期, 2024秋)
        val singleYearPattern = Regex("""(20[0-9]{2})""")
        val yearMatch = singleYearPattern.find(query)
        if (yearMatch != null) {
            val xnm = yearMatch.groupValues[1]
            val restOfQuery = query.replace(xnm, "")
            val termVal = when {
                restOfQuery.contains("一") || restOfQuery.contains("1") || restOfQuery.contains("3") || restOfQuery.contains("上") || restOfQuery.contains("秋") -> "3"
                restOfQuery.contains("二") || restOfQuery.contains("2") || restOfQuery.contains("12") || restOfQuery.contains("下") || restOfQuery.contains("春") -> "12"
                else -> null
            }
            return if (termVal != null) {
                ExactTermQuery(xnm, termVal)
            } else {
                YearOnlyTermQuery(xnm)
            }
        }

        // 4. Grade Levels (大一, 大二, 大三, 大四, 大五)
        val gradePattern = Regex("""大([一二三四五12345])""")
        val gradeMatch = gradePattern.find(query)
        if (gradeMatch != null) {
            val gradeChar = gradeMatch.groupValues[1]
            val gradeNum = when (gradeChar) {
                "一", "1" -> 1
                "二", "2" -> 2
                "三", "3" -> 3
                "四", "4" -> 4
                "五", "5" -> 5
                else -> 1
            }
            val gradeName = "大$gradeChar"
            val termVal = when {
                query.contains("上") || query.contains("一") || query.contains("1") || query.contains("3") -> 1
                query.contains("下") || query.contains("二") || query.contains("2") || query.contains("12") -> 2
                else -> null
            }
            return GradeTermQuery(gradeName, termVal, gradeNum, enrollmentYear)
        }

        return null
    }

    private fun Double.formatScore(): String {
        return if (this % 1.0 == 0.0) this.toInt().toString() else this.formatDecimal(1)
    }

    private data class RouteCandidate(
        val intent: AcademicToolIntent,
        val confidence: Int
    )

    private companion object {
        val teacherTerms = listOf("老师", "教师", "任课", "授课", "教过", "导师")
        val listTerms = listOf("哪些", "谁", "列表", "上过", "教过", "都有", "全部", "所有")
        val failedTerms = listOf("挂科", "挂过", "挂了", "没过", "不及格", "未通过", "不合格", "低于60", "低于 60", "补考", "重修")
        val courseTerms = listOf("课程", "科目", "课", "成绩", "分数")
        val gpaTerms = listOf("绩点", "GPA", "gpa", "学位绩点")
        val targetTerms = listOf("达到", "达标", "到", "多少分", "考多少", "提升", "提高")
        val summaryTerms = listOf("总结", "概况", "情况", "分析", "怎么样", "毕业", "学分")
        val lowScoreTerms = listOf("低分", "考得差", "分低", "差的课", "垫底", "最差", "低成绩", "分数较低", "成绩较低")
        val termTerms = listOf("学期", "学年", "上学期", "下学期", "第一学期", "第二学期", "大一", "大二", "大三", "大四", "大五", "最新", "最近", "秋", "春")
        val rankingTerms = listOf("最高", "最低", "排行", "排名", "第一", "最好", "最差", "垫底", "前几", "后几", "前五", "后五", "最棒", "最优秀")
        val scoreTerms = listOf("分数", "成绩", "分")
        val lowestTerms = listOf("最低", "最差", "垫底", "最差的", "倒数第一", "分数最低", "成绩最低")
        val creditTerms = listOf("学分")
    }
}

private interface TermQuery {
    fun matches(grade: GradeEntity): Boolean
    fun displayName(): String
}

private class ExactTermQuery(val xnm: String, val xqm: String) : TermQuery {
    override fun matches(grade: GradeEntity): Boolean = grade.xnm == xnm && grade.xqm == xqm
    override fun displayName(): String = "${xnm}学年${if (xqm == "3") "第一学期" else "第二学期"}"
}

private class YearOnlyTermQuery(val xnm: String) : TermQuery {
    override fun matches(grade: GradeEntity): Boolean = grade.xnm == xnm
    override fun displayName(): String = "$xnm-${(xnm.toIntOrNull() ?: 0) + 1} 学年"
}

private class GradeTermQuery(val gradeName: String, val termVal: Int?, val gradeNum: Int, val enrollmentYear: Int) : TermQuery {
    override fun matches(grade: GradeEntity): Boolean {
        val targetXnm = (enrollmentYear + (gradeNum - 1)).toString()
        val xnmMatch = grade.xnm == targetXnm
        val xqmMatch = when (termVal) {
            1 -> grade.xqm == "3"
            2 -> grade.xqm == "12"
            else -> true
        }
        return xnmMatch && xqmMatch
    }
    override fun displayName(): String = "$gradeName${when(termVal) { 1 -> "第一学期"; 2 -> "第二学期"; else -> "" }}"
}

private class RelativeTermQuery(val relativeType: String, val resolvedTerm: Term?) : TermQuery {
    override fun matches(grade: GradeEntity): Boolean {
        return resolvedTerm != null && grade.xnm == resolvedTerm.xnm && grade.xqm == resolvedTerm.xqm
    }
    override fun displayName(): String = "$relativeType（${resolvedTerm?.let { "${it.xnm}学年${if (it.xqm == "3") "第一学期" else "第二学期"}" } ?: "未知学期"}）"
}

private data class Term(val xnm: String, val xqm: String) : Comparable<Term> {
    val yearInt = xnm.toIntOrNull() ?: 0
    val termVal = if (xqm == "3") 1 else 2

    override fun compareTo(other: Term): Int {
        if (this.yearInt != other.yearInt) {
            return this.yearInt.compareTo(other.yearInt)
        }
        return this.termVal.compareTo(other.termVal)
    }
}
