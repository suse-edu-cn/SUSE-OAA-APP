package com.suseoaa.projectoaa.courseList.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * 账号信息表
 * 存储账号、密码、姓名、班级、入学年份(NJDM_ID)
 */
@Entity(tableName = "course_accounts")
data class CourseAccountEntity(
    @PrimaryKey val studentId: String, // 学号作为主键
    val password: String,
    val name: String,          // XM
    val className: String,     // BJMC
    val njdmId: String,        // NJDM_ID 入学年份，用于计算可选学年
    val major: String          // ZYMC 专业
)

/**
 * 课程表实体
 * 联合主键升级：学号 + 课程名 + 学年 + 学期 + 是否自定义
 * 解决多账号课程冲突、多学期存储问题
 */
@Entity(
    tableName = "courses",
    primaryKeys = ["studentId", "courseName", "xnm", "xqm", "isCustom"]
)
data class CourseEntity(
    val studentId: String,
    val courseName: String,
    val xnm: String, // 学年 (2024)
    val xqm: String, // 学期 (3 或 12)
    val isCustom: Boolean = false, // 是否为自定义课程

    // 以下为非必填信息
    val remoteCourseId: String = "",
    val nature: String = "",
    val background: String = "",
    val category: String = "",
    val assessment: String = "",
    val totalHours: String = ""
)

/**
 * 课程时间表实体
 * 外键关联到 courses 表 (级联删除)
 */
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
    val location: String = "", // 地点 (自定义课程选填)
    val teacher: String = "",  // 教师 (自定义课程选填)
    val duration: String = "", // 持续时间 (自定义课程选填)
    val teacherTitle: String = "",
    val politicalStatus: String = "",
    val classGroup: String = ""
)

data class CourseWithTimes(
    @Embedded val course: CourseEntity,
    @Relation(
        parentColumn = "courseName",
        entityColumn = "courseOwnerName"
    )
    val times: List<ClassTimeEntity> // 注意：这里Room会自动匹配外键，但我们需要在DAO里通过查询过滤确保精确匹配
)