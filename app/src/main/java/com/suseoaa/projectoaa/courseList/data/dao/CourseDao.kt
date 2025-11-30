package com.suseoaa.projectoaa.courseList.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.suseoaa.projectoaa.courseList.data.entity.ClassTimeEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseWithTimes
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassTimes(times: List<ClassTimeEntity>)

    @Query("DELETE FROM class_times WHERE studentId = :studentId AND courseOwnerName = :courseName")
    suspend fun deleteTimesForCourse(studentId: String, courseName: String)

    @Transaction
    suspend fun upsertCourseWithTimes(course: CourseEntity, times: List<ClassTimeEntity>) {
        insertCourse(course)
        deleteTimesForCourse(course.studentId, course.courseName)
        if (times.isNotEmpty()) insertClassTimes(times)
    }

    @Transaction
    @Query("SELECT * FROM courses WHERE studentId = :studentId")
    fun getCoursesByStudentId(studentId: String): Flow<List<CourseWithTimes>>
}