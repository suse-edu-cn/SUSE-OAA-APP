package com.suseoaa.projectoaa.shared.util

import kotlin.time.Clock
import kotlin.time.Instant

object OaaClock {
    fun now(): Instant = Clock.System.now()
}

