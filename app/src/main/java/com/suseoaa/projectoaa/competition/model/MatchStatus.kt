package com.suseoaa.projectoaa.competition.model

enum class MatchStatus {
    UPCOMING,           // 0
    REGISTERING,        // 1
    REGISTRATION_ENDED, // 2
    ONGOING,            // 3
    ENDED;              // 4

    companion object {
        fun fromInt(code: Int): MatchStatus {
            return when (code) {
                0 -> UPCOMING
                1 -> REGISTERING
                2 -> REGISTRATION_ENDED
                3 -> ONGOING
                4 -> ENDED
                else -> UPCOMING
            }
        }
    }
}