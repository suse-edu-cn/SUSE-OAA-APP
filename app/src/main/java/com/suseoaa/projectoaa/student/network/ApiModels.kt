package com.suseoaa.projectoaa.student.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 对应后端 /application 接口的请求体结构
 * 完全依照 API 文档中的 Example JSON 编写
 */
@Serializable
data class ApplicationRequest(
    val name: String,         // 姓名
    val reason: String,       // 竞选理由
    val phone: String,        // 电话
    val gender: String,       // 性别
    val qq: String,           // QQ

    // --- 需要重命名的字段 ---

    @SerialName("choice1")
    val choice1: String,      // 第一志愿

    @SerialName("choice2")
    val choice2: String,      // 第二志愿

    @SerialName("experience")
    val experience: String,   // 个人简历/经历 (对应前端 resume)

    @SerialName("major")
    val major: String,        // 专业 (对应前端 college/所在学院 位置)

    @SerialName("class")
    val className: String,    // 班级 (对应前端 majorClass/专业班级 位置)

    @SerialName("birthday")
    val birthday: String,     // 出生日期 (格式: 2006-02-03)

    @SerialName("politic_stance")
    val politicStance: String,// 政治面貌 (对应前端 politicalStatus)

    @SerialName("adjustiment")
    val adjustiment: Int      // 服从调剂 (后端拼写为 adjustiment，类型为 Int: 0或1)
)

/**
 * 对应后端 /application 接口的响应体
 * 文档显示 200 OK 返回空内容，500 返回 message
 */
@Serializable
data class ApplicationResponse(
    val message: String? = null
)