package com.suseoaa.projectoaa.shared.domain.nearfield

import kotlinx.serialization.Serializable

/**
 * 近场签到任务的数据模型
 * 该类用于描述一个特定的签到活动，包含签到的基本信息
 */
@Serializable
data class NearFieldCheckinTask(
    /**
     * 任务的全局唯一标识符
     */
    val taskIdentifier: String,
    
    /**
     * 签到活动的名称
     */
    val activityName: String,
    
    /**
     * 发布该签到任务的主机名称
     */
    val hostDeviceName: String,
    
    /**
     * 签到任务的发布时间戳
     */
    val publishTimestamp: Long,

    /**
     * 随机数(Nonce)，用于增强签到信号的唯一性，防止重放攻击
     */
    val securityNonce: String = "",

    /**
     * 签到开始时间戳
     */
    val startTime: Long,

    /**
     * 签到截止时间戳
     */
    val endTime: Long,

    /**
     * 主机IP地址
     */
    val hostAddress: String? = null,

    /**
     * 主机通信端口
     */
    val hostPort: Int? = null,

    /**
     * 附加的元数据（例如地点、备注等）
     */
    val extraMetadata: Map<String, String> = emptyMap()
) {
    /**
     * 检查任务是否仍在有效期内
     */
    fun isValid(currentTimeMillis: Long): Boolean {
        return currentTimeMillis in startTime..endTime
    }
}
