package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.SchoolApiService
import com.suseoaa.projectoaa.shared.domain.model.school.CourseResponseJson
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json

/**
 * 校历解析结果
 * @param startDate 学期最早的周一日期（如果有第0周则是第0周的周一，否则是第1周的周一）
 * @param hasWeekZero 是否存在第0周
 */
data class SemesterCalendarInfo(
    val startDate: String,
    val hasWeekZero: Boolean
)

class SchoolCourseRepository(
    private val api: SchoolApiService,
    private val json: Json
) {
    suspend fun getCourseSchedule(year: String, semester: String): Result<CourseResponseJson> {
        return try {
            val response = api.querySchedule(year = year, semester = semester)

            if (response.status.value == 200) {
                val jsonString = response.bodyAsText()

                if (isLoginRequired(jsonString)) {
                    return Result.failure(SessionExpiredException())
                }

                try {
                    val data = json.decodeFromString<CourseResponseJson>(jsonString)
                    Result.success(data)
                } catch (e: Exception) {
                    Result.failure(Exception("JSON 解析失败: ${e.message}"))
                }
            } else {
                if (response.status.value == 302 || response.status.value == 901) {
                    Result.failure(SessionExpiredException())
                } else {
                    Result.failure(Exception("请求失败: ${response.status.value}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从教务系统校历 HTML 中获取学期起始信息。
     *
     * HTML 结构关键点：
     * - `<tr class="tab-th-2">` 行包含每周的周次编号（可能从0开始，也可能从1开始）
     * - `<tbody>` 第一个 `<tr>` 是"星期一"行，其中 `<td id='YYYY-MM-DD'>` 对应每周的周一日期
     * - 两行的列数一一对应：第 N 列的周次编号对应第 N 列的周一日期
     *
     * @return SemesterCalendarInfo 包含起始日期和是否有第0周，如果解析失败返回 null
     */
    suspend fun fetchSemesterStart(): SemesterCalendarInfo? {
        return try {
            val response = api.getCalendar()
            if (response.status.value != 200) return null

            val html = response.bodyAsText()
            if (isLoginRequired(html)) return null

            parseSemesterStartFromCalendar(html)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 解析校历 HTML，提取起始日期和是否存在第0周
     */
    private fun parseSemesterStartFromCalendar(html: String): SemesterCalendarInfo? {
        try {
            // 1. 提取 tab-th-2 行中的周次编号
            val weekRow = extractWeekNumberRow(html) ?: return fallbackParseSemesterStart(html)
            val weekNumbers = parseThValues(weekRow)
            if (weekNumbers.isEmpty()) return fallbackParseSemesterStart(html)

            // 2. 检查是否存在第0周
            val hasWeekZero = weekNumbers.any { it.trim() == "0" }

            // 3. 提取 tbody 第一行（星期一）的所有 td id（即日期）
            val mondayDates = extractMondayDates(html)
            if (mondayDates.isEmpty()) return fallbackParseSemesterStart(html)

            // 4. 取第一列的周一日期作为学期起始日期（包含第0周或第1周）
            val firstValidDate = mondayDates.firstOrNull {
                it.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
            }
            if (firstValidDate != null) {
                return SemesterCalendarInfo(
                    startDate = firstValidDate,
                    hasWeekZero = hasWeekZero
                )
            }

            return fallbackParseSemesterStart(html)
        } catch (e: Exception) {
            return fallbackParseSemesterStart(html)
        }
    }

    /**
     * 提取 <tr class="tab-th-2"> 行的 HTML 内容
     */
    private fun extractWeekNumberRow(html: String): String? {
        val regex = Regex("""<tr\s+class\s*=\s*"tab-th-2"[^>]*>(.*?)</tr>""", RegexOption.DOT_MATCHES_ALL)
        return regex.find(html)?.groupValues?.get(1)
    }

    /**
     * 从一行 HTML 中提取所有 <th> 的文本内容
     */
    private fun parseThValues(rowHtml: String): List<String> {
        val regex = Regex("""<th[^>]*>(.*?)</th>""", RegexOption.DOT_MATCHES_ALL)
        return regex.findAll(rowHtml).map { it.groupValues[1].trim() }.toList()
    }

    /**
     * 从 tbody 的第一个 <tr> 中提取所有 <td> 的 id 值（日期字符串）
     */
    private fun extractMondayDates(html: String): List<String> {
        // 定位 <tbody> 内容
        val tbodyRegex = Regex("""<tbody>(.*?)</tbody>""", RegexOption.DOT_MATCHES_ALL)
        val tbodyContent = tbodyRegex.find(html)?.groupValues?.get(1) ?: return emptyList()

        // 取第一个 <tr>...</tr>（星期一）
        val firstTrRegex = Regex("""<tr[^>]*>(.*?)</tr>""", RegexOption.DOT_MATCHES_ALL)
        val firstTrContent = firstTrRegex.find(tbodyContent)?.groupValues?.get(1) ?: return emptyList()

        // 提取所有 td 的 id 值
        val tdIdRegex = Regex("""<td\s+id\s*=\s*'([^']*)'""")
        return tdIdRegex.findAll(firstTrContent).map { it.groupValues[1] }.toList()
    }

    /**
     * 回退方案：用旧的正则匹配 "YYYY-MM-DD 至" 模式
     */
    private fun fallbackParseSemesterStart(html: String): SemesterCalendarInfo? {
        val regex = Regex("""(\d{4}-\d{2}-\d{2})\s*至""")
        val date = regex.find(html)?.groupValues?.get(1) ?: return null
        return SemesterCalendarInfo(startDate = date, hasWeekZero = false)
    }

    private fun isLoginRequired(content: String): Boolean {
        return content.contains("用户登录") || content.contains("/xtgl/login_slogin.html")
    }

    class SessionExpiredException : Exception("Session Expired")
}
