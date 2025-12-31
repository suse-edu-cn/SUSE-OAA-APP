package com.suseoaa.projectoaa.core.database.entity

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/**
 * 账号信息表
 */
@Keep
@Entity(tableName = "course_accounts")
data class CourseAccountEntity(
    @PrimaryKey val studentId: String,
    val password: String,
    val name: String,
    val className: String,
    val njdmId: String,
    val major: String
)

/**
 * 课程表实体
 */
@Keep
@Entity(
    tableName = "courses",
    primaryKeys = ["studentId", "courseName", "xnm", "xqm", "isCustom"]
)
data class CourseEntity(
    val studentId: String,
    val courseName: String,
    val xnm: String,
    val xqm: String,
    val isCustom: Boolean = false,
    val remoteCourseId: String = "",
    val nature: String = "",
    val background: String = "",
    val category: String = "",
    val assessment: String = "",
    val totalHours: String = ""
)

/**
 * 课程时间表实体
 */
@Keep
@Entity(
    tableName = "class_times",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["studentId", "courseName", "xnm", "xqm", "isCustom"],
            childColumns = ["studentId", "courseOwnerName", "xnm", "xqm", "isCustom"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("studentId", "courseOwnerName", "xnm", "xqm", "isCustom")
    ]
)
data class ClassTimeEntity(
    @PrimaryKey(autoGenerate = true) val uniqueId: Long = 0,
    val studentId: String,
    val courseOwnerName: String,
    val xnm: String,
    val xqm: String,
    val isCustom: Boolean,

    val weekday: String = "",
    val period: String = "",
    val weeks: String = "",
    val weeksMask: Long = 0L,
    val location: String = "",
    val teacher: String = "",
    val duration: String = "",
    val teacherTitle: String = "",
    val politicalStatus: String = "",
    val classGroup: String = ""
)

data class CourseWithTimes(
    @Embedded val course: CourseEntity,
    val times: List<ClassTimeEntity>
)