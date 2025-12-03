package com.suseoaa.projectoaa.competition.model

/**
 * 比赛状态的枚举
 */
enum class MatchStatus {
    UPCOMING, // 即将报名
    REGISTERING, // 报名中
    REGISTRATION_ENDED, // 报名结束
    ONGOING, // 比赛中
    ENDED // 已结束
}