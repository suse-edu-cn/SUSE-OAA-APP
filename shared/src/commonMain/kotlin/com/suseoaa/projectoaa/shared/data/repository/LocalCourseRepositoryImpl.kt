package com.suseoaa.projectoaa.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.suseoaa.projectoaa.shared.domain.course.CourseScheduleParser
import com.suseoaa.projectoaa.shared.domain.model.course.*
import com.suseoaa.projectoaa.shared.domain.model.school.CourseResponseJson
import com.suseoaa.projectoaa.shared.database.CourseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.suseoaa.projectoaa.shared.util.AppLog
import com.suseoaa.projectoaa.shared.domain.repository.LocalCourseRepository

class LocalCourseRepositoryImpl(private val database: CourseDatabase) : LocalCourseRepository {

    // ===== 账号管理 =====

    override fun getAllAccounts(): Flow<List<CourseAccountEntity>> {
        return database.courseAccountQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { it.toEntity() }
            }
    }

    override suspend fun getAccountById(studentId: String): CourseAccountEntity? =
        withContext(Dispatchers.IO) {
            database.courseAccountQueries.selectById(studentId).executeAsOneOrNull()?.toEntity()
        }

    override suspend fun insertOrReplaceAccount(account: CourseAccountEntity): Unit = withContext(Dispatchers.IO) {
        database.courseAccountQueries.insertOrReplace(
            studentId = account.studentId,
            password = account.password,
            name = account.name,
            className = account.className,
            njdmId = account.njdmId,
            major = account.major,
            sortIndex = account.sortIndex.toLong(),
            jgId = account.jgId,
            zyhId = account.zyhId
        )
    }

    override suspend fun updateSortIndex(studentId: String, newIndex: Int): Unit = withContext(Dispatchers.IO) {
        database.courseAccountQueries.updateSortIndex(newIndex.toLong(), studentId)
    }

    override suspend fun getMaxSortIndex(): Int = withContext(Dispatchers.IO) {
        database.courseAccountQueries.getMaxSortIndex().executeAsOneOrNull()?.maxIndex?.toInt() ?: 0
    }

    override suspend fun deleteAccount(studentId: String): Unit = withContext(Dispatchers.IO) {
        database.courseAccountQueries.deleteById(studentId)
    }

    override suspend fun updateMajorInfo(studentId: String, jgId: String, zyhId: String, njdmId: String): Unit =
        withContext(Dispatchers.IO) {
            database.courseAccountQueries.updateMajorInfo(jgId, zyhId, njdmId, studentId)
        }

    override suspend fun updateAllSortIndices(accounts: List<CourseAccountEntity>) =
        withContext(Dispatchers.IO) {
            database.transaction {
                accounts.forEachIndexed { index, account ->
                    // 事务是同步的，因此我们调用生成的 updateSortIndex。
                    database.courseAccountQueries.updateSortIndex(index.toLong(), account.studentId)
                }
            }
        }

    // ===== 课程管理 =====

    override fun getCoursesByTerm(studentId: String, xnm: String, xqm: String): Flow<List<CourseEntity>> {
        return database.courseQueries.selectByTerm(studentId, xnm, xqm)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toEntity() } }
    }

    override fun getClassTimesByTerm(
        studentId: String,
        xnm: String,
        xqm: String
    ): Flow<List<ClassTimeEntity>> {
        return database.classTimeQueries.selectByTerm(studentId, xnm, xqm)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toEntity() } }
    }

    override fun getCourses(studentId: String, xnm: String, xqm: String): Flow<List<CourseWithTimes>> {
        return getCoursesByTerm(studentId, xnm, xqm).map { courses ->
            courses.map { course ->
                val times = database.classTimeQueries.selectByCourse(
                    studentId = course.studentId,
                    courseOwnerName = course.courseName,
                    xnm = course.xnm,
                    xqm = course.xqm,
                    isCustom = if (course.isCustom) 1L else 0L
                ).executeAsList().map { it.toEntity() }
                CourseWithTimes(course, times)
            }
        }
    }

    override fun getAllCoursesByStudent(studentId: String): Flow<List<CourseWithTimes>> {
        return database.courseQueries.selectAllByStudent(studentId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                val allTimes = database.classTimeQueries.selectAllByStudent(studentId).executeAsList().map { it.toEntity() }
                val timesByCourseKey = allTimes.groupBy { 
                    "${it.studentId}_${it.courseOwnerName}_${it.xnm}_${it.xqm}_${it.isCustom}" 
                }
                list.map { c ->
                    val course = c.toEntity()
                    val key = "${course.studentId}_${course.courseName}_${course.xnm}_${course.xqm}_${course.isCustom}"
                    CourseWithTimes(course, timesByCourseKey[key] ?: emptyList())
                }
            }
    }

    override fun getAvailableTerms(studentId: String): Flow<List<Pair<String, String>>> {
        return database.courseQueries.selectDistinctTerms(studentId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { it.xnm to it.xqm }
            }
    }

    // 从插入辅助函数中移除了 suspend 以便在事务内部使用
    private fun insertCourseInternal(course: CourseEntity) {
        database.courseQueries.insertOrReplace(
            studentId = course.studentId,
            courseName = course.courseName,
            xnm = course.xnm,
            xqm = course.xqm,
            isCustom = if (course.isCustom) 1L else 0L,
            remoteCourseId = course.remoteCourseId,
            nature = course.nature,
            background = course.background,
            category = course.category,
            assessment = course.assessment,
            totalHours = course.credit
        )
    }

    override suspend fun insertCourse(course: CourseEntity) = withContext(Dispatchers.IO) {
        insertCourseInternal(course)
    }

    private fun insertClassTimeInternal(time: ClassTimeEntity) {
        database.classTimeQueries.insert(
            studentId = time.studentId,
            courseOwnerName = time.courseOwnerName,
            xnm = time.xnm,
            xqm = time.xqm,
            isCustom = if (time.isCustom) 1L else 0L,
            weekday = time.weekday,
            period = time.period,
            weeks = time.weeks,
            weeksMask = time.weeksMask,
            location = time.location,
            teacher = time.teacher,
            duration = time.duration,
            teacherTitle = time.teacherTitle,
            politicalStatus = time.politicalStatus,
            classGroup = time.classGroup
        )
    }

    override suspend fun insertClassTime(time: ClassTimeEntity) = withContext(Dispatchers.IO) {
        insertClassTimeInternal(time)
    }

    override suspend fun deleteRemoteCoursesByTerm(studentId: String, xnm: String, xqm: String): Unit =
        withContext(Dispatchers.IO) {
            database.courseQueries.deleteRemoteCoursesByTerm(studentId, xnm, xqm)
        }

    override suspend fun deleteAllCoursesByStudent(studentId: String): Unit = withContext(Dispatchers.IO) {
        database.courseQueries.deleteAllByStudent(studentId)
    }

    override suspend fun deleteCourse(
        studentId: String,
        courseName: String,
        xnm: String,
        xqm: String,
        isCustom: Boolean
    ): Unit = withContext(Dispatchers.IO) {
        database.courseQueries.deleteCourse(
            studentId = studentId,
            courseName = courseName,
            xnm = xnm,
            xqm = xqm,
            isCustom = if (isCustom) 1L else 0L
        )
    }

    override suspend fun updateTermCourses(
        studentId: String,
        xnm: String,
        xqm: String,
        courses: List<CourseEntity>,
        times: List<ClassTimeEntity>,
        practiceCourses: List<PracticeCourseEntity>) = withContext(Dispatchers.IO) {
        database.transaction {
            // 先删除时间表，再删除课程表（确保完全清除）
            database.classTimeQueries.deleteRemoteByTerm(studentId, xnm, xqm)
            database.courseQueries.deleteRemoteCoursesByTerm(studentId, xnm, xqm)
            database.practiceCourseQueries.deleteByTerm(studentId, xnm, xqm)
            courses.forEach { insertCourseInternal(it) }
            times.forEach { insertClassTimeInternal(it) }
            practiceCourses.forEach { insertPracticeCourseInternal(it) }
        }
    }

    /** 整周实践课（实习等），没有星期节次，不进课表格子，单独查询。 */
    override fun getPracticeCourses(studentId: String, xnm: String, xqm: String): Flow<List<PracticeCourseEntity>> {
        return database.practiceCourseQueries.selectByTerm(studentId, xnm, xqm)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toEntity() } }
    }

    private fun insertPracticeCourseInternal(entity: PracticeCourseEntity) {
        database.practiceCourseQueries.insertOrReplace(
            studentId = entity.studentId,
            xnm = entity.xnm,
            xqm = entity.xqm,
            courseName = entity.courseName,
            teacher = entity.teacher,
            classGroup = entity.classGroup,
            weeks = entity.weeks,
            weeksMask = entity.weeksMask,
            credit = entity.credit,
            assessment = entity.assessment,
            campus = entity.campus
        )
    }

    override suspend fun insertCustomCourse(course: CourseEntity, time: ClassTimeEntity) =
        withContext(Dispatchers.IO) {
            database.transaction {
                insertCourseInternal(course)
                insertClassTimeInternal(time)
            }
        }

    override suspend fun saveFromResponse(
        studentId: String,
        password: String,
        response: CourseResponseJson
    ) = withContext(Dispatchers.IO) {
        val xsxx = response.xsxx
        val list = response.kbList ?: emptyList()

        // 确定学年学期
        val xnm = xsxx?.xnm ?: list.firstOrNull()?.xnm ?: "2024"
        val xqm = xsxx?.xqm ?: list.firstOrNull()?.xqm ?: "3"

        // 保存账号信息（非常重要！）
        if (xsxx != null) {
            val oldAccount = getAccountById(studentId)
            val newSortIndex = oldAccount?.sortIndex ?: (getMaxSortIndex() + 1)

            val account = CourseAccountEntity(
                studentId = studentId,
                password = password,
                name = xsxx.name ?: "未知姓名",
                className = xsxx.className ?: "未知班级",
                njdmId = xsxx.njdmId ?: xnm,
                major = xsxx.major ?: "",
                sortIndex = newSortIndex,
                jgId = oldAccount?.jgId ?: "",
                zyhId = xsxx.zyhId ?: oldAccount?.zyhId ?: ""
            )
            insertOrReplaceAccount(account)
            AppLog.d("[LocalCourse] Saved account: ${account.name} (${account.studentId})")
        } else {
            // 即使没有 xsxx 信息，也要创建基本账号记录
            val oldAccount = getAccountById(studentId)
            if (oldAccount == null) {
                val newSortIndex = getMaxSortIndex() + 1
                val account = CourseAccountEntity(
                    studentId = studentId,
                    password = password,
                    name = "用户$studentId",
                    className = "未知班级",
                    njdmId = xnm,
                    major = "",
                    sortIndex = newSortIndex,
                    jgId = "",
                    zyhId = ""
                )
                insertOrReplaceAccount(account)
                AppLog.d("[LocalCourse] Created basic account for: $studentId")
            }
        }

        if (list.isEmpty()) {
            AppLog.d("[LocalCourse] No courses in response")
            return@withContext
        }

        val courseEntities = mutableListOf<CourseEntity>()
        val timeEntities = mutableListOf<ClassTimeEntity>()

        val processedCourseNames = mutableSetOf<String>()

        list.forEach { item ->
            val courseName = item.kcmc ?: "未知课程"

            // 课程去重
            if (courseName !in processedCourseNames) {
                processedCourseNames.add(courseName)
                courseEntities.add(
                    CourseEntity(
                        studentId = studentId,
                        courseName = courseName,
                        xnm = item.xnm ?: xnm,
                        xqm = item.xqm ?: xqm,
                        isCustom = false,
                        remoteCourseId = item.kchId ?: "",
                        nature = item.kcxzmc ?: "",
                        category = item.kclbmc ?: item.kclb ?: "",  // 优先 kclbmc，备选 kclb
                        assessment = item.khfsmc ?: "",
                        credit = item.xf ?: "",
                        background = "#FFFFFF"
                    )
                )
            }

            timeEntities.add(
                ClassTimeEntity(
                    studentId = studentId,
                    courseOwnerName = courseName,
                    xnm = item.xnm ?: xnm,
                    xqm = item.xqm ?: xqm,
                    isCustom = false,
                    weekday = item.xqj ?: "1",
                    period = item.jcs ?: "",
                    weeks = item.zcd ?: "",
                    weeksMask = CourseScheduleParser.weeksToMask(item.zcd ?: ""),
                    location = item.cdmc ?: "",
                    teacher = item.xm ?: "",
                    classGroup = item.jxbzc ?: ""  // jxbzc 是上课班级
                )
            )
        }

        // 整周实践课：教务系统放在 sjkList 里，没有星期节次，只有 qsjsz(如 "19-20周")
        val practiceEntities = (response.sjkList ?: emptyList()).mapNotNull { item ->
            val courseName = item.kcmc?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val weeks = item.qsjsz ?: ""
            PracticeCourseEntity(
                studentId = studentId,
                xnm = xnm,
                xqm = xqm,
                courseName = courseName,
                teacher = item.jsxm ?: "",
                classGroup = item.jxbzh ?: "",
                weeks = weeks,
                weeksMask = CourseScheduleParser.weeksToMask(weeks),
                credit = item.xf ?: "",
                assessment = item.khfsmc ?: "",
                campus = item.xqmc ?: ""
            )
        }

        updateTermCourses(studentId, xnm, xqm, courseEntities, timeEntities, practiceEntities)
        AppLog.d(
            "[LocalCourse] Saved ${courseEntities.size} courses, ${timeEntities.size} time slots " +
                "and ${practiceEntities.size} practice courses"
        )
    }

    // ===== 扩展函数: SQLDelight生成类 -> 数据实体 =====

    private fun com.suseoaa.projectoaa.shared.database.CourseAccount.toEntity() = CourseAccountEntity(
        studentId = studentId,
        password = password,
        name = name,
        className = className,
        njdmId = njdmId,
        major = major,
        sortIndex = sortIndex.toInt(),
        jgId = jgId,
        zyhId = zyhId
    )

    private fun com.suseoaa.projectoaa.shared.database.Course.toEntity() = CourseEntity(
        studentId = studentId,
        courseName = courseName,
        xnm = xnm,
        xqm = xqm,
        isCustom = isCustom == 1L,
        remoteCourseId = remoteCourseId,
        nature = nature,
        background = background,
        category = category,
        assessment = assessment,
        credit = totalHours
    )

    private fun com.suseoaa.projectoaa.shared.database.ClassTime.toEntity() = ClassTimeEntity(
        uniqueId = uniqueId,
        studentId = studentId,
        courseOwnerName = courseOwnerName,
        xnm = xnm,
        xqm = xqm,
        isCustom = isCustom == 1L,
        weekday = weekday,
        period = period,
        weeks = weeks,
        weeksMask = weeksMask,
        location = location,
        teacher = teacher,
        duration = duration,
        teacherTitle = teacherTitle,
        politicalStatus = politicalStatus,
        classGroup = classGroup
    )

    private fun com.suseoaa.projectoaa.shared.database.PracticeCourse.toEntity() = PracticeCourseEntity(
        studentId = studentId,
        xnm = xnm,
        xqm = xqm,
        courseName = courseName,
        teacher = teacher,
        classGroup = classGroup,
        weeks = weeks,
        weeksMask = weeksMask,
        credit = credit,
        assessment = assessment,
        campus = campus
    )

    /**
     * 添加自定义课程
     */
    override suspend fun addCustomCourse(
        studentId: String,
        xnm: String,
        xqm: String,
        courseName: String,
        location: String,
        teacher: String,
        weeks: String,
        dayOfWeek: Int,
        startNode: Int,
        duration: Int
    ) = withContext(Dispatchers.IO) {
        val courseEntity = CourseEntity(
            studentId = studentId,
            courseName = courseName,
            xnm = xnm,
            xqm = xqm,
            isCustom = true,
            remoteCourseId = "",
            nature = "自定义",
            category = "自定义课程",
            assessment = "",
            credit = "",
            background = "#7E57C2"
        )

        val timeEntity = ClassTimeEntity(
            studentId = studentId,
            courseOwnerName = courseName,
            xnm = xnm,
            xqm = xqm,
            isCustom = true,
            weekday = dayOfWeek.toString(),
            period = "$startNode-${startNode + duration - 1}",
            weeks = weeks,
            weeksMask = CourseScheduleParser.weeksToMask(weeks),
            location = location,
            teacher = teacher,
            duration = duration.toString(),
            teacherTitle = "",
            politicalStatus = "",
            classGroup = ""
        )

        insertCustomCourse(courseEntity, timeEntity)
    }
}
