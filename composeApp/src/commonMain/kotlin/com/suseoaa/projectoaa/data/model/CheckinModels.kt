package com.suseoaa.projectoaa.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 打卡账号数据模型
 */
data class CheckinAccountData(
    val id: Long = 0,
    val studentId: String,
    val password: String,
    val name: String = "",
    val remark: String = "",
    val lastCheckinTime: String? = null,
    val lastCheckinStatus: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ==================== 用户组 API ====================

/**
 * 用户组响应 - 用于获取用户所属的打卡组（如本科生）
 * GET /site/app/base/common/api/user/groups.rst?appCode=qddk
 */
@Serializable
data class UserGroupsResponse(
    val resultCode: Int = 0,
    val errorMsg: String? = null,
    val success: Boolean = false,
    val result: UserGroupsResult? = null
)

@Serializable
data class UserGroupsResult(
    val data: List<UserGroup> = emptyList(),
    val total: Int = 0
)

@Serializable
data class UserGroup(
    val id: Long = 0,
    val code: String = "",  // 组编码，如 "_sudy2_XDH6iuWnV4="
    val name: String = "",  // 组名称，如 "本科生"
    val type: Int = 0,
    val enable: Boolean = true
)

// ==================== 任务列表 API ====================

/**
 * 打卡任务列表响应
 * GET /site/qddk/qdrw/api/myList.rst?status={1|2|3}
 * status: 1=待签到, 2=已完成, 3=已缺勤
 */
@Serializable
data class CheckinTaskListResponse(
    val resultCode: Int = 0,
    val errorMsg: String? = null,
    val success: Boolean = false,
    val result: CheckinTaskResult? = null
)

@Serializable
data class CheckinTaskResult(
    val data: List<CheckinTask> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CheckinTask(
    val id: Long = 0,                    // 任务ID
    val rwmc: String = "",               // 任务名称
    val rwzt: String = "",               // 任务状态（进行中、已结束）
    val rwlx: Int = 0,                   // 任务类型
    val qdlx: String = "",               // 签到类型（定位签到等）
    val needTime: String = "",           // 需要签到的日期 (如 2025-09-08)
    val qdkssj: String = "",             // 签到开始时间 (如 19:10:00)
    val qdjssj: String = "",             // 签到结束时间 (如 23:00:00)
    val qdksrq: String = "",             // 签到开始日期
    val qdjsrq: String = "",             // 签到结束日期
    val sfwifizjqd: Boolean = false,     // 是否需要WiFi签到
    val sfxyqt: String = "",             // 是否需要其他
    val cjrName: String = "",            // 创建人姓名
    val zqdkrq: String = "",             // 周期打卡日期范围
    val zqdkxq: String = "",             // 周期打卡星期
    val allowRange: Boolean = false,     // 是否允许范围
    @SerialName("start_date")
    val startDate: String = "",          // 开始日期
    @SerialName("end_date")
    val endDate: String = ""             // 结束日期
)

// ==================== 打卡提交 API ====================

/**
 * 打卡提交响应
 * POST /site/app/base/common/api/group/{group_code}/qddk/set.rst
 */
@Serializable
data class CheckinSubmitResponse(
    val resultCode: Int = 0,
    val errorMsg: String? = null,
    val success: Boolean = false,
    val result: CheckinSubmitResult? = null
)

@Serializable
data class CheckinSubmitResult(
    val data: Boolean = false,
    val total: Int = 0
)

// ==================== 其他模型 ====================

/**
 * 打卡位置信息（预留）
 */
data class CheckinLocation(
    val address: String,
    val locationJson: String
)

/**
 * 打卡结果
 */
sealed class CheckinResult {
    data class Success(val message: String) : CheckinResult()
    data class AlreadyChecked(val message: String) : CheckinResult()
    data class NoTask(val message: String) : CheckinResult()
    data class Failed(val error: String) : CheckinResult()
}
