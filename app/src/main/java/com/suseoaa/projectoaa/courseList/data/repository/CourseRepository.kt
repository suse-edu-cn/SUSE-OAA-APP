package com.suseoaa.projectoaa.courseList.data.repository

import com.suseoaa.projectoaa.courseList.data.dao.CourseDao
import com.suseoaa.projectoaa.courseList.data.entity.ClassTimeEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseWithTimes
import com.suseoaa.projectoaa.courseList.data.remote.dto.CourseResponseJson
import com.suseoaa.projectoaa.courseList.data.remote.dto.Kb
import kotlinx.coroutines.flow.Flow

/**
 * 课程仓库
 * MVVM架构 - Model层（Repository，封装数据访问逻辑）
 */
class CourseRepository(private val dao: CourseDao) {
    
    /**
     * 获取所有课程及时间
     */
    fun getAllCoursesWithTimes(): Flow<List<CourseWithTimes>> = 
        dao.getAllCoursesWithTimes()
    
    /**
     * 从网络响应保存课程数据
     */
    suspend fun saveFromResponse(resp: CourseResponseJson) {
        val groups: Map<String, List<Kb>> = (resp.kbList ?: emptyList())
            .filterNotNull()
            .groupBy { it.kcmc ?: "未知课程" }
        
        for ((courseName, list) in groups) {
            val first = list.first()
            val course = CourseEntity(
                courseName = courseName,
                remoteCourseId = first.kchId ?: "",
                nature = first.kcxz ?: "",
                background = first.kcbj ?: "",
                category = first.kclb ?: "",
                assessment = first.khfsmc ?: "",
                totalHours = first.kcxszc ?: ""
            )
            val times = list.map { kb ->
                val mask = parseWeeksToMask(kb.zcd ?: "")
                ClassTimeEntity(
                    courseOwnerName = courseName,
                    weekday = kb.xqjmc ?: "",
                    period = kb.jc ?: "",
                    weeks = kb.zcd ?: "",
                    weeksMask = mask,
                    location = kb.cdmc ?: "",
                    teacher = kb.xm ?: "",
                    teacherTitle = kb.zcmc ?: "",
                    politicalStatus = kb.zzmm ?: "",
                    classGroup = kb.jxbzc ?: ""
                )
            }
            dao.upsertCourseWithTimes(course, times)
        }
    }
    
    /**
     * 解析周次字符串为位掩码
     * 例如：1-16周、1-16周 单周、2-14周 双周、1,3,5周、3-5,7,9-10周
     */
    internal fun parseWeeksToMask(raw: String): Long {
        var s = raw.replace("周", "").replace(" ", "").trim()
        if (s.isEmpty()) return 0L
        val isOdd = s.contains("单")
        val isEven = s.contains("双")
        s = s.replace("单", "").replace("双", "").replace("周", "").replace("，", ",")
        
        var mask = 0L
        fun setWeek(w: Int) { if (w in 1..63) mask = mask or (1L shl (w - 1)) }
        
        val parts = s.split(',').filter { it.isNotBlank() }
        for (p in parts) {
            if ("-" in p) {
                val (a, b) = p.split('-').mapNotNull { it.toIntOrNull() }.let { it.first() to it.last() }
                val range = if (a <= b) a..b else b..a
                for (w in range) setWeek(w)
            } else {
                p.toIntOrNull()?.let { setWeek(it) }
            }
        }
        if (isOdd && !isEven) {
            mask = mask and ODD_MASK
        } else if (isEven && !isOdd) {
            mask = mask and EVEN_MASK
        }
        return mask
    }
    
    companion object {
        private const val ODD_BITS: Long = 0x5555555555555555L
        private val ODD_MASK = ODD_BITS
        private val EVEN_MASK = (ODD_BITS shl 1)
    }
}
