package com.suseoaa.projectoaa.core.data.repository

import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.entity.ClassTimeEntity
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.CourseEntity
import com.suseoaa.projectoaa.core.database.entity.CourseWithTimes
import com.suseoaa.projectoaa.core.network.model.course.CourseResponseJson
import com.suseoaa.projectoaa.core.network.model.course.Kb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.filter
import kotlin.collections.map

class CourseRepository @Inject constructor(
    private val dao: CourseDao
) {

    val allAccounts: Flow<List<CourseAccountEntity>> = dao.getAllAccounts()

    // 处理列表排序交换
    // fromIndex 和 toIndex 是 UI 列表中的位置，currentList 是当前的列表数据
    suspend fun swapAccountOrder(
        fromIndex: Int,
        toIndex: Int,
        currentList: List<CourseAccountEntity>
    ) = withContext(Dispatchers.IO) {
        if (fromIndex == toIndex) return@withContext
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices) return@withContext

        // 1. 在内存中交换顺序
        val mutableList = currentList.toMutableList()
        java.util.Collections.swap(mutableList, fromIndex, toIndex)

        // 2. 将新的顺序索引写入数据库
        // 注意：这里会重新分配 0, 1, 2... 的 sortIndex 给调整后的列表
        dao.updateAllSortIndices(mutableList)
    }

    fun getCourses(studentId: String, xnm: String, xqm: String): Flow<List<CourseWithTimes>> {
        val coursesFlow = dao.getCourseEntities(studentId, xnm, xqm)
        val timesFlow = dao.getClassTimeEntities(studentId, xnm, xqm)

        return coursesFlow.combine(timesFlow) { courses, allTimes ->
            courses.map { course ->
                val matchingTimes = allTimes.filter { time ->
                    time.courseOwnerName == course.courseName &&
                            time.isCustom == course.isCustom
                }
                CourseWithTimes(course, matchingTimes)
            }
        }
//        确保上述组合逻辑在计算线程执行
            .flowOn(Dispatchers.Default)
    }

    suspend fun deleteAccount(studentId: String) {
        dao.deleteAccount(studentId)
        dao.deleteAllCoursesByStudent(studentId)
    }

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
        val periodStr = "$startNode-${startNode + duration - 1}"
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

    suspend fun saveFromResponse(studentId: String, password: String, resp: CourseResponseJson) {
        val xsxx = resp.xsxx
        val xnm = xsxx?.xNM ?: "2024"
        val xqm = xsxx?.xQM ?: "3"

        if (xsxx != null) {
            // [关键修改] 保存账户前，先检查旧数据，为了保留 sortIndex
            val oldAccount = dao.getAccountById(studentId)

            // 如果是老用户，沿用旧的 sortIndex；如果是新用户，放到末尾 (max + 1)
            val newSortIndex = oldAccount?.sortIndex ?: ((dao.getMaxSortIndex() ?: -1) + 1)

            val account = CourseAccountEntity(
                studentId = studentId,
                password = password,
                name = xsxx.xM ?: "未知姓名",
                className = xsxx.bJMC ?: "未知班级",
                njdmId = xsxx.nJDMID ?: xnm,
                major = xsxx.zYMC ?: "",
                // 确保这里传入了计算好的 sortIndex
                sortIndex = newSortIndex
            )
            dao.insertAccount(account)
        }

        // 下面是保存课程的代码，保持不变
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

        dao.updateTermCourses(studentId, xnm, xqm, courses, allTimes)
    }

    internal fun parseWeeksToMask(raw: String): Long {
        if (raw.isBlank()) return 0L
        var mask = 0L
        try {
            val normalized = raw
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mask
    }

    private fun shouldInclude(week: Int, isOdd: Boolean, isEven: Boolean): Boolean {
        if (week !in 1..63) return false
        if (isOdd && !isEven && week % 2 == 0) return false
        if (isEven && !isOdd && week % 2 != 0) return false
        return true
    }
}