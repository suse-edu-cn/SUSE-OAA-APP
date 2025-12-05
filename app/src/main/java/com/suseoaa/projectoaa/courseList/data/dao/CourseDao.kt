package com.suseoaa.projectoaa.courseList.data.dao

import androidx.room.*
import com.suseoaa.projectoaa.courseList.data.entity.ClassTimeEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseAccountEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseWithTimes
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    // === 账号操作 ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: CourseAccountEntity)

    @Query("SELECT * FROM course_accounts")
    fun getAllAccounts(): Flow<List<CourseAccountEntity>>

    @Query("DELETE FROM course_accounts WHERE studentId = :studentId")
    suspend fun deleteAccount(studentId: String)

    // === 课程操作 ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassTimes(times: List<ClassTimeEntity>)

    // 删除特定学期、特定用户的非自定义课程（刷新课表时使用）
    @Query("DELETE FROM courses WHERE studentId = :studentId AND xnm = :xnm AND xqm = :xqm AND isCustom = 0")
    suspend fun deleteRemoteCoursesByTerm(studentId: String, xnm: String, xqm: String)

    // 删除某个账号的所有课程（删除账号时使用）
    @Query("DELETE FROM courses WHERE studentId = :studentId")
    suspend fun deleteAllCoursesByStudent(studentId: String)

    @Transaction
    suspend fun updateTermCourses(studentId: String, xnm: String, xqm: String, courses: List<CourseEntity>, times: List<ClassTimeEntity>) {
        // 1. 清除该学期旧的教务系统数据
        deleteRemoteCoursesByTerm(studentId, xnm, xqm)
        // 2. 插入新数据
        courses.forEach { insertCourse(it) }
        insertClassTimes(times)
    }

    @Transaction
    suspend fun insertCustomCourse(course: CourseEntity, time: ClassTimeEntity) {
        insertCourse(course)
        insertClassTimes(listOf(time))
    }

    // === 查询 ===
    // 这里的 Transaction 保证 CourseWithTimes 的组装是原子的
    // 我们需要手动过滤 times，因为 @Relation 默认只匹配主键，但我们的 ClassTimeEntity 还有 xnm/xqm 区分
    // 这里采用一种更稳健的方法：直接查 times 然后在内存组装，或者依靠 Room 的 Relation 匹配 parentColumns
    @Transaction
    @Query("SELECT * FROM courses WHERE studentId = :studentId AND xnm = :xnm AND xqm = :xqm")
    fun getCoursesByTerm(studentId: String, xnm: String, xqm: String): Flow<List<CourseWithTimes>>
}