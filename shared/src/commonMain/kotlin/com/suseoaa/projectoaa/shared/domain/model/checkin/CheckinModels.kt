package com.suseoaa.projectoaa.shared.domain.model.checkin

import kotlinx.datetime.toLocalDateTime
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
    val updatedAt: String = "",
    // 扫码登录相关
    val loginType: Int = 0,              // 0=密码登录, 1=扫码登录
    val sessionToken: String? = null,     // 扫码登录的Session
    val sessionExpireTime: String? = null,// Session过期时间
    val selectedLocation: String = "A4教学楼" // 签到地点
) {
    /**
     * 是否为扫码登录账号
     */
    val isQrCodeLogin: Boolean get() = loginType == 1

    /**
     * Session 是否有效（未过期）
     */
    fun isSessionValid(): Boolean {
        if (sessionToken.isNullOrBlank() || sessionExpireTime.isNullOrBlank()) {
            return false
        }
        return try {
            val currentTime = kotlinx.datetime.Clock.System.now()
                .toLocalDateTime(kotlinx.datetime.TimeZone.of("Asia/Shanghai"))
            val currentTimeStr = "${currentTime.date} ${
                currentTime.hour.toString().padStart(2, '0')
            }:${currentTime.minute.toString().padStart(2, '0')}:${
                currentTime.second.toString().padStart(2, '0')
            }"
            sessionExpireTime > currentTimeStr
        } catch (e: Exception) {
            false
        }
    }
}

// ==================== 微信扫码登录 API ====================

@Serializable
data class WechatClientIdResponse(
    val code: Int = 0,
    val msg: String? = null,
    val message: String? = null,
    val data: WechatClientIdData? = null
) {
    fun getClientIdValue(): String? = data?.clientId
}

@Serializable
data class WechatClientIdData(
    @SerialName("client_id")
    val clientId: String? = null
)

@Serializable
data class WechatQrCodeRequest(
    @SerialName("app_id")
    val appId: String,
    @SerialName("client_id")
    val clientId: String
)

@Serializable
data class WechatQrCodeResponse(
    val code: Int = 0,
    val msg: String? = null,
    val message: String? = null,
    val data: WechatQrCodeData? = null
)

@Serializable
data class WechatQrCodeData(
    val img: String = "",
    val imgType: String = "",
    val minute: Int = 5,
    val url: String = ""
) {
    fun getQrCodeImage(): String = img.ifBlank { url }
}

@Serializable
data class WechatScanStatusResponse(
    val code: Int = 0,
    val msg: String? = null,
    val message: String? = null,
    val data: WechatScanStatusData? = null
)

@Serializable
data class WechatScanStatusData(
    val status: Int = 0,
    @SerialName("callback_url")
    val callbackUrl: String? = null,
    @SerialName("user_info")
    val userInfo: WechatUserInfo? = null
)

@Serializable
data class WechatUserInfo(
    val name: String? = null,
    val code: String? = null,
    @SerialName("student_id")
    val studentId: String? = null
)

@Serializable
data class EduUserInfoResponse(
    val code: Int = 0,
    val msg: String? = null,
    val data: EduUserInfo? = null
)

@Serializable
data class EduUserInfo(
    val id: String? = null,
    val code: String? = null,
    val name: String? = null,
    val category: String? = null,
    @SerialName("entergrade")
    val enterGrade: String? = null,
    @SerialName("class")
    val classInfo: EduClassInfo? = null,
    val groups: List<EduGroup>? = null
)

@Serializable
data class EduClassInfo(
    val id: String? = null,
    val code: String? = null,
    val name: String? = null
)

@Serializable
data class EduGroup(
    val id: String? = null,
    val code: String? = null,
    val name: String? = null
)

// ==================== 用户组 API ====================

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
    val code: String = "",
    val name: String = "",
    val type: Int = 0,
    val enable: Boolean = true
)

// ==================== 任务列表 API ====================

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
    val id: Long = 0,
    val rwmc: String = "",
    val rwzt: String = "",
    val rwlx: Int = 0,
    val qdlx: String = "",
    val needTime: String = "",
    val qdkssj: String = "",
    val qdjssj: String = "",
    val qdksrq: String = "",
    val qdjsrq: String = "",
    val sfwifizjqd: Boolean = false,
    val sfxyqt: String = "",
    val cjrName: String = "",
    val zqdkrq: String = "",
    val zqdkxq: String = "",
    val allowRange: Boolean = false,
    @SerialName("start_date")
    val startDate: String = "",
    @SerialName("end_date")
    val endDate: String = "",
    val qdsj: String? = null,
    val qdzt: Int? = null
)

// ==================== 打卡提交 API ====================

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

// ==================== 签到详情 ====================

@Serializable
data class CheckinDetailResponse(
    val resultCode: Int = 0,
    val errorMsg: String? = null,
    val success: Boolean = false,
    val result: CheckinDetailResult? = null
)

@Serializable
data class CheckinDetailResult(
    val data: CheckinDetailData? = null
)

@Serializable
data class CheckinDetailData(
    val dkxx: CheckinDkxxData? = null
)

@Serializable
data class CheckinDkxxData(
    val id: Long = 0,
    val qdzt: Int = 0,
    val qdsj: String? = null,
    val qtzt: Int = 0,
    val qtsj: String? = null,
    val qdddjtdz: String? = null,
    val location: String? = null,
    val needTime: String? = null,
    val need: Boolean = true,
    val xgh: String? = null,
    val xm: String? = null,
    val qdrwid: Long? = null,
    val qddakafs: Int? = null,
    val isOuted: Boolean = false,
    val isLated: Boolean = false,
    val dkddPhoto: String? = null,
    val qdddmc: String? = null,
    val qdddjd: String? = null,
    val qdddwd: String? = null,
    val sfccfw: Boolean = false,
    val sfhq: String? = null,
    val sfwg: Boolean = false,
    val qjsy: String? = null,
    val cdsy: String? = null,
    val wgsy: String? = null,
    val fwwsy: String? = null,
    val noNeedRemark: String? = null,
    val txxx: String? = null
)

@Serializable
data class CheckinLocationRequest(
    val id: Long,
    val qdzt: Int = 1,
    val qdsj: String,
    val isOuted: Int = 0,
    val isLated: Int = 0,
    val dkddPhoto: String = "",
    val qdddjtdz: String,
    val location: String,
    val txxx: String = "{}"
)

// ==================== 预设签到地点 ====================

object CheckinLocations {
    val A4_BUILDING = CheckinLocation(
        name = "A4教学楼",
        address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学(宜宾校区)A4教学楼",
        locationJson = """{"point":[104.401341,28.482517],"address":"四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学(宜宾校区)A4教学楼"}"""
    )

    val COMPUTER_COLLEGE = CheckinLocation(
        name = "计算机学院",
        address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学(宜宾校区)计算机学院",
        locationJson = """{"point":[104.401151,28.483207],"address":"四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学(宜宾校区)计算机学院"}"""
    )

    val ALL = listOf(A4_BUILDING, COMPUTER_COLLEGE)
    val DEFAULT = A4_BUILDING

    fun fromName(name: String?): CheckinLocation {
        return ALL.find { it.name == name } ?: DEFAULT
    }
}

data class CheckinLocation(
    val name: String,
    val address: String,
    val locationJson: String
)

sealed class CheckinResult {
    data class Success(val message: String) : CheckinResult()
    data class AlreadyChecked(val message: String) : CheckinResult()
    data class NoTask(val message: String) : CheckinResult()
    data class Failed(val error: String) : CheckinResult()
}
