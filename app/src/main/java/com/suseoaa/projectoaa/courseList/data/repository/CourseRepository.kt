package com.suseoaa.projectoaa.courseList.data.repository

import com.suseoaa.projectoaa.courseList.data.dao.CourseDao
import com.suseoaa.projectoaa.courseList.data.entity.ClassTimeEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseWithTimes
import com.suseoaa.projectoaa.courseList.data.remote.dto.CourseResponseJson
import com.suseoaa.projectoaa.courseList.data.remote.dto.Kb
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val dao: CourseDao) {

    fun getCoursesByStudent(studentId: String): Flow<List<CourseWithTimes>> =
        dao.getCoursesByStudentId(studentId)

    suspend fun saveFromResponse(studentId: String, resp: CourseResponseJson) {
        val rawList = resp.kbList ?: emptyList()

        // 1. 过滤无效数据，确保课程名存在
        val validList = rawList.filterNotNull().filter {
            !it.courseName.isNullOrBlank()
        }

        // 2. 按课程名分组
        // 数据库 CourseEntity 主键是 (studentId, courseName)，同名课程必须归为一个实体
        // 但其下的所有时间段（ClassTimeEntity）会保留所有差异（如不同老师、不同周次）
        val groups: Map<String, List<Kb>> = validList.groupBy { it.courseName!! }

        for ((courseName, list) in groups) {
            // 选出信息最全的一个作为课程元数据（优先选有 ID 的）
            val infoSource = list.find { !it.courseId.isNullOrBlank() } ?: list.first()

            val course = CourseEntity(
                studentId = studentId,
                courseName = courseName,
                remoteCourseId = infoSource.courseId ?: "",
                nature = infoSource.nature ?: "",
                background = infoSource.background ?: "",
                category = infoSource.category ?: "",
                assessment = infoSource.assessment ?: "",
                totalHours = infoSource.totalHours ?: ""
            )

            // 3. 构建所有时间段
            // map 操作会为列表中的每一项生成一个时间段记录，确保不会因为合并而丢失“张三老师”和“李四老师”的区别
            val times = list.map { kb ->
                val mask = parseWeeksToMask(kb.weeks ?: "")

                ClassTimeEntity(
                    studentId = studentId,
                    courseOwnerName = courseName,
                    weekday = kb.dayOfWeek ?: "",
                    period = kb.period ?: "",
                    weeks = kb.weeks ?: "全周",
                    weeksMask = mask,
                    location = kb.location ?: "",
                    teacher = kb.teacher ?: "", // 这里的老师信息会被完整保留
                    teacherTitle = kb.teacherTitle ?: "",
                    politicalStatus = kb.politicalStatus ?: "",
                    classGroup = kb.classGroup ?: ""
                )
            }

            // 4. 写入数据库
            // upsert 会先插入/更新 Course，然后替换该课程名下的所有 Time
            dao.upsertCourseWithTimes(course, times)
        }
    }

    /**
     * 解析周次字符串，生成位掩码。
     * 支持格式示例：
     * - "1-16周"
     * - "1-8周(单), 10-16周(双)"
     * - "1-8周 10-16周" (空格分隔)
     * - "1,3,5-9"
     */
    internal fun parseWeeksToMask(raw: String): Long {
        if (raw.isBlank()) return 0L

        var mask = 0L

        try {
            // 1. 预处理：统一所有可能的分隔符为英文逗号
            // 关键修复：将空格 " " 也视为分隔符，防止 "1-8 10-16" 变成 "1-810-16"
            var normalized = raw
                .replace("，", ",")
                .replace("；", ",")
                .replace(";", ",")
                .replace("、", ",")
                .replace("\n", ",") // 换行符
                .replace(Regex("\\s+"), ",") // 将所有空白字符（包括空格、制表符）替换为逗号

            // 移除可能产生的连续逗号
            normalized = normalized.replace(Regex(",+"), ",")

            // 2. 按逗号拆分片段
            val parts = normalized.split(',')

            for (part in parts) {
                if (part.isBlank()) continue

                // 3. 判断当前片段的单双周属性
                // 这样可以正确处理混合情况，如一段单周，另一段双周
                val isOdd = part.contains("单")
                val isEven = part.contains("双")

                // 4. 提取纯数字范围
                // 移除除数字和连字符外的所有字符
                val cleanPart = part.replace(Regex("[^0-9-]"), "")

                if (cleanPart.isBlank()) continue

                // 5. 解析范围 (e.g., "1-8") 或 单点 (e.g., "5")
                if (cleanPart.contains("-")) {
                    val rangeParts = cleanPart.split('-')
                    if (rangeParts.size >= 2) {
                        // 过滤掉空字符串，防止 "-5" 或 "5-" 这种情况导致的异常
                        val start = rangeParts[0].toIntOrNull()
                        val end = rangeParts[1].toIntOrNull()

                        if (start != null && end != null) {
                            val range = if (start <= end) start..end else end..start
                            for (w in range) {
                                if (shouldInclude(w, isOdd, isEven)) {
                                    mask = mask or (1L shl (w - 1))
                                }
                            }
                        }
                    }
                } else {
                    val w = cleanPart.toIntOrNull()
                    if (w != null) {
                        if (shouldInclude(w, isOdd, isEven)) {
                            mask = mask or (1L shl (w - 1))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 解析失败时不应该返回 -1 (全 1)，因为这会显示在所有周
            // 返回 0 可能更好，或者视情况而定。为了安全起见，这里记录错误但允许流程继续
            return 0L
        }

        return mask
    }

    private fun shouldInclude(week: Int, isOdd: Boolean, isEven: Boolean): Boolean {
        // 防止越界
        if (week !in 1..63) return false

        // 如果指定了单周，且当前是偶数 -> 不包含
        if (isOdd && !isEven && week % 2 == 0) return false

        // 如果指定了双周，且当前是奇数 -> 不包含
        if (isEven && !isOdd && week % 2 != 0) return false

        return true
    }
}