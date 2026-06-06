package com.suseoaa.projectoaa.shared.domain.engine

import com.suseoaa.projectoaa.shared.data.repository.GradeEntity

/**
 * 局部 Tool Calling 引擎
 * 由于端侧小模型（如 Gemma 2B/4B）通常不具备强大的 Function Calling 和浮点计算能力，
 * 我们采用预计算上下文注入（Pre-computed Context Injection）的策略。
 * 
 * 在用户发起提问时，引擎先使用 Kotlin 原生代码分析数据库，提取精确指标，
 * 组装成系统提示词（System Prompt），再交给小模型进行自然语言润色和归纳。
 */
class AiToolEngine(private val grades: List<GradeEntity>) {
    private val calculator = AcademicCalculator(grades)

    /**
     * 生成包含绝对正确数据的上下文，供 LLM 消费
     */
    fun buildAcademicContext(): String {
        val (gpa, credits) = calculator.calculateTotalGpa()
        val missing = calculator.calculateMissingCredits()
        val failed = calculator.analyzeFailedCourses()
        
        val failedStr = if (failed.isEmpty()) {
            "无挂科记录，表现完美。"
        } else {
            "有 ${failed.size} 门课程需要重修：" + failed.joinToString("、") { it.courseName }
        }

        return """
            【学生学业真实数据（请基于此数据进行回答，绝对不要自己进行数学计算）】
            - 总绩点 (GPA): ${gpa.formatDecimal(2)}
            - 已获总学分: $credits
            - 距离毕业还差学分: $missing
            - 挂科及重修情况: $failedStr
            - 包含的课程总数: ${grades.size} 门
        """.trimIndent()
    }

    /**
     * 辅助扩展：格式化浮点数
     */
    private fun Double.formatDecimal(decimals: Int): String {
        var factor = 1.0
        repeat(decimals) { factor *= 10.0 }
        val rounded = kotlin.math.round(this * factor) / factor
        val parts = rounded.toString().split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else ""
        return if (decimals > 0) {
            "$intPart.${decPart.padEnd(decimals, '0').take(decimals)}"
        } else {
            intPart
        }
    }
}
