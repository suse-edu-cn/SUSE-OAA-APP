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

/**
 * 学业问答的意图路由。
 *
 * 职责只有三件事：识别意图、挑选相关成绩记录、决定是直接回答还是交给大模型。
 * 具体分工：
 * - 文本解析与相关度打分 → AcademicQueryParser.kt
 * - 成绩数值计算 → GradeMath.kt
 * - 回答文案生成 → [AcademicAnswerBuilder]
 *
 * 置信度足够高时给出 `directAnswer` 直接回用户；否则只提供 `llmContext`，
 * 由大模型基于本地事实作答，避免它凭空编造成绩。
 */
class AcademicToolRouter(private val grades: List<GradeEntity>) {

    private val calculator = AcademicCalculator(grades)
    private val termIndex = TermIndex(grades)
    private val answers = AcademicAnswerBuilder(grades, termIndex)

    fun route(query: String): AcademicToolRouteResult {
        val trimmedQuery = query.trim()
        val route = detectIntent(trimmedQuery)
        val relevantGrades = selectRelevantGrades(trimmedQuery, route.intent)

        return AcademicToolRouteResult(
            intent = route.intent,
            confidence = route.confidence,
            toolsUsed = toolsFor(route.intent),
            directAnswer = buildDirectAnswer(route.intent, route.confidence, trimmedQuery, relevantGrades),
            llmContext = buildLlmContext(trimmedQuery, route.intent, route.confidence, relevantGrades)
        )
    }

    // ==================== 意图识别 ====================

    /**
     * 按关键词打分选出最可能的意图。
     * 总分低于 [MIN_CONFIDENCE] 视为无法判断，退回 GENERAL 走通用检索。
     */
    private fun detectIntent(query: String): RouteCandidate {
        if (query.isBlank()) return RouteCandidate(AcademicToolIntent.ACADEMIC_SUMMARY, 20)

        val scores = mutableMapOf<AcademicToolIntent, Int>()
        fun add(intent: AcademicToolIntent, points: Int) {
            scores[intent] = (scores[intent] ?: 0) + points
        }

        if (containsAny(query, AcademicTerms.teacher)) add(AcademicToolIntent.TEACHER_LIST, 35)
        if (containsAny(query, AcademicTerms.list)) add(AcademicToolIntent.TEACHER_LIST, 15)

        if (containsAny(query, AcademicTerms.failed)) add(AcademicToolIntent.FAILED_COURSES, 45)
        if (containsAny(query, AcademicTerms.course)) add(AcademicToolIntent.FAILED_COURSES, 10)

        if (containsAny(query, AcademicTerms.gpa)) add(AcademicToolIntent.GPA_REPAIR_PLAN, 35)
        if (containsAny(query, AcademicTerms.target)) add(AcademicToolIntent.GPA_REPAIR_PLAN, 20)
        if (containsAny(query, AcademicTerms.failed)) add(AcademicToolIntent.GPA_REPAIR_PLAN, 10)

        if (containsAny(query, AcademicTerms.lowScore)) add(AcademicToolIntent.LOW_SCORE_COURSES, 35)
        if (containsAny(query, AcademicTerms.course)) add(AcademicToolIntent.LOW_SCORE_COURSES, 10)

        if (containsAny(query, AcademicTerms.term) || parseTermQuery(query, termIndex) != null) {
            add(AcademicToolIntent.TERM_GRADES, 40)
        }
        if (containsAny(query, AcademicTerms.course)) add(AcademicToolIntent.TERM_GRADES, 8)

        if (containsAny(query, AcademicTerms.ranking)) add(AcademicToolIntent.SCORE_RANKING, 35)
        if (containsAny(query, AcademicTerms.score)) add(AcademicToolIntent.SCORE_RANKING, 10)

        if (containsAny(query, AcademicTerms.summary) ||
            containsAny(query, AcademicTerms.credit) ||
            containsAny(query, AcademicTerms.gpa)
        ) {
            add(AcademicToolIntent.CREDIT_GPA_SUMMARY, 30)
        }

        // 查询里直接点名了某门课时，课程检索的分数会自然压过其它意图
        val matchedCourseScore = grades.maxOfOrNull { relevanceScore(query, it) } ?: 0
        if (matchedCourseScore >= COURSE_MATCH_THRESHOLD) {
            add(AcademicToolIntent.COURSE_LOOKUP, matchedCourseScore)
        }

        if (containsAny(query, AcademicTerms.summary)) add(AcademicToolIntent.ACADEMIC_SUMMARY, 25)

        val best = scores.maxByOrNull { it.value }
        return if (best == null || best.value < MIN_CONFIDENCE) {
            RouteCandidate(AcademicToolIntent.GENERAL, best?.value ?: 0)
        } else {
            RouteCandidate(best.key, best.value)
        }
    }

    /** 只有置信度达到各意图的阈值时才直接作答，否则交给大模型 */
    private fun buildDirectAnswer(
        intent: AcademicToolIntent,
        confidence: Int,
        query: String,
        relevantGrades: List<GradeEntity>
    ): String? = when (intent) {
        AcademicToolIntent.TEACHER_LIST ->
            if (confidence >= 45) answers.answerTeacherList() else null
        AcademicToolIntent.FAILED_COURSES ->
            if (confidence >= 40) answers.answerFailedCourses() else null
        AcademicToolIntent.GPA_REPAIR_PLAN ->
            if (confidence >= 50) answers.answerGpaRepairPlan(query) else null
        AcademicToolIntent.LOW_SCORE_COURSES ->
            if (confidence >= 40) answers.answerLowScoreCourses(query) else null
        AcademicToolIntent.TERM_GRADES ->
            if (confidence >= 40) answers.answerTermGrades(query) else null
        AcademicToolIntent.SCORE_RANKING ->
            if (confidence >= 40) answers.answerScoreRanking(query) else null
        AcademicToolIntent.CREDIT_GPA_SUMMARY ->
            if (confidence >= 35) answers.answerCreditGpaSummary() else null
        AcademicToolIntent.COURSE_LOOKUP ->
            if (confidence >= 70) answers.answerCourseLookup(relevantGrades) else null
        AcademicToolIntent.ACADEMIC_SUMMARY,
        AcademicToolIntent.GENERAL -> null
    }

    // ==================== 相关记录检索 ====================

    /**
     * 挑出要喂给模型的成绩记录：先按意图取一批「主记录」，
     * 再按查询相关度补齐，最后去重截断到该意图的记录上限。
     */
    private fun selectRelevantGrades(
        query: String,
        intent: AcademicToolIntent
    ): List<GradeEntity> {
        val bestAttempts = grades.bestAttemptsByCourse().values.toList()
        if (bestAttempts.isEmpty()) return emptyList()

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

        val primary = primaryGradesFor(query, intent, bestAttempts)
        val byRelevance = bestAttempts
            .map { grade -> grade to relevanceScore(query, grade) }
            .sortedWith(
                compareByDescending<Pair<GradeEntity, Int>> { it.second }
                    .thenBy { it.first.xnm }
                    .thenBy { it.first.xqm }
                    .thenBy { it.first.courseName }
            )
            .map { it.first }

        return (primary + byRelevance)
            .distinctBy { "${it.courseName}|${it.xnm}|${it.xqm}" }
            .take(maxRecords)
            .ifEmpty { bestAttempts.sortedBy { it.courseName }.take(maxRecords) }
    }

    private fun primaryGradesFor(
        query: String,
        intent: AcademicToolIntent,
        bestAttempts: List<GradeEntity>
    ): List<GradeEntity> = when (intent) {
        AcademicToolIntent.FAILED_COURSES,
        AcademicToolIntent.GPA_REPAIR_PLAN -> grades.failedBestAttempts()

        AcademicToolIntent.TEACHER_LIST -> bestAttempts.filter { it.teacher.isNotBlank() }

        AcademicToolIntent.LOW_SCORE_COURSES -> {
            val threshold = parseScoreThreshold(query) ?: DEFAULT_LOW_SCORE_THRESHOLD
            bestAttempts
                .filter { parseScore(it.score) in 0.0..<threshold }
                .sortedWith(compareBy<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName })
        }

        AcademicToolIntent.TERM_GRADES ->
            parseTermQuery(query, termIndex)
                ?.let { termQuery -> grades.filter { termQuery.matches(it) } }
                .orEmpty()

        AcademicToolIntent.SCORE_RANKING -> {
            val wantsLowest = containsAny(query, AcademicTerms.lowest)
            bestAttempts.sortedWith(
                if (wantsLowest) {
                    compareBy<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName }
                } else {
                    compareByDescending<GradeEntity> { parseScore(it.score) }.thenBy { it.courseName }
                }
            )
        }

        AcademicToolIntent.CREDIT_GPA_SUMMARY,
        AcademicToolIntent.ACADEMIC_SUMMARY -> bestAttempts

        else -> emptyList()
    }

    // ==================== 大模型上下文 ====================

    private fun toolsFor(intent: AcademicToolIntent): List<String> = when (intent) {
        AcademicToolIntent.TEACHER_LIST ->
            listOf("Grade.teacher 分组", "Course evidence retrieval")
        AcademicToolIntent.FAILED_COURSES ->
            listOf("Best-attempt course aggregation", "Pass/fail classifier")
        AcademicToolIntent.GPA_REPAIR_PLAN ->
            listOf("Best-attempt course aggregation", "Weighted GPA estimator", "Pass/fail classifier")
        AcademicToolIntent.LOW_SCORE_COURSES ->
            listOf("Score threshold filter", "Best-attempt course aggregation")
        AcademicToolIntent.TERM_GRADES ->
            listOf("Term parser", "Term grade filter")
        AcademicToolIntent.SCORE_RANKING ->
            listOf("Score ranking", "Best-attempt course aggregation")
        AcademicToolIntent.CREDIT_GPA_SUMMARY ->
            listOf("Academic summary calculator", "Credit/GPA aggregator")
        AcademicToolIntent.COURSE_LOOKUP ->
            listOf("Course fuzzy retrieval")
        AcademicToolIntent.ACADEMIC_SUMMARY ->
            listOf("Academic summary calculator", "Course evidence retrieval")
        AcademicToolIntent.GENERAL ->
            listOf("Structured grade RAG")
    }

    private fun buildLlmContext(
        query: String,
        intent: AcademicToolIntent,
        confidence: Int,
        relevantGrades: List<GradeEntity>
    ): String {
        val (gpa, credits) = calculator.calculateTotalGpa()
        val missing = calculator.calculateMissingCredits()

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
            - 按课程取最高分后的课程数: ${grades.bestAttemptsByCourse().size}
            - 总绩点(GPA): ${gpa.formatDecimal(2)}
            - 已获总学分: ${credits.formatDecimal(1)}
            - 距离毕业还差学分: ${missing.formatDecimal(1)}
            - 学期分布: ${answers.summaryTerms()}
            - 未通过/可能需要重修课程: ${answers.summaryFailedCourses(grades.failedBestAttempts())}
            - 低于70分课程: ${answers.summaryLowScores(maxItems = 16)}
            - 最高分课程: ${answers.summaryScoreExtremes(highest = true)}
            - 最低分课程: ${answers.summaryScoreExtremes(highest = false)}
            - 已记录任课教师: ${answers.summaryTeachers()}

            【相关课程记录】
            ${answers.formatGradeRecords(relevantGrades).ifBlank { "未检索到相关课程记录。" }}
        """.trimIndent()
    }

    private data class RouteCandidate(
        val intent: AcademicToolIntent,
        val confidence: Int
    )

    private companion object {
        const val MIN_CONFIDENCE = 20
        const val COURSE_MATCH_THRESHOLD = 45
        const val DEFAULT_LOW_SCORE_THRESHOLD = 70.0
    }
}
