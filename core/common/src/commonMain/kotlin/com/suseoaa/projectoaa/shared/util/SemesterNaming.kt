package com.suseoaa.projectoaa.shared.util

/**
 * 学期的年级化命名。
 *
 * 教务系统给的是「学年码 + 学期码」（如 xnm=2024, xqm=3），直译出来是
 * 「2024-2025学年 第1学期」——学生要在脑子里换算才知道那是自己大几。
 * 这里按入学年份折算成「大一上学期」这种说法。
 *
 * 两种长度：[full] 用于课表的滚动选择器（空间充裕），[short] 用于成绩、
 * 绩点这类筛选标签（空间紧张）。
 *
 * 入学年份取自年级代码 njdmId。判定不出年级时（没有 njdmId、或年份超出
 * 学制范围）一律回落到原来的「学年 + 第几学期」写法，不猜。
 */
object SemesterNaming {

    /** 最长按五年制处理；超出范围说明数据异常或是研究生，回落到学年写法。 */
    private val GRADE_NAMES = listOf("大一", "大二", "大三", "大四", "大五")

    /** 学期码 -> 上/下。其它学期码（如短学期 16）没有通行叫法，返回 null。 */
    private fun halfName(xqm: String): String? = when (xqm) {
        "3" -> "上"
        "12" -> "下"
        else -> null
    }

    /** 学期码 -> 第几学期，用于回落文案。 */
    private fun ordinal(xqm: String): String = when (xqm) {
        "3" -> "1"
        "12" -> "2"
        "16" -> "3"
        else -> xqm
    }

    /**
     * 折算年级，1 表示大一。
     * @param enrollmentYear 入学年份，通常等于年级代码
     * @param academicYear 该学期所属学年的起始年份
     */
    fun gradeName(enrollmentYear: Int?, academicYear: Int): String? {
        if (enrollmentYear == null) return null
        val index = academicYear - enrollmentYear
        return GRADE_NAMES.getOrNull(index)
    }

    /** 回落文案：「2024-2025学年 第1学期」。 */
    private fun fallback(xnm: String, xqm: String): String {
        val year = xnm.take(4).toIntOrNull() ?: return "第${ordinal(xqm)}学期"
        return "$year-${year + 1}学年 第${ordinal(xqm)}学期"
    }

    /** 完整写法：「大一上学期」。课表的滚动选择器用这个。 */
    fun full(enrollmentYear: Int?, xnm: String, xqm: String): String {
        val grade = gradeName(enrollmentYear, xnm.take(4).toIntOrNull() ?: return fallback(xnm, xqm))
        val half = halfName(xqm)
        return if (grade != null && half != null) "$grade${half}学期" else fallback(xnm, xqm)
    }

    /** 简写：「大一上」。成绩、绩点等筛选标签用这个。 */
    fun short(enrollmentYear: Int?, xnm: String, xqm: String): String {
        val grade = gradeName(enrollmentYear, xnm.take(4).toIntOrNull() ?: return fallback(xnm, xqm))
        val half = halfName(xqm)
        return if (grade != null && half != null) "$grade$half" else fallback(xnm, xqm)
    }

    /** 便利重载：入学年份来自年级代码字符串。 */
    fun full(njdmId: String?, xnm: String, xqm: String): String =
        full(njdmId?.take(4)?.toIntOrNull(), xnm, xqm)

    fun short(njdmId: String?, xnm: String, xqm: String): String =
        short(njdmId?.take(4)?.toIntOrNull(), xnm, xqm)
}
