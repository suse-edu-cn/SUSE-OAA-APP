package com.suseoaa.projectoaa.shared.domain.model.grade

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StudentGradeResponse(
    @SerialName("items")
    val items: List<GradeItem>? = emptyList(),
    @SerialName("totalResult")
    val totalResult: Int? = 0,
    @SerialName("currentPage")
    val currentPage: Int? = 1
)

@Serializable
data class GradeItem(
    @SerialName("bfzcj")
    val bfzcj: String? = "",       // 百分制成绩
    @SerialName("bh")
    val bh: String? = "",          // 班号
    @SerialName("bj")
    val bj: String? = "",          // 班级
    @SerialName("cj")
    val cj: String? = "",          // 成绩
    @SerialName("cjbdczr")
    val cjbdczr: String? = "",     // 成绩变动操作人
    @SerialName("jd")
    val jd: String? = "",          // 绩点
    @SerialName("jg_id")
    val jgId: String? = "",        // 学院ID
    @SerialName("jgmc")
    val jgmc: String? = "",        // 学院名称
    @SerialName("jsxm")
    val jsxm: String? = "",        // 教师姓名
    @SerialName("jxb_id")
    val jxbId: String? = "",       // 教学班ID (用于获取详情)
    @SerialName("jxbmc")
    val jxbmc: String? = "",       // 教学班名称
    @SerialName("kcbj")
    val kcbj: String? = "",        // 课程标记
    @SerialName("kch")
    val kch: String? = "",         // 课程号
    @SerialName("kch_id")
    val kchId: String? = "",       // 课程ID
    @SerialName("kclbmc")
    val kclbmc: String? = "",      // 课程类别名称
    @SerialName("kcmc")
    val kcmc: String? = "",        // 课程名称
    @SerialName("kcxzmc")
    val kcxzmc: String? = "",      // 课程性质名称 (专业基础必修等)
    @SerialName("khfsmc")
    val khfsmc: String? = "",      // 考核方式名称
    @SerialName("kkbmmc")
    val kkbmmc: String? = "",      // 开课部门名称
    @SerialName("ksxz")
    val ksxz: String? = "",        // 考试性质 (正常考试/补考)
    @SerialName("njdm_id")
    val njdmId: String? = "",      // 年级代码ID
    @SerialName("njmc")
    val njmc: String? = "",        // 年级名称
    @SerialName("sfxwkc")
    val sfxwkc: String? = "",      // 是否学位课程
    @SerialName("xf")
    val xf: String? = "",          // 学分
    @SerialName("xfjd")
    val xfjd: String? = "",        // 学分绩点
    @SerialName("xh")
    val xh: String? = "",          // 学号
    @SerialName("xm")
    val xm: String? = "",          // 姓名
    @SerialName("xnm")
    val xnm: String? = "",         // 学年码
    @SerialName("xnmmc")
    val xnmmc: String? = "",       // 学年名称
    @SerialName("xqm")
    val xqm: String? = "",         // 学期码
    @SerialName("xqmmc")
    val xqmmc: String? = "",       // 学期名称
    @SerialName("zyh_id")
    val zyhId: String? = "",       // 专业ID
    @SerialName("zymc")
    val zymc: String? = ""         // 专业名称
)

/**
 * 原先声明在 data/repository 包里，导致 UI 层必须 import 数据层才能拿到模型。
 * 它是纯领域模型，归位到 domain/model。
 */
/**
 * 成绩实体类 (用于UI层)
 */
data class GradeEntity(
    val studentId: String,
    val xnm: String,
    val xqm: String,
    val courseId: String,
    val jxbId: String = "",
    val regularScore: String = "",
    val regularRatio: String = "",
    val experimentScore: String = "",
    val experimentRatio: String = "",
    val finalScore: String = "",
    val finalRatio: String = "",
    val courseName: String,
    val score: String,
    val credit: String,
    val gpa: String,
    val courseType: String,
    val examType: String,
    val teacher: String,
    val examNature: String
)
