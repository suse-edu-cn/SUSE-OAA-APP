package com.suseoaa.projectoaa.shared.domain.course

import com.suseoaa.projectoaa.shared.domain.model.school.CourseResponseJson
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 用真实教务系统响应做端到端校验。
 *
 * 这份样本是 2026-2027学年第1学期的完整课表（已裁剪掉与解析无关的字段），
 * 里面同时包含区间周次、`第N周` 单周课，以及只出现在 `sjkList` 里的整周实践课。
 */
class RealTimetableParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sample = """
    {
      "xsxx": { "XM": "鄢楠", "XNM": "2026", "XQM": "3", "BJMC": "计科20243", "NJDM_ID": "2024" },
      "sjkList": [
        {
          "kcmc": "IT项目实习", "jsxm": "赵良军", "jxbzh": "计科20243",
          "kclb": "实践课", "khfsmc": "考查", "qsjsz": "19-20周",
          "xf": "2.0", "xqmc": "临港校区"
        }
      ],
      "kbList": [
        { "kcmc": "机器学习",             "xqj": "1", "jcs": "5-6",  "zcd": "第4周" },
        { "kcmc": "计算机网络",           "xqj": "1", "jcs": "7-8",  "zcd": "1-5周,7-11周" },
        { "kcmc": "机器学习",             "xqj": "2", "jcs": "1-2",  "zcd": "1-5周,7-9周" },
        { "kcmc": "单片机原理及接口技术", "xqj": "2", "jcs": "5-6",  "zcd": "1-5周,7-8周" },
        { "kcmc": "软件工程",             "xqj": "2", "jcs": "7-8",  "zcd": "10-17周" },
        { "kcmc": "单片机原理及接口技术", "xqj": "2", "jcs": "9-11", "zcd": "10-14周" },
        { "kcmc": "计算机网络",           "xqj": "4", "jcs": "7-8",  "zcd": "1-4周,6-11周" },
        { "kcmc": "机器学习",             "xqj": "5", "jcs": "3-4",  "zcd": "1-3周,6-9周" },
        { "kcmc": "单片机原理及接口技术", "xqj": "6", "jcs": "1-2",  "zcd": "第6周" },
        { "kcmc": "机器学习",             "xqj": "6", "jcs": "5-8",  "zcd": "7-10周" },
        { "kcmc": "单片机原理及接口技术", "xqj": "7", "jcs": "5-6",  "zcd": "第3周" }
      ]
    }
    """.trimIndent()

    private val response: CourseResponseJson get() = json.decodeFromString(sample)

    @Test
    fun `完整响应能被解析`() {
        val parsed = response
        assertEquals(11, parsed.kbList?.size)
        assertEquals(1, parsed.sjkList?.size)
    }

    @Test
    fun `第4周的单周课会出现在第4周而不是别的周`() {
        val item = response.kbList!!.first { it.zcd == "第4周" }
        val weeks = (1..25).filter { CourseScheduleParser.isWeekActive(it, item.zcd!!) }
        assertEquals(listOf(4), weeks)
        assertEquals(1, CourseScheduleParser.parseWeekday(item.xqj!!))
        assertEquals(PeriodSpan(5, 2), CourseScheduleParser.parsePeriod(item.jcs!!))
    }

    @Test
    fun `每一条课程都至少能排进一周`() {
        // 回归用例：`第N周` 解析失败时，这类课的掩码为 0，整门课在任何一周都不显示
        response.kbList!!.forEach { item ->
            val mask = CourseScheduleParser.weeksToMask(item.zcd!!)
            assertTrue(mask != 0L, "周次 ${item.zcd} 解析后没有任何有效周次")
        }
    }

    @Test
    fun `第6周只有三门课`() {
        val atWeek6 = response.kbList!!
            .filter { CourseScheduleParser.isWeekActive(6, it.zcd!!) }
            .map { it.zcd }
        // 第6周是"机动周"：多数课程的周次都特意跳过它，只剩下这三条
        assertEquals(listOf("1-4周,6-11周", "1-3周,6-9周", "第6周"), atWeek6)
    }

    @Test
    fun `跳过的那一周确实没有课`() {
        // `1-5周,7-11周` 明确跳过第6周
        val item = response.kbList!!.first { it.zcd == "1-5周,7-11周" }
        assertTrue(CourseScheduleParser.isWeekActive(5, item.zcd!!))
        assertTrue(!CourseScheduleParser.isWeekActive(6, item.zcd!!))
        assertTrue(CourseScheduleParser.isWeekActive(7, item.zcd!!))
    }

    @Test
    fun `整周实践课能被解析并落在19到20周`() {
        val practice = response.sjkList!!.single()
        assertEquals("IT项目实习", practice.kcmc)
        assertEquals("赵良军", practice.jsxm)

        val weeks = (1..25).filter { CourseScheduleParser.isWeekActive(it, practice.qsjsz!!) }
        assertEquals(listOf(19, 20), weeks)
    }
}
