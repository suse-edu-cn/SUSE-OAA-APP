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
    val selectedLocation: String = "宜宾" // 签到校区（宜宾/李白河/汇东）
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
            val currentTime = com.suseoaa.projectoaa.shared.util.OaaClock.now()
                .toLocalDateTime(kotlinx.datetime.TimeZone.of("Asia/Shanghai"))
            val currentTimeStr = "${currentTime.date} ${
                currentTime.hour.toString().padStart(2, '0')
            }:${currentTime.minute.toString().padStart(2, '0')}:${
                currentTime.second.toString().padStart(2, '0')
            }"
            sessionExpireTime > currentTimeStr
        } catch (_: Exception) {
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

// ==================== 校区和签到地点 ====================

/**
 * 校区数据模型，包含多个预设签到位置
 */
data class Campus(
    val name: String,
    val locations: List<CheckinLocation>
) {
    /**
     * 随机选择一个位置
     */
    fun randomLocation(): CheckinLocation {
        return locations.random()
    }
}

/**
 * 签到位置信息
 */
data class CheckinLocation(
    val address: String,
    val locationJson: String
)

/**
 * 预设校区和签到地点
 */
object CheckinLocations {
    val 宜宾 = Campus(
        name = "宜宾",
        locations = listOf(
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674665, 28.804867], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674634, 28.804868], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674666, 28.804866], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674655, 28.804956], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675577, 28.805467], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道干锅王(龙观嘉园店)四川轻化工大学宜宾校区", locationJson = """{"point": [104.666635, 28.804571], "address": "四川省宜宾市翠屏区白沙湾街道干锅王(龙观嘉园店)四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道品正食府四川轻化工大学宜宾校区", locationJson = """{"point": [104.673723, 28.803802], "address": "四川省宜宾市翠屏区白沙湾街道品正食府四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675645, 28.805614], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675484, 28.805382], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675648, 28.805554], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675551, 28.805411], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675541, 28.805473], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675521, 28.805505], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674574, 28.804864], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674446, 28.804882], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674413, 28.804857], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区A4教学楼", locationJson = """{"point": [104.67306, 28.803896], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区A4教学楼"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674648, 28.804946], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区自动化与信息工程学院", locationJson = """{"point": [104.670291, 28.804048], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区自动化与信息工程学院"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道白塔路科教·公元π", locationJson = """{"point": [104.674717, 28.800673], "address": "四川省宜宾市翠屏区白沙湾街道白塔路科教·公元π"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674643, 28.804943], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道芭茅田四川轻化工大学宜宾校区", locationJson = """{"point": [104.666082, 28.80777], "address": "四川省宜宾市翠屏区白沙湾街道芭茅田四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道芭茅田四川轻化工大学宜宾校区", locationJson = """{"point": [104.666031, 28.807639], "address": "四川省宜宾市翠屏区白沙湾街道芭茅田四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.67561, 28.805407], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675451, 28.805431], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675512, 28.805461], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675573, 28.805402], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区B6宿舍楼", locationJson = """{"point": [104.666897, 28.806461], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区B6宿舍楼"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675546, 28.805492], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675547, 28.805439], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675433, 28.805413], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675699, 28.805404], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674568, 28.804578], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674417, 28.804854], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674733, 28.804981], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674967, 28.804386], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674602, 28.804738], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.675305, 28.804906], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674671, 28.804649], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674527, 28.804786], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674755, 28.804483], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学美术学院四川轻化工大学李白河校区", locationJson = """{"point": [104.830447, 29.378223], "address": "四川省自贡市大安区大山铺镇四川轻化工大学美术学院四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674805, 28.804456], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.675376, 28.803665], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675593, 28.805394], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675437, 28.805444], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675511, 28.805398], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675515, 28.805463], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675484, 28.805453], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675512, 28.805449], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675518, 28.80544], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675506, 28.80545], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675186, 28.805344], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道白塔路四川轻化工大学宜宾校区", locationJson = """{"point": [104.676428, 28.802406], "address": "四川省宜宾市翠屏区白沙湾街道白塔路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675398, 28.805424], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675173, 28.805419], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675303, 28.805342], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.67534, 28.804415], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674667, 28.804721], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674707, 28.804946], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674833, 28.804588], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674637, 28.804712], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674684, 28.804683], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道品正食府四川轻化工大学宜宾校区", locationJson = """{"point": [104.674306, 28.804197], "address": "四川省宜宾市翠屏区白沙湾街道品正食府四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674629, 28.804748], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674705, 28.804744], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.674662, 28.804665], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.674618, 28.804864], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园", locationJson = """{"point": [104.67469, 28.804764], "address": "四川省宜宾市翠屏区白沙湾街道四川轻化工大学宜宾校区品正园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道干锅王(龙观嘉园店)龙观嘉园", locationJson = """{"point": [104.666359, 28.803539], "address": "四川省宜宾市翠屏区白沙湾街道干锅王(龙观嘉园店)龙观嘉园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道干锅王(龙观嘉园店)龙观嘉园", locationJson = """{"point": [104.666432, 28.803638], "address": "四川省宜宾市翠屏区白沙湾街道干锅王(龙观嘉园店)龙观嘉园"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675397, 28.805481], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.675477, 28.805465], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
            CheckinLocation(address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区", locationJson = """{"point": [104.67548, 28.805436], "address": "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学宜宾校区"}"""),
        )
    )

    val 李白河 = Campus(
        name = "李白河",
        locations = listOf(
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829171, 29.377728], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829322, 29.37762], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829775, 29.377374], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区", locationJson = """{"point": [104.82811, 29.377335], "address": "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区", locationJson = """{"point": [104.828101, 29.377333], "address": "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区", locationJson = """{"point": [104.832512, 29.37879], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.828983, 29.377621], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区", locationJson = """{"point": [104.832861, 29.378843], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.828988, 29.378293], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区", locationJson = """{"point": [104.831166, 29.378615], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区", locationJson = """{"point": [104.832071, 29.378629], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区", locationJson = """{"point": [104.832531, 29.378774], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区", locationJson = """{"point": [104.832771, 29.379016], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇东环路四川轻化工大学李白河校区", locationJson = """{"point": [104.827969, 29.376971], "address": "四川省自贡市大安区大山铺镇东环路四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区", locationJson = """{"point": [104.828087, 29.377164], "address": "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829237, 29.377599], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829284, 29.377554], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829352, 29.377544], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829682, 29.377346], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829234, 29.377662], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829252, 29.377696], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.82837, 29.377042], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇东环路四川轻化工大学李白河校区", locationJson = """{"point": [104.82829, 29.376939], "address": "四川省自贡市大安区大山铺镇东环路四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区", locationJson = """{"point": [104.828328, 29.377047], "address": "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区", locationJson = """{"point": [104.828025, 29.37745], "address": "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.828586, 29.377484], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区", locationJson = """{"point": [104.8281, 29.377458], "address": "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区", locationJson = """{"point": [104.828129, 29.377472], "address": "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.828366, 29.377084], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学美术学院四川轻化工大学李白河校区", locationJson = """{"point": [104.832693, 29.378304], "address": "四川省自贡市大安区大山铺镇四川轻化工大学美术学院四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829097, 29.377728], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829348, 29.377553], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829268, 29.377627], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区", locationJson = """{"point": [104.828149, 29.377386], "address": "四川省自贡市大安区大山铺镇艺雅苑1号楼四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇肯德基车速取餐点(四川轻化工大学店)四川轻化工大学李白河校区", locationJson = """{"point": [104.831257, 29.376241], "address": "四川省自贡市大安区大山铺镇肯德基车速取餐点(四川轻化工大学店)四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区", locationJson = """{"point": [104.83262, 29.37883], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区", locationJson = """{"point": [104.832922, 29.378814], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.828464, 29.377595], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学美术学院四川轻化工大学李白河校区", locationJson = """{"point": [104.832692, 29.378304], "address": "四川省自贡市大安区大山铺镇四川轻化工大学美术学院四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区", locationJson = """{"point": [104.83255, 29.378801], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区", locationJson = """{"point": [104.833044, 29.378916], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.828678, 29.37725], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区", locationJson = """{"point": [104.832719, 29.378875], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.828745, 29.377615], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇东环路四川轻化工大学李白河校区", locationJson = """{"point": [104.827621, 29.377195], "address": "四川省自贡市大安区大山铺镇东环路四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区", locationJson = """{"point": [104.832944, 29.379055], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区"}"""),
            CheckinLocation(address = "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑", locationJson = """{"point": [104.829025, 29.377711], "address": "四川省自贡市大安区大山铺镇四川轻化工大学李白河校区艺雅苑"}"""),
        )
    )

    val 汇东 = Campus(
        name = "汇东",
        locations = listOf(
            CheckinLocation(address = "四川省自贡市自流井区学苑街道汇雅路15号四川轻化工大学汇东校区", locationJson = """{"point": [104.763952, 29.330347], "address": "四川省自贡市自流井区学苑街道汇雅路15号四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道四川轻化工大学汇东校区学生公寓6栋", locationJson = """{"point": [104.763525, 29.330216], "address": "四川省自贡市自流井区学苑街道四川轻化工大学汇东校区学生公寓6栋"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道汇勤路四川轻化工大学汇东校区", locationJson = """{"point": [104.766355, 29.331665], "address": "四川省自贡市自流井区学苑街道汇勤路四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区", locationJson = """{"point": [104.761283, 29.329964], "address": "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区", locationJson = """{"point": [104.761095, 29.330553], "address": "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街紫萝香居", locationJson = """{"point": [104.760865, 29.330652], "address": "四川省自贡市自流井区学苑街道南苑街紫萝香居"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街25号四川轻化工大学汇东校区", locationJson = """{"point": [104.761191, 29.330842], "address": "四川省自贡市自流井区学苑街道南苑街25号四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区", locationJson = """{"point": [104.760958, 29.330763], "address": "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街27号四川轻化工大学汇东校区", locationJson = """{"point": [104.761287, 29.331014], "address": "四川省自贡市自流井区学苑街道南苑街27号四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街27号四川轻化工大学汇东校区", locationJson = """{"point": [104.7612, 29.330924], "address": "四川省自贡市自流井区学苑街道南苑街27号四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街27号四川轻化工大学汇东校区", locationJson = """{"point": [104.761194, 29.331105], "address": "四川省自贡市自流井区学苑街道南苑街27号四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区", locationJson = """{"point": [104.760997, 29.330791], "address": "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区", locationJson = """{"point": [104.760824, 29.330893], "address": "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道汇智路四川轻化工大学汇东校区", locationJson = """{"point": [104.766039, 29.333461], "address": "四川省自贡市自流井区学苑街道汇智路四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道四川轻化工大学汇东校区学生公寓6栋", locationJson = """{"point": [104.763716, 29.330286], "address": "四川省自贡市自流井区学苑街道四川轻化工大学汇东校区学生公寓6栋"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区", locationJson = """{"point": [104.760906, 29.331095], "address": "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街29号四川轻化工大学汇东校区", locationJson = """{"point": [104.761, 29.331285], "address": "四川省自贡市自流井区学苑街道南苑街29号四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街29号四川轻化工大学汇东校区", locationJson = """{"point": [104.761031, 29.331123], "address": "四川省自贡市自流井区学苑街道南苑街29号四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区", locationJson = """{"point": [104.761112, 29.330785], "address": "四川省自贡市自流井区学苑街道南苑街四川轻化工大学汇东校区"}"""),
            CheckinLocation(address = "四川省自贡市自流井区学苑街道汇川路1637号四川轻化工大学汇东校区", locationJson = """{"point": [104.762494, 29.330348], "address": "四川省自贡市自流井区学苑街道汇川路1637号四川轻化工大学汇东校区"}"""),
        )
    )

    /** 所有校区列表 */
    val ALL_CAMPUSES = listOf(宜宾, 李白河, 汇东)

    /** 所有校区名称 */
    val CAMPUS_NAMES = ALL_CAMPUSES.map { it.name }

    /** 默认校区 */
    val DEFAULT_CAMPUS = 宜宾

    /**
     * 根据校区名称查找校区
     */
    fun fromCampusName(name: String?): Campus {
        return ALL_CAMPUSES.find { it.name == name } ?: DEFAULT_CAMPUS
    }

    /**
     * 根据校区名称随机获取一个签到位置
     */
    fun randomLocationForCampus(campusName: String?): CheckinLocation {
        return fromCampusName(campusName).randomLocation()
    }
}

sealed class CheckinResult {
    data class Success(val message: String) : CheckinResult()
    data class AlreadyChecked(val message: String) : CheckinResult()
    data class NoTask(val message: String) : CheckinResult()
    data class Failed(val error: String) : CheckinResult()
}
