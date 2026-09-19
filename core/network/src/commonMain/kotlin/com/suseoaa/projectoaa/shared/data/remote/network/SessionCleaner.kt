package com.suseoaa.projectoaa.shared.data.remote.network

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

/**
 * 全局会话清理器，用于在退出登录时清除所有状态
 */
object SessionCleaner : KoinComponent {
    private val checkinCookieStorage: ClearableCookieStorage by inject(named("checkinCookieStorage"))
    private val qrCheckinCookieStorage: ClearableCookieStorage by inject(named("qrCheckinCookieStorage"))

    suspend fun clearAllNetworkSessions() {
        SchoolHttpClient.cookieStorage.clear()
        checkinCookieStorage.clear()
        qrCheckinCookieStorage.clear()
    }
}
