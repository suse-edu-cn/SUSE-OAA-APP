package com.suseoaa.projectoaa.shared.data.repository.checkin

import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinLocation
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 位置签到请求体的构造。
 *
 * 这段 JSON 结构原先在两个仓库里被重复拼装了五次，任何一次字段调整都得改五处，
 * 现在只有这一个出口。
 */
object CheckinSignData {

    /**
     * @param taskId 签到任务 ID（服务端要求传任务 ID，而非签到记录 ID）
     * @param location 本次签到使用的地点
     * @param signTime 签到时间，格式 "yyyy-MM-dd HH:mm:ss"
     */
    fun build(
        taskId: Long,
        location: CheckinLocation,
        signTime: String = CheckinClock.nowString()
    ): String = buildJsonObject {
        put("id", taskId)
        put("qdzt", 1)              // 签到状态：1=已签到
        put("qdsj", signTime)       // 签到时间
        put("isOuted", 0)           // 是否超出范围
        put("isLated", 0)           // 是否迟到
        put("dkddPhoto", "")        // 打卡地点照片
        put("qdddjtdz", location.address)      // 签到地点具体地址
        put("location", location.locationJson) // 位置信息 JSON
        put("txxx", "{}")           // 图像信息
    }.toString()
}
