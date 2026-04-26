package com.suseoaa.projectoaa.shared.util

import kotlinx.datetime.Instant

object OaaClock {
    fun now(): Instant = kotlinx.datetime.Clock.System.now()
}
