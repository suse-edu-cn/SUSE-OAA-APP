package com.suseoaa.projectoaa.courseList.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * 课程实体（课程主表）
 * MVVM架构 - Model层（数据实体） 
 */
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val courseName: String,
    val remoteCourseId: String = "",
    val nature: String = "",
    val background: String = "",
    val category: String = "",
    val assessment: String = "",
    val totalHours: String = ""
)

/**
 * 上课时间实体（时间明细表）
 */
@Entity(
    tableName = "class_times",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["courseName"],
            childColumns = ["courseOwnerName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("courseOwnerName"),
        Index(
            value = ["courseOwnerName", "weekday", "period", "weeks", "location", "teacher", "classGroup"],
            unique = true
        )
    ]
)
data class ClassTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseOwnerName: String,
    val weekday: String = "",
    val period: String = "",
    val weeks: String = "",
    val weeksMask: Long = 0L,
    val location: String = "",
    val teacher: String = "",
    val teacherTitle: String = "",
    val politicalStatus: String = "",
    val classGroup: String = ""
)

/**
 * 课程及其时间（一对多关系）
 */
data class CourseWithTimes(
    @Embedded val course: CourseEntity,
    @Relation(
        parentColumn = "courseName",
        entityColumn = "courseOwnerName"
    )
    val times: List<ClassTimeEntity>
)

/**
 * 时间及其课程（多对一关系）
 */
data class TimeWithCourse(
    @Embedded val time: ClassTimeEntity,
    @Relation(
        parentColumn = "courseOwnerName",
        entityColumn = "courseName"
    )
    val course: CourseEntity
)
