package com.suseoaa.projectoaa.shared.domain.model.exam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamResponse(
    @SerialName("items")
    val items: List<ExamItem>? = emptyList(),
    @SerialName("totalResult")
    val totalResult: Int? = 0,
    @SerialName("currentPage")
    val currentPage: Int? = 1
)

@Serializable
data class ExamItem(
    @SerialName("kcmc")
    val kcmc: String? = "",   // 课程名称: "网络安全技术"
    @SerialName("kssj")
    val kssj: String? = "",   // 考试时间: "2026-01-08(09:30-11:30)"
    @SerialName("cdmc")
    val cdmc: String? = "",   // 教室名称: "LA5-322"
    @SerialName("cdxqmc")
    val cdxqmc: String? = "", // 校区: "临港校区"
    @SerialName("ksmc")
    val ksmc: String? = "",   // 考试名称: "2025-2026-1 期末考试"
    @SerialName("xnmc")
    val xnmc: String? = "",   // 学年名称: "2025-2026"
    @SerialName("xqm")
    val xqm: String? = "",    // 学期码: "3"
    @SerialName("khfs")
    val khfs: String? = "",   // 考核方式: "考试"
    @SerialName("xf")
    val xf: String? = "",     // 学分: "3.0"
    @SerialName("zw")
    val zw: String? = "",     // 座位号
    @SerialName("xh")
    val xh: String? = "",     // 学号
    @SerialName("xm")
    val xm: String? = ""      // 姓名
)

/**
 * 考试响应 - 匹配教务系统返回的完整结构（含分页）
 */
@Serializable
data class ExamApiResponse(
    @SerialName("items")
    val items: List<ExamApiItem>? = emptyList(),
    @SerialName("totalResult")
    val totalResult: Int? = 0,
    @SerialName("currentPage")
    val currentPage: Int? = 1,
    @SerialName("totalPage")
    val totalPage: Int? = 1
)

/**
 * 考试信息条目 - 匹配教务系统返回的完整字段
 */
@Serializable
data class ExamApiItem(
    @SerialName("kcmc")
    val kcmc: String? = "",       // 课程名称: "网络安全技术"
    @SerialName("kssj")
    val kssj: String? = "",       // 考试时间: "2026-01-08(09:30-11:30)"
    @SerialName("cdmc")
    val cdmc: String? = "",       // 教室名称: "LA5-322"
    @SerialName("cdxqmc")
    val cdxqmc: String? = "",     // 校区: "临港校区"
    @SerialName("ksmc")
    val ksmc: String? = "",       // 考试名称: "2025-2026-1 期末考试"
    @SerialName("xnm")
    val xnm: String? = "",        // 学年码: "2025"
    @SerialName("xnmc")
    val xnmc: String? = "",       // 学年名称: "2025-2026"
    @SerialName("xqm")
    val xqm: String? = "",        // 学期码: "3"
    @SerialName("xqmmc")
    val xqmmc: String? = "",      // 学期名称: "1"
    @SerialName("khfs")
    val khfs: String? = "",       // 考核方式: "考试"
    @SerialName("xf")
    val xf: String? = "",         // 学分: "3.0"
    @SerialName("kkxy")
    val kkxy: String? = "",       // 开课学院
    @SerialName("kch")
    val kch: String? = "",        // 课程号
    @SerialName("bj")
    val bj: String? = "",         // 班级
    @SerialName("xh")
    val xh: String? = "",         // 学号
    @SerialName("xm")
    val xm: String? = ""          // 姓名
)
