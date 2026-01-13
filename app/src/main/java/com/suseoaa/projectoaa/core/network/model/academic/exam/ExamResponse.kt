package com.suseoaa.projectoaa.core.network.model.academic.exam

import kotlinx.serialization.Serializable

@Serializable
data class ExamResponse(
    val items: List<ExamItem>? = emptyList(),
    val totalResult: Int? = 0,
    val currentPage: Int? = 1
)

@Serializable
data class ExamItem(
    val kcmc: String? = "",   // 课程名称: "网络安全技术"
    val kssj: String? = "",   // 考试时间: "2026-01-08(09:30-11:30)"
    val cdmc: String? = "",   // 教室名称: "LA5-322"
    val cdxqmc: String? = "", // 校区: "临港校区"
    val zw: String? = "",     // 座位号
    val xh: String? = "",     // 学号
    val xm: String? = ""      // 姓名
)