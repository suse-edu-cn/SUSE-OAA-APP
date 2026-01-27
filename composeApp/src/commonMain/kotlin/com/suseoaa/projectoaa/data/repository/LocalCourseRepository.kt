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

    suspend fun updateTermCourses(
        studentId: String,
        xnm: String,
        xqm: String,
        courses: List<CourseEntity>,
        times: List<ClassTimeEntity>
    ) = withContext(Dispatchers.IO) {
        database.transaction {
            // inside transaction, use raw queries or private synchronous helpers
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
        val list = response.kbList ?: return@withContext
        
        if (list.isEmpty()) return@withContext

        val firstItem = list.first()
        val xnm = firstItem.xnm ?: "2024"
        val xqm = firstItem.xqm ?: "3"

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
                        category = item.kclbmc ?: "",
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
                    classGroup = "" // item.jxbmc not available in model
                )
            )
        }

        updateTermCourses(studentId, xnm, xqm, courseEntities, timeEntities)
    }

    private fun parseWeeksToMask(weeksStr: String): Long {
        var mask = 0L
        try {
            val parts = weeksStr.replace("周", "").split(",")
            for (part in parts) {
                if (part.contains("-")) {
                    val rangeParts = part.split("-")
                    val start = rangeParts[0].toIntOrNull() ?: 1
                    var endStr = rangeParts[1]
                    var step = 1
                    
                    if (endStr.contains("(单)")) {
                        step = 2
                        if (start % 2 == 0) step = 0 
                        endStr = endStr.replace("(单)", "")
                    } else if (endStr.contains("(双)")) {
                        step = 2
                        if (start % 2 != 0) step = 0 
                        endStr = endStr.replace("(双)", "")
                    }
                    
                    val end = endStr.toIntOrNull() ?: start
                    
                    for (i in start..end step if (step == 0) 1 else step) {
                        if (i in 1..60) {
                            mask = mask or (1L shl (i - 1))
                        }
                    }
                } else {
                    val week = part.toIntOrNull()
                    if (week != null && week in 1..60) {
                         mask = mask or (1L shl (week - 1))
                    }
                }
            }
        } catch (e: Exception) {
            // Log error
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
}
