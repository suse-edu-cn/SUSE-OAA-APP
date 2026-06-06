package com.suseoaa.projectoaa.shared.domain.engine

import com.suseoaa.projectoaa.shared.data.repository.GradeEntity

/**
 * 原生 Kotlin 数学分析工具链
 * 用于替代本地 LLM 薄弱的浮点计算能力
 */
class AcademicCalculator(private val grades: List<GradeEntity>) {

    // 默认毕业学分要求（可根据专业配置）
    private val TARGET_GRADUATION_CREDITS = 165.0

    /**
     * 计算总绩点 (GPA) 和已获学分
     */
    fun calculateTotalGpa(): Pair<Double, Double> {
        var totalCredit = 0.0
        var totalPoints = 0.0

        grades.forEach { grade ->
            val credit = grade.credit.toDoubleOrNull() ?: 0.0
            val gpa = grade.gpa.toDoubleOrNull() ?: 0.0
            
            // 只有及格的课程才算学分
            val score = grade.score.toDoubleOrNull()
            val isPass = if (score != null) {
                score >= 60.0
            } else {
                grade.score in listOf("优秀", "良好", "中等", "及格", "通过")
            }

            if (isPass) {
                totalCredit += credit
            }
            // 绩点是按所有参与计算的课程加权
            if (credit > 0) {
                totalPoints += gpa * credit
            }
        }

        val finalGpa = if (totalCredit > 0) totalPoints / totalCredit else 0.0
        return Pair(finalGpa, totalCredit)
    }

    /**
     * 分析挂科重修名单
     */
    fun analyzeFailedCourses(): List<GradeEntity> {
        val failed = mutableListOf<GradeEntity>()
        // 获取每个课程的最高分（有的学生考了多次）
        val courseMap = mutableMapOf<String, GradeEntity>()
        
        grades.forEach { grade ->
            val courseName = grade.courseName
            val currentScore = parseScore(grade.score)
            val existingScore = parseScore(courseMap[courseName]?.score)
            
            if (currentScore > existingScore) {
                courseMap[courseName] = grade
            }
        }

        courseMap.values.forEach { grade ->
            if (parseScore(grade.score) < 60.0 && !isPassText(grade.score)) {
                failed.add(grade)
            }
        }
        return failed
    }

    /**
     * 计算毕业还差多少学分
     */
    fun calculateMissingCredits(target: Double = TARGET_GRADUATION_CREDITS): Double {
        val (_, earned) = calculateTotalGpa()
        return if (earned >= target) 0.0 else target - earned
    }

    private fun parseScore(cj: String?): Double {
        if (cj == null) return 0.0
        return cj.toDoubleOrNull() ?: when (cj) {
            "优秀" -> 95.0
            "良好" -> 85.0
            "中等" -> 75.0
            "及格", "通过" -> 65.0
            "不及格", "未通过", "缓考", "缺考" -> 0.0
            else -> 0.0
        }
    }
    
    private fun isPassText(cj: String?): Boolean {
        return cj in listOf("优秀", "良好", "中等", "及格", "通过")
    }
}
