package com.suseoaa.projectoaa.core.database.dao

import androidx.room.*
import com.suseoaa.projectoaa.core.database.entity.ClassTimeEntity
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    // 账号操作

    // 必须按 sortIndex 排序，否则手动排序无效
    @Query("SELECT * FROM course_accounts ORDER BY sortIndex ASC")
    fun getAllAccounts(): Flow<List<CourseAccountEntity>>

    // 获取单个账号（用于 GradesViewModel 同步状态）
    @Query("SELECT * FROM course_accounts WHERE studentId = :studentId LIMIT 1")
    suspend fun getAccountById(studentId: String): CourseAccountEntity?

    // 获取当前最大的排序索引
    @Query("SELECT MAX(sortIndex) FROM course_accounts")
    suspend fun getMaxSortIndex(): Int?

    // 更新单个排序
    @Query("UPDATE course_accounts SET sortIndex = :newIndex WHERE studentId = :studentId")
    suspend fun updateSortIndex(studentId: String, newIndex: Int)

    // 批量更新排序
    @Transaction
    suspend fun updateAllSortIndices(accounts: List<CourseAccountEntity>) {
        accounts.forEachIndexed { index, account ->
            updateSortIndex(account.studentId, index)
        }
    }

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
    suspend fun updateTermCourses(
        studentId: String,
        xnm: String,
        xqm: String,
        courses: List<CourseEntity>,
        times: List<ClassTimeEntity>
    ) {
        deleteRemoteCoursesByTerm(studentId, xnm, xqm)
        courses.forEach { insertCourse(it) }
        insertClassTimes(times)
    }

    @Transaction
    suspend fun insertCustomCourse(course: CourseEntity, time: ClassTimeEntity) {
        insertCourse(course)
        insertClassTimes(listOf(time))
    }

    // 查询操作

    @Query("SELECT * FROM courses WHERE studentId = :studentId AND xnm = :xnm AND xqm = :xqm")
    fun getCourseEntities(studentId: String, xnm: String, xqm: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM class_times WHERE studentId = :studentId AND xnm = :xnm AND xqm = :xqm")
    fun getClassTimeEntities(
        studentId: String,
        xnm: String,
        xqm: String
    ): Flow<List<ClassTimeEntity>>

    @Query("SELECT * FROM course_accounts WHERE studentId = :studentId")
    fun getAccountFlow(studentId: String): Flow<CourseAccountEntity?>

    @Query("SELECT * FROM course_accounts WHERE studentId = :studentId")
    suspend fun getAccount(studentId: String): CourseAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: CourseAccountEntity)

    // === 新增：只更新专业信息 ===
    @Query("UPDATE course_accounts SET jgId = :jgId, zyhId = :zyhId, njdmId = :njdmId WHERE studentId = :studentId")
    suspend fun updateStudentMajorInfo(
        studentId: String,
        jgId: String,
        zyhId: String,
        njdmId: String
    )
}