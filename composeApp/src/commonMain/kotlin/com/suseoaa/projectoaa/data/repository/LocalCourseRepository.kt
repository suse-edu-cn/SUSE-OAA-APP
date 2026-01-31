package com.suseoaa.projectoaa.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.suseoaa.projectoaa.data.model.*
import com.suseoaa.projectoaa.database.CourseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalCourseRepository(private val database: CourseDatabase) {

    // ===== 账号管理 =====

    fun getAllAccounts(): Flow<List<CourseAccountEntity>> {
        return database.courseAccountQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { it.toEntity() }
            }
    }

    suspend fun getAccountById(studentId: String): CourseAccountEntity? = withContext(Dispatchers.IO) {
        database.courseAccountQueries.selectById(studentId).executeAsOneOrNull()?.toEntity()
    }

    suspend fun insertOrReplaceAccount(account: CourseAccountEntity) = withContext(Dispatchers.IO) {
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

    suspend fun updateSortIndex(studentId: String, newIndex: Int) = withContext(Dispatchers.IO) {
        database.courseAccountQueries.updateSortIndex(newIndex.toLong(), studentId)
    }

    suspend fun getMaxSortIndex(): Int = withContext(Dispatchers.IO) {
        database.courseAccountQueries.getMaxSortIndex().executeAsOneOrNull()?.maxIndex?.toInt() ?: 0
    }

    suspend fun deleteAccount(studentId: String) = withContext(Dispatchers.IO) {
        database.courseAccountQueries.deleteById(studentId)
    }

    suspend fun updateMajorInfo(studentId: String, jgId: String, zyhId: String, njdmId: String) = withContext(Dispatchers.IO) {
        database.courseAccountQueries.updateMajorInfo(jgId, zyhId, njdmId, studentId)
    }

    suspend fun updateAllSortIndices(accounts: List<CourseAccountEntity>) = withContext(Dispatchers.IO) {
        database.transaction {
            accounts.forEachIndexed { index, account ->
                // transaction is synchronous, so we call generated updateSortIndex.
                database.courseAccountQueries.updateSortIndex(index.toLong(), account.studentId)
            }
        }
    }

    // ===== 课程管理 =====

    fun getCoursesByTerm(studentId: String, xnm: String, xqm: String): Flow<List<CourseEntity>> {
        return database.courseQueries.selectByTerm(studentId, xnm, xqm)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toEntity() } }
    }

    fun getClassTimesByTerm(studentId: String, xnm: String, xqm: String): Flow<List<ClassTimeEntity>> {
        return database.classTimeQueries.selectByTerm(studentId, xnm, xqm)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toEntity() } }
    }

    fun getCourses(studentId: String, xnm: String, xqm: String): Flow<List<CourseWithTimes>> {
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

    // removed suspend from insert helpers to allow usage inside transaction
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
            totalHours = course.totalHours
        )
    }
    
    suspend fun insertCourse(course: CourseEntity) = withContext(Dispatchers.IO) {
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

    suspend fun insertClassTime(time: ClassTimeEntity) = withContext(Dispatchers.IO) {
         insertClassTimeInternal(time)
    }

    suspend fun deleteRemoteCoursesByTerm(studentId: String, xnm: String, xqm: String) = withContext(Dispatchers.IO) {
        database.courseQueries.deleteRemoteCoursesByTerm(studentId, xnm, xqm)
    }

    suspend fun deleteAllCoursesByStudent(studentId: String) = withContext(Dispatchers.IO) {
        database.courseQueries.deleteAllByStudent(studentId)
    }
    
    suspend fun deleteCourse(studentId: String, courseName: String, xnm: String, xqm: String, isCustom: Boolean) = withContext(Dispatchers.IO) {
        database.courseQueries.deleteCourse(
            studentId = studentId,
            courseName = courseName,
            xnm = xnm,
            xqm = xqm,
            isCustom = if (isCustom) 1L else 0L
        )
    }

    suspend fun updateTermCourses(
        studentId: String,
        xnm: String,
        xqm: String,
        courses: List<CourseEntity>,
        times: List<ClassTimeEntity>
    ) = withContext(Dispatchers.IO) {
        database.transaction {
            // 先删除时间表，再删除课程表（确保完全清除）
            database.classTimeQueries.deleteRemoteByTerm(studentId, xnm, xqm)
            database.courseQueries.deleteRemoteCoursesByTerm(studentId, xnm, xqm)
            courses.forEach { insertCourseInternal(it) }
            times.forEach { insertClassTimeInternal(it) }
        }
    }

    suspend fun insertCustomCourse(course: CourseEntity, time: ClassTimeEntity) = withContext(Dispatchers.IO) {
        database.transaction {
            insertCourseInternal(course)
            insertClassTimeInternal(time)
        }
    }

    suspend fun saveFromResponse(
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
            println("[LocalCourse] Saved account: ${account.name} (${account.studentId})")
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
                println("[LocalCourse] Created basic account for: $studentId")
            }
        }

        if (list.isEmpty()) {
            println("[LocalCourse] No courses in response")
            return@withContext
        }

        val courseEntities = mutableListOf<CourseEntity>()
        val timeEntities = mutableListOf<ClassTimeEntity>()
        
        val processedCourseNames = mutableSetOf<String>()

        list.forEach { item ->
            val courseName = item.kcmc ?: "未知课程"
            
            // Deduplicate courses
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
                        totalHours = item.xf ?: "", 
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
                    weeksMask = parseWeeksToMask(item.zcd ?: ""),
                    location = item.cdmc ?: "",
                    teacher = item.xm ?: "",
                    classGroup = item.jxbzc ?: ""  // jxbzc 是上课班级
                )
            )
        }

        updateTermCourses(studentId, xnm, xqm, courseEntities, timeEntities)
        println("[LocalCourse] Saved ${courseEntities.size} courses and ${timeEntities.size} time slots")
    }

    private fun parseWeeksToMask(weeksStr: String): Long {
        var mask = 0L
        try {
            // 先检测整个字符串是否包含单双周标记
            val globalOddOnly = weeksStr.contains("单") && !weeksStr.contains("双")
            val globalEvenOnly = weeksStr.contains("双") && !weeksStr.contains("单")
            
            // 清理字符串：移除"周"，用特殊标记保留单双周信息
            val cleanedStr = weeksStr
                .replace("周", "")
                .replace("(单)", "#ODD#")  // 用特殊标记保留单双周信息
                .replace("（单）", "#ODD#")
                .replace("(双)", "#EVEN#")
                .replace("（双）", "#EVEN#")
                .replace("单", "")  // 移除单独的"单"字（如"单周"的"单"）
                .replace("双", "")  // 移除单独的"双"字
                .replace(" ", "")
            
            val parts = cleanedStr.split(",")
            for (part in parts) {
                // 检查此部分是否有单双周标记
                val isOddOnly = part.contains("#ODD#") || (globalOddOnly && !part.contains("#EVEN#"))
                val isEvenOnly = part.contains("#EVEN#") || (globalEvenOnly && !part.contains("#ODD#"))
                
                val cleanPart = part.replace("#ODD#", "").replace("#EVEN#", "")
                
                if (cleanPart.contains("-")) {
                    val rangeParts = cleanPart.split("-")
                    val start = rangeParts[0].toIntOrNull() ?: continue
                    val end = rangeParts[1].toIntOrNull() ?: continue
                    
                    for (week in start..end) {
                        if (week in 1..60) {
                            // 检查单双周过滤
                            val shouldInclude = when {
                                isOddOnly -> week % 2 == 1   // 单周：奇数周
                                isEvenOnly -> week % 2 == 0  // 双周：偶数周
                                else -> true                  // 默认：所有周
                            }
                            if (shouldInclude) {
                                mask = mask or (1L shl (week - 1))
                            }
                        }
                    }
                } else {
                    val week = cleanPart.toIntOrNull()
                    if (week != null && week in 1..60) {
                         mask = mask or (1L shl (week - 1))
                    }
                }
            }
        } catch (e: Exception) {
            println("[LocalCourse] Error parsing weeks: ${e.message}")
        }
        return mask
    }

    // ===== 扩展函数: SQLDelight生成类 -> 数据实体 =====

    private fun com.suseoaa.projectoaa.database.CourseAccount.toEntity() = CourseAccountEntity(
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

    private fun com.suseoaa.projectoaa.database.Course.toEntity() = CourseEntity(
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
        totalHours = totalHours
    )

    private fun com.suseoaa.projectoaa.database.ClassTime.toEntity() = ClassTimeEntity(
        uniqueId = uniqueId ?: 0,
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

    /**
     * 添加自定义课程
     */
    suspend fun addCustomCourse(
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
            totalHours = "",
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
            weeksMask = parseWeeksToMask(weeks),
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
