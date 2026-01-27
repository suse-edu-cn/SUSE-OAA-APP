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
    @SerialName("zw")
    val zw: String? = "",     // 座位号
    @SerialName("xh")
    val xh: String? = "",     // 学号
    @SerialName("xm")
    val xm: String? = ""      // 姓名
)
