package com.suseoaa.projectoaa.core.database.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index

@Keep
@Entity(
    tableName = "grades",
    primaryKeys = ["studentId", "xnm", "xqm", "courseId"],
    indices = [
        Index(value = ["studentId", "xnm", "xqm"])
    ]
)
data class GradeEntity(
    val studentId: String,
    val xnm: String,
    val xqm: String,
    val courseId: String,

    val jxbId: String = "",       // 教学班ID

    // --- 成绩详情 ---
    val regularScore: String = "", // 平时成绩
    val regularRatio: String = "", // 平时比例 (如 "40%")

    // 实验成绩字段
    val experimentScore: String = "",
    val experimentRatio: String = "",

    val finalScore: String = "",   // 期末成绩
    val finalRatio: String = "",   // 期末比例 (如 "60%")

    val courseName: String,
    val score: String,
    val credit: String,
    val gpa: String,
    val courseType: String,
    val examType: String,
    val teacher: String,
    val examNature: String
)