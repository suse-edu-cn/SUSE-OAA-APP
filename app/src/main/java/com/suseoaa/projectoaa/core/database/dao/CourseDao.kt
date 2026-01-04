package com.suseoaa.projectoaa.core.database.dao

import androidx.room.*
import com.suseoaa.projectoaa.core.database.entity.ClassTimeEntity
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    // 账号操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: CourseAccountEntity)

    @Query("SELECT * FROM course_accounts")
    fun getAllAccounts(): Flow<List<CourseAccountEntity>>

    @Query("DELETE FROM course_accounts WHERE studentId = :studentId")
    suspend fun deleteAccount(studentId: String)

    // 课程操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassTimes(times: List<ClassTimeEntity>)

    @Query("DELETE FROM courses WHERE studentId = :studentId AND xnm = :xnm AND xqm = :xqm AND isCustom = 0")
    suspend fun deleteRemoteCoursesByTerm(studentId: String, xnm: String, xqm: String)

    @Query("DELETE FROM courses WHERE studentId = :studentId")
    suspend fun deleteAllCoursesByStudent(studentId: String)

    @Transaction
    suspend fun updateTermCourses(studentId: String, xnm: String, xqm: String, courses: List<CourseEntity>, times: List<ClassTimeEntity>) {
        deleteRemoteCoursesByTerm(studentId, xnm, xqm)
        courses.forEach { insertCourse(it) }
        insertClassTimes(times)
    }

    @Transaction
    suspend fun insertCustomCourse(course: CourseEntity, time: ClassTimeEntity) {
        insertCourse(course)
        insertClassTimes(listOf(time))
    }

    // 查询部分

    // 1. 只查课程本体
    @Query("SELECT * FROM courses WHERE studentId = :studentId AND xnm = :xnm AND xqm = :xqm")
    fun getCourseEntities(studentId: String, xnm: String, xqm: String): Flow<List<CourseEntity>>

    // 2. 只查课程时间 (注意这里严格限制了 studentId)
    @Query("SELECT * FROM class_times WHERE studentId = :studentId AND xnm = :xnm AND xqm = :xqm")
    fun getClassTimeEntities(studentId: String, xnm: String, xqm: String): Flow<List<ClassTimeEntity>>
}