package com.suseoaa.projectoaa.core.database.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index

/**
 * 成绩实体类
 * 对应数据库中的 "grades" 表
 * 使用 (studentId, xnm, xqm, courseId) 联合主键，确保同一用户在同一学期的同一门课只有一条记录
 */
@Keep
@Entity(
    tableName = "grades",
    primaryKeys = ["studentId", "xnm", "xqm", "courseId"],
    indices = [
        Index(value = ["studentId", "xnm", "xqm"]) // 加速按学期查询
    ]
)
data class GradeEntity(
    // --- 核心标识 (用于确定唯一性) ---
    val studentId: String,      // 学号 (区分不同用户)
    val xnm: String,            // 学年 (如 "2024")
    val xqm: String,            // 学期 (如 "3"=上学期, "12"=下学期)
    val courseId: String,       // 课程号 (kch_id 或 kch)

    // --- 展示数据 ---
    val courseName: String,     // 课程名 (kcmc)
    val score: String,          // 成绩 (cj)
    val credit: String,         // 学分 (xf)
    val gpa: String,            // 绩点 (jd)

    // --- 辅助详情 ---
    val courseType: String,     // 课程性质 (kcxzmc，如"专业基础必修")
    val examType: String,       // 考核方式 (khfsmc，如"考试")
    val teacher: String,        // 教师 (jsxm 或 cjbdczr)
    val examNature: String      // 考试性质 (ksxz，如"正常考试")
)