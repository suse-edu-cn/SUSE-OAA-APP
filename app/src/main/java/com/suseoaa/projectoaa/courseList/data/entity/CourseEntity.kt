package com.suseoaa.projectoaa.courseList.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation

@Entity(
    tableName = "courses",
    primaryKeys = ["studentId", "courseName"]
)
data class CourseEntity(
    val studentId: String,
    val courseName: String,
    val remoteCourseId: String = "",
    val nature: String = "",
    val background: String = "",
    val category: String = "",
    val assessment: String = "",
    val totalHours: String = ""
)

@Entity(
    tableName = "class_times",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["studentId", "courseName"],
            childColumns = ["studentId", "courseOwnerName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            "studentId", "courseOwnerName"
        ),
        Index(
            value = [
                "studentId",
                "courseOwnerName",
                "weekday",
                "period",
                "weeks",
                "location",
                "teacher",
                "classGroup"
            ],
            unique = true
        )
    ]
)
data class ClassTimeEntity(
    val id: Long = 0,
    val studentId: String,
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
) {
    @androidx.room.PrimaryKey(autoGenerate = true)
    var uniqueId: Long = 0
}

data class CourseWithTimes(
    @Embedded val course: CourseEntity,
    @Relation(
        parentColumn = "courseName",
        entityColumn = "courseOwnerName"
    )
    val times: List<ClassTimeEntity>
)