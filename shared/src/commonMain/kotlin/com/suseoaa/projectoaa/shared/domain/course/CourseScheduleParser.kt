package com.suseoaa.projectoaa.shared.domain.course

/**
 * 课表文本解析器：把教务系统给的周次 / 节次 / 星期文本，统一解析成程序能用的数字。
 *
 * 教务系统导出的课表里，同一类信息存在多种写法（下面的例子都取自真实课表）：
 *
 * - 周次：`1-5周,7-9周`、`第6周`、`1-4周,6-11周`、`1-16周(单)`、`2-16周（双）`、`19-20周`
 * - 节次：`1-2`、`3-4节`、`5-8节`、`9-11节`、`5`
 * - 星期：`1`..`7`、`星期一`..`星期日`、`周三`、`周天`
 *
 * 其中最容易被忽略的是**单周课**：教务系统对只上一周的课（补课、机动周）写作 `第6周`，
 * 而不是 `6周`。旧代码只把"周"字去掉，`第6` 解析成数字失败，这类课直接不显示。
 *
 * 这套逻辑历史上在 4 个文件里各抄了一份，互相之间还有细微差异，导致同一门课在课表页、
 * 桌面小组件和上课提醒里表现不一致，现在统一到这里。
 */
object CourseScheduleParser {

    /**
     * 掩码支持的最大周次。Long 有 64 位，取 53 周（一年）足够用，且离溢出还有余量。
     */
    const val MAX_WEEK: Int = 53

    /** 周次掩码的位序：第 N 周对应第 N 位（第 0 周对应第 0 位，教务系统确实存在第 0 周）。 */
    private const val MIN_WEEK: Int = 0

    /** 连接起止周/起止节的分隔符，除半角连字符外还兼容各种全角写法。 */
    private const val RANGE_SEPARATORS = "-~～–—至"

    private val RANGE_REGEX = Regex("""(\d+)\s*[$RANGE_SEPARATORS]\s*(\d+)""")
    private val NUMBER_REGEX = Regex("""\d+""")

    // ==================== 周次 ====================

    /**
     * 把周次文本解析成掩码：第 N 周有课，则第 N 位为 1。
     *
     * 空文本返回 0（表示"没有周次限制"，由调用方决定语义），无法识别的片段会被跳过而不是让整串失败。
     */
    fun weeksToMask(weeksStr: String): Long {
        if (weeksStr.isBlank()) return 0L

        val normalized = normalizeDigits(weeksStr)
        val segments = normalized.split(',', '，', ';', '；', '、').filter { it.isNotBlank() }
        if (segments.isEmpty()) return 0L

        // 只有当没有任何分段自带单双周标记时，才把整串的单双周当作全局设置。
        // 否则形如 `4-6周(双),7-8周` 的课，会把没标记的 `7-8周` 也误判成只上双周。
        val segmentsDeclareParity = segments.any { it.hasParityTag() }
        val globalParity = if (segmentsDeclareParity) Parity.ALL else normalized.parityTag()

        var mask = 0L
        for (segment in segments) {
            val parity = segment.parityTag().takeIf { it != Parity.ALL } ?: globalParity
            mask = mask or segment.toWeekMask(parity)
        }
        return mask
    }

    /**
     * 判断某一周是否要上这门课。
     *
     * @param weeksStr 周次原文，优先使用
     * @param fallbackMask 周次原文为空时使用的掩码（历史数据里存的是同一份文本解析出来的结果）
     */
    fun isWeekActive(week: Int, weeksStr: String, fallbackMask: Long = 0L): Boolean {
        val mask = if (weeksStr.isBlank()) fallbackMask else weeksToMask(weeksStr)
        // 掩码为 0 说明这门课没写周次，按"每周都上"处理，避免因为脏数据整门课消失。
        if (mask == 0L) return true
        return isWeekInMask(week, mask)
    }

    /** 测试某一周是否落在掩码里，越界的周次一律返回 false（而不是让移位溢出）。 */
    fun isWeekInMask(week: Int, mask: Long): Boolean {
        if (week !in MIN_WEEK..MAX_WEEK) return false
        return (mask and (1L shl week)) != 0L
    }

    private fun String.toWeekMask(parity: Parity): Long {
        val range = RANGE_REGEX.find(this)
        val (start, end) = if (range != null) {
            val first = range.groupValues[1].toIntOrNull() ?: return 0L
            val second = range.groupValues[2].toIntOrNull() ?: return 0L
            // 容忍 `16-1周` 这种起止写反的脏数据
            minOf(first, second) to maxOf(first, second)
        } else {
            val single = NUMBER_REGEX.find(this)?.value?.toIntOrNull()
            // 只写了"单周"/"双周"而没有具体周次时，按整个学期的单/双周处理
                ?: return if (parity == Parity.ALL) 0L else fullTermMask(parity)
            single to single
        }

        var mask = 0L
        for (week in maxOf(start, MIN_WEEK)..minOf(end, MAX_WEEK)) {
            if (parity.accepts(week)) mask = mask or (1L shl week)
        }
        return mask
    }

    private fun fullTermMask(parity: Parity): Long {
        var mask = 0L
        for (week in 1..MAX_WEEK) {
            if (parity.accepts(week)) mask = mask or (1L shl week)
        }
        return mask
    }

    private enum class Parity {
        ALL, ODD, EVEN;

        fun accepts(week: Int): Boolean = when (this) {
            ALL -> true
            ODD -> week % 2 == 1
            EVEN -> week % 2 == 0
        }
    }

    private fun String.hasParityTag(): Boolean = contains('单') || contains('双')

    private fun String.parityTag(): Parity {
        val odd = contains('单')
        val even = contains('双')
        return when {
            odd && !even -> Parity.ODD
            even && !odd -> Parity.EVEN
            else -> Parity.ALL
        }
    }

    // ==================== 星期 ====================

    /**
     * 解析星期文本，返回 1(周一)..7(周日)。识别不出来时返回 1，保证课程至少能显示出来。
     */
    fun parseWeekday(weekday: String): Int {
        val text = normalizeDigits(weekday).trim()
        if (text.isEmpty()) return 1

        // 先按中文匹配，"星期日"/"周天"这类写法里没有阿拉伯数字
        val chinese = when {
            text.contains('一') -> 1
            text.contains('二') -> 2
            text.contains('三') -> 3
            text.contains('四') -> 4
            text.contains('五') -> 5
            text.contains('六') -> 6
            text.contains('日') || text.contains('天') -> 7
            else -> null
        }
        if (chinese != null) return chinese

        return NUMBER_REGEX.find(text)?.value?.toIntOrNull()?.takeIf { it in 1..7 } ?: 1
    }

    // ==================== 节次 ====================

    /**
     * 解析节次文本，如 `1-2`、`3-4节`、`9-11节`、`5`。
     *
     * @return 起始节次与连上几节；识别不出来时按"第 1 节，上 1 节"处理。
     */
    fun parsePeriod(period: String): PeriodSpan {
        val text = normalizeDigits(period)

        RANGE_REGEX.find(text)?.let { match ->
            val first = match.groupValues[1].toIntOrNull()
            val second = match.groupValues[2].toIntOrNull()
            if (first != null && second != null) {
                val start = minOf(first, second)
                val end = maxOf(first, second)
                return PeriodSpan(start = start, span = end - start + 1)
            }
        }

        val single = NUMBER_REGEX.find(text)?.value?.toIntOrNull() ?: 1
        return PeriodSpan(start = single, span = 1)
    }

    // ==================== 通用 ====================

    /** 把全角数字转成半角，教务系统偶尔会混用。 */
    private fun normalizeDigits(text: String): String {
        if (text.none { it in '０'..'９' }) return text
        return buildString(text.length) {
            for (char in text) {
                append(if (char in '０'..'９') '0' + (char - '０') else char)
            }
        }
    }
}

/**
 * 一段连续的节次：从第 [start] 节开始，连上 [span] 节。
 */
data class PeriodSpan(val start: Int, val span: Int)
