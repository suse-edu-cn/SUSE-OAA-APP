package com.suseoaa.projectoaa.shared.util

import kotlinx.datetime.Instant

object OaaClock {
    fun now(): Instant = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
}
