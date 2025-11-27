package com.suseoaa.projectoaa.courseList.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.suseoaa.projectoaa.courseList.data.entity.ClassTimeEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseWithTimes
import com.suseoaa.projectoaa.courseList.data.entity.TimeWithCourse
import kotlinx.coroutines.flow.Flow

/**
 * 课表DAO
 * MVVM架构 - Model层（数据访问对象）
 */
@Dao
interface CourseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassTimes(times: List<ClassTimeEntity>)
    
    @Query("DELETE FROM class_times WHERE courseOwnerName = :courseName")
    suspend fun deleteTimesForCourse(courseName: String)
    
    @Transaction
    suspend fun upsertCourseWithTimes(course: CourseEntity, times: List<ClassTimeEntity>) {
        insertCourse(course)
        deleteTimesForCourse(course.courseName)
        if (times.isNotEmpty()) insertClassTimes(times)
    }
    
    @Transaction
    @Query("SELECT * FROM courses")
    fun getAllCoursesWithTimes(): Flow<List<CourseWithTimes>>
    
    @Transaction
    @Query("SELECT * FROM courses WHERE courseName = :courseName")
    fun getCourseWithTimes(courseName: String): Flow<CourseWithTimes?>
    
    @Transaction
    @Query("SELECT * FROM class_times WHERE weekday = :weekday")
    fun getTimesByWeekday(weekday: String): Flow<List<TimeWithCourse>>
    
    @Transaction
    @Query("SELECT * FROM class_times WHERE weeks LIKE '%' || :weekToken || '%' AND weekday = :weekday")
    fun getTimesByWeekAndWeekday(weekToken: String, weekday: String): Flow<List<TimeWithCourse>>
    
    @Transaction
    @Query("SELECT * FROM class_times WHERE (weeksMask & :weekBit) != 0 AND weekday = :weekday")
    fun getTimesByWeekBitAndWeekday(weekBit: Long, weekday: String): Flow<List<TimeWithCourse>>
}
