package com.suseoaa.projectoaa.data.model

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
        // 简单比较时间字符串（格式：yyyy-MM-dd HH:mm:ss）
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

/**
 * 获取 ClientId 响应
 * 实际响应格式: {"code":200,"msg":"ok","data":{"client_id":"xxx"}}
 */
@Serializable
data class WechatClientIdResponse(
    val code: Int = 0,
    val msg: String? = null,
    val message: String? = null,
    val data: WechatClientIdData? = null
) {
    /**
     * 获取实际的 ClientId
     */
    fun getClientIdValue(): String? = data?.clientId
}

@Serializable
data class WechatClientIdData(
    @SerialName("client_id")
    val clientId: String? = null
)

/**
 * 获取二维码 URL 请求
 */
@Serializable
data class WechatQrCodeRequest(
    @SerialName("app_id")
    val appId: String,
    @SerialName("client_id")
    val clientId: String
)

/**
 * 获取二维码响应
 * 实际返回格式: {"code":200,"msg":"ok","data":{"img":"data:image/png;base64,...","imgType":"base64","minute":5}}
 */
@Serializable
data class WechatQrCodeResponse(
    val code: Int = 0,
    val msg: String? = null,
    val message: String? = null,
    val data: WechatQrCodeData? = null
)

@Serializable
data class WechatQrCodeData(
    val img: String = "",          // base64 格式的二维码图片 (data:image/png;base64,...)
    val imgType: String = "",      // 图片类型 (base64)
    val minute: Int = 5,           // 有效期（分钟）
    val url: String = ""           // 备用：可能的 URL 格式
) {
    /**
     * 获取二维码图片数据（优先使用 img，其次用 url）
     */
    fun getQrCodeImage(): String = img.ifBlank { url }
}

/**
 * 扫码状态检查响应
 * POST /edu/v2/weixin/checkScan
 */
@Serializable
data class WechatScanStatusResponse(
    val code: Int = 0,
    val msg: String? = null,
    val message: String? = null,
    val data: WechatScanStatusData? = null
)

@Serializable
data class WechatScanStatusData(
    val status: Int = 0,           // 0=未扫码, 1=已扫码待确认, 2=已确认/已授权
    @SerialName("callback_url")
    val callbackUrl: String? = null,  // 授权成功后的回调 URL
    @SerialName("user_info")
    val userInfo: WechatUserInfo? = null
)

@Serializable
data class WechatUserInfo(
    val name: String? = null,
    val code: String? = null,      // 学号
    @SerialName("student_id")
    val studentId: String? = null
)

/**
 * edu 用户信息响应
 * GET /edu/api/v1/user/getinfo
 */
@Serializable
data class EduUserInfoResponse(
    val code: Int = 0,
    val msg: String? = null,
    val data: EduUserInfo? = null
)

@Serializable
data class EduUserInfo(
    val id: String? = null,
    val code: String? = null,      // 学号
    val name: String? = null,      // 姓名
    val category: String? = null,  // 用户类别: "2" = 本科生
    @SerialName("entergrade")
    val enterGrade: String? = null, // 入学年份
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
    val endDate: String = "",            // 结束日期
    // 签到信息（从详情API获取或任务列表返回）
    val qdsj: String? = null,            // 签到时间 (如 2025-09-08 20:33:02)
    val qdzt: Int? = null                // 签到状态: 0=未签到, 1=已签到
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

// ==================== 基于Python脚本的打卡API（扫码登录使用）====================

/**
 * 签到详情响应
 * GET /site/qddk/qdrw/qdxx/api/detailList.rst?qdrwId={taskId}
 */
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
    val dkxx: CheckinDkxxData? = null  // 打卡信息
)

@Serializable
data class CheckinDkxxData(
    val id: Long = 0,
    val qdzt: Int = 0,           // 签到状态：0=未签到, 1=已签到
    val qdsj: String? = null,    // 签到时间
    val qtzt: Int = 0,           // 签退状态
    val qtsj: String? = null,    // 签退时间
    val qdddjtdz: String? = null,// 签到地点具体地址
    val location: String? = null, // 签到位置JSON
    val needTime: String? = null, // 签到日期 (如 "2025-09-08")
    val need: Boolean = true,    // 是否需要签到
    val xgh: String? = null,     // 学号
    val xm: String? = null,      // 姓名
    val qdrwid: Long? = null,    // 签到任务ID
    val qddakafs: Int? = null,   // 签到打卡方式: 1=定位签到, 2=二维码签到
    val isOuted: Boolean = false, // 是否超出范围
    val isLated: Boolean = false, // 是否迟到
    val dkddPhoto: String? = null, // 打卡照片
    val qdddmc: String? = null,  // 签到地点名称
    val qdddjd: String? = null,  // 签到地点经度
    val qdddwd: String? = null,  // 签到地点纬度
    val sfccfw: Boolean = false, // 是否超出范围
    val sfhq: String? = null,    // 是否缺勤
    val sfwg: Boolean = false,   // 是否未归
    val qjsy: String? = null,    // 请假事由
    val cdsy: String? = null,    // 迟到事由
    val wgsy: String? = null,    // 未归事由
    val fwwsy: String? = null,   // 范围外事由
    val noNeedRemark: String? = null, // 不需要备注
    val txxx: String? = null     // 体温信息
)

/**
 * 位置签到请求
 * POST /site/qddk/qdrw/api/checkSignLocationWithPhoto.rst
 */
@Serializable
data class CheckinLocationRequest(
    val id: Long,                // 任务ID
    val qdzt: Int = 1,           // 签到状态：1=签到
    val qdsj: String,            // 签到时间 "YYYY-MM-DD HH:mm:ss"
    val isOuted: Int = 0,        // 是否外出
    val isLated: Int = 0,        // 是否迟到
    val dkddPhoto: String = "",  // 打卡照片
    val qdddjtdz: String,        // 签到地点具体地址
    val location: String,        // 签到位置JSON {"point":[lng,lat],"address":"..."}
    val txxx: String = "{}"      // 体温信息
)

// ==================== 预设签到地点 ====================

/**
 * 预设签到地点（来自Python脚本）
 */
object CheckinLocations {
    /**
     * A4教学楼
     */
    val A4_BUILDING = CheckinLocation(
        name = "A4教学楼",
        address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学(宜宾校区)A4教学楼",
        locationJson = """{"point":[104.401341,28.482517],"address":"四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学(宜宾校区)A4教学楼"}"""
    )

    /**
     * 计算机学院
     */
    val COMPUTER_COLLEGE = CheckinLocation(
        name = "计算机学院",
        address = "四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学(宜宾校区)计算机学院",
        locationJson = """{"point":[104.401151,28.483207],"address":"四川省宜宾市翠屏区白沙湾街道大学路四川轻化工大学(宜宾校区)计算机学院"}"""
    )

    /**
     * 所有预设地点列表
     */
    val ALL = listOf(A4_BUILDING, COMPUTER_COLLEGE)

    /**
     * 默认地点
     */
    val DEFAULT = A4_BUILDING

    /**
     * 根据名称获取地点
     */
    fun fromName(name: String?): CheckinLocation {
        return ALL.find { it.name == name } ?: DEFAULT
    }
}

// ==================== 其他模型 ====================

/**
 * 打卡位置信息
 */
data class CheckinLocation(
    val name: String,            // 地点名称（用于UI显示）
    val address: String,         // 详细地址
    val locationJson: String     // 位置JSON（发送给API）
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
