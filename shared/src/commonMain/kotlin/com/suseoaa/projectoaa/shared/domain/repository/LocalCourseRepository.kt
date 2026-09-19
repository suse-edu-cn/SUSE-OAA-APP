package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.course.*
import com.suseoaa.projectoaa.shared.domain.model.school.CourseResponseJson
import kotlinx.coroutines.flow.Flow

/**
 * LocalCourseRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface LocalCourseRepository {

    fun getAllAccounts(): Flow<List<CourseAccountEntity>>

    suspend fun getAccountById(studentId: String): CourseAccountEntity?

    suspend fun insertOrReplaceAccount(account: CourseAccountEntity): Unit

    suspend fun updateSortIndex(studentId: String, newIndex: Int): Unit

    suspend fun getMaxSortIndex(): Int

    suspend fun deleteAccount(studentId: String): Unit

    suspend fun updateMajorInfo(studentId: String, jgId: String, zyhId: String, njdmId: String): Unit

    suspend fun updateAllSortIndices(accounts: List<CourseAccountEntity>): Unit

    fun getCoursesByTerm(studentId: String, xnm: String, xqm: String): Flow<List<CourseEntity>>

    fun getClassTimesByTerm(
        studentId: String,
        xnm: String,
        xqm: String
        ): Flow<List<ClassTimeEntity>>

    fun getCourses(studentId: String, xnm: String, xqm: String): Flow<List<CourseWithTimes>>

    fun getAllCoursesByStudent(studentId: String): Flow<List<CourseWithTimes>>

    fun getAvailableTerms(studentId: String): Flow<List<Pair<String, String>>>

    suspend fun insertCourse(course: CourseEntity): Unit

    suspend fun insertClassTime(time: ClassTimeEntity): Unit

    suspend fun deleteRemoteCoursesByTerm(studentId: String, xnm: String, xqm: String): Unit

    suspend fun deleteAllCoursesByStudent(studentId: String): Unit

    suspend fun deleteCourse(
        studentId: String,
        courseName: String,
        xnm: String,
        xqm: String,
        isCustom: Boolean
        ): Unit

    suspend fun updateTermCourses(
        studentId: String,
        xnm: String,
        xqm: String,
        courses: List<CourseEntity>,
        times: List<ClassTimeEntity>,
        practiceCourses: List<PracticeCourseEntity>): Unit

    fun getPracticeCourses(studentId: String, xnm: String, xqm: String): Flow<List<PracticeCourseEntity>>

    suspend fun insertCustomCourse(course: CourseEntity, time: ClassTimeEntity): Unit

    suspend fun saveFromResponse(
        studentId: String,
        password: String,
        response: CourseResponseJson
        ): Unit

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
        ): Unit
}
