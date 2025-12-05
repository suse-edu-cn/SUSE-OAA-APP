package com.suseoaa.projectoaa.courseList.data.repository

import com.suseoaa.projectoaa.courseList.data.dao.CourseDao
import com.suseoaa.projectoaa.courseList.data.entity.ClassTimeEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseAccountEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseWithTimes
import com.suseoaa.projectoaa.courseList.data.remote.dto.CourseResponseJson
import com.suseoaa.projectoaa.courseList.data.remote.dto.Kb
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val dao: CourseDao) {

    // 获取账号列表
    val allAccounts: Flow<List<CourseAccountEntity>> = dao.getAllAccounts()

    // 根据学号、学年、学期获取课程
    fun getCourses(studentId: String, xnm: String, xqm: String): Flow<List<CourseWithTimes>> =
        dao.getCoursesByTerm(studentId, xnm, xqm)

    // 删除账号
    suspend fun deleteAccount(studentId: String) {
        dao.deleteAccount(studentId)
        dao.deleteAllCoursesByStudent(studentId)
    }

    // 保存自定义课程
    suspend fun saveCustomCourse(
        studentId: String,
        xnm: String,
        xqm: String,
        name: String,
        location: String,
        teacher: String,
        weekday: String,
        startNode: Int,
        duration: Int,
        weeks: String
    ) {
        val course = CourseEntity(
            studentId = studentId,
            courseName = name,
            xnm = xnm,
            xqm = xqm,
            isCustom = true,
            background = "",
        )
        // 转换节次格式 (例如 1-2)
        val periodStr = "$startNode-${startNode + duration - 1}"

        // 计算周次Mask
        val mask = parseWeeksToMask(weeks)

        val time = ClassTimeEntity(
            studentId = studentId,
            courseOwnerName = name,
            xnm = xnm,
            xqm = xqm,
            isCustom = true,
            weekday = weekday,
            period = periodStr,
            weeks = weeks,
            weeksMask = mask,
            location = location,
            teacher = teacher,
            duration = duration.toString()
        )
        dao.insertCustomCourse(course, time)
    }

    // 从网络响应保存
    suspend fun saveFromResponse(studentId: String, password: String, resp: CourseResponseJson) {
        // 1. 保存/更新账号信息
        val xsxx = resp.xsxx
        val xnm = xsxx?.xNM ?: "2024" // 默认值
        val xqm = xsxx?.xQM ?: "3"

        if (xsxx != null) {
            val account = CourseAccountEntity(
                studentId = studentId,
                password = password,
                name = xsxx.xM ?: "未知姓名",
                className = xsxx.bJMC ?: "未知班级",
                njdmId = xsxx.nJDMID ?: xnm,
                major = xsxx.zYMC ?: ""
            )
            dao.insertAccount(account)
        }

        // 2. 处理课程列表
        val rawList = resp.kbList ?: emptyList()
        val validList = rawList.filterNotNull().filter { !it.courseName.isNullOrBlank() }
        val groups: Map<String, List<Kb>> = validList.groupBy { it.courseName!! }

        val courses = mutableListOf<CourseEntity>()
        val allTimes = mutableListOf<ClassTimeEntity>()

        for ((courseName, list) in groups) {
            val infoSource = list.find { !it.courseId.isNullOrBlank() } ?: list.first()

            val course = CourseEntity(
                studentId = studentId,
                courseName = courseName,
                xnm = xnm,
                xqm = xqm,
                isCustom = false,
                remoteCourseId = infoSource.courseId ?: "",
                nature = infoSource.nature ?: "",
                background = infoSource.background ?: "",
                category = infoSource.category ?: "",
                assessment = infoSource.assessment ?: "",
                totalHours = infoSource.totalHours ?: ""
            )
            courses.add(course)

            val times = list.map { kb ->
                val mask = parseWeeksToMask(kb.weeks ?: "")
                ClassTimeEntity(
                    studentId = studentId,
                    courseOwnerName = courseName,
                    xnm = xnm,
                    xqm = xqm,
                    isCustom = false,
                    weekday = kb.dayOfWeek ?: "",
                    period = kb.period ?: "",
                    weeks = kb.weeks ?: "全周",
                    weeksMask = mask,
                    location = kb.location ?: "",
                    teacher = kb.teacher ?: "",
                    teacherTitle = kb.teacherTitle ?: "",
                    politicalStatus = kb.politicalStatus ?: "",
                    classGroup = kb.classGroup ?: ""
                )
            }
            allTimes.addAll(times)
        }

        // 3. 写入数据库
        dao.updateTermCourses(studentId, xnm, xqm, courses, allTimes)
    }

    internal fun parseWeeksToMask(raw: String): Long {
        if (raw.isBlank()) return 0L
        var mask = 0L
        try {
            var normalized = raw
                .replace("，", ",")
                .replace("；", ",")
                .replace(";", ",")
                .replace("、", ",")
                .replace("\n", ",")
                .replace(Regex("\\s+"), ",")
                .replace(Regex(",+"), ",")

            val parts = normalized.split(',')
            for (part in parts) {
                if (part.isBlank()) continue
                val isOdd = part.contains("单")
                val isEven = part.contains("双")
                val cleanPart = part.replace(Regex("[^0-9-]"), "")
                if (cleanPart.isBlank()) continue

                if (cleanPart.contains("-")) {
                    val rangeParts = cleanPart.split('-')
                    if (rangeParts.size >= 2) {
                        val start = rangeParts[0].toIntOrNull()
                        val end = rangeParts[1].toIntOrNull()
                        if (start != null && end != null) {
                            val range = if (start <= end) start..end else end..start
                            for (w in range) {
                                if (shouldInclude(w, isOdd, isEven)) mask = mask or (1L shl (w - 1))
                            }
                        }
                    }
                } else {
                    val w = cleanPart.toIntOrNull()
                    if (w != null && shouldInclude(w, isOdd, isEven)) {
                        mask = mask or (1L shl (w - 1))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return mask
    }

    private fun shouldInclude(week: Int, isOdd: Boolean, isEven: Boolean): Boolean {
        if (week !in 1..63) return false
        if (isOdd && !isEven && week % 2 == 0) return false
        if (isEven && !isOdd && week % 2 != 0) return false
        return true
    }
}